package io.legado.app.data.repository.manga

import android.app.Application
import io.legado.app.BuildConfig
import io.legado.app.domain.model.LocalReadingPolicy
import io.legado.app.R
import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.readRecord.ReadRecordSession
import io.legado.app.data.repository.ReadRecordRepository
import io.legado.app.domain.gateway.MangaReaderDataGateway
import io.legado.app.domain.model.manga.MangaBookPresentation
import io.legado.app.domain.model.manga.MangaBookState
import io.legado.app.domain.model.manga.MangaChapterContent
import io.legado.app.domain.model.manga.MangaProgressState
import io.legado.app.domain.model.manga.OpenedMangaBook
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.isLocalModified
import io.legado.app.help.book.readSimulating
import io.legado.app.help.book.simulatedTotalChapterNum
import io.legado.app.help.storage.Backup
import io.legado.app.model.localBook.LocalBook
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MangaReaderDataRepository(
    private val application: Application,
    private val database: AppDatabase,
    private val readRecordRepository: ReadRecordRepository,
) : MangaReaderDataGateway {

    private var readingStartedAt: Long? = null
    private val chapterPageLoader = MangaChapterPageLoader(application, database)
    private val localMangaLoader =
        LocalMangaLoader(java.io.File(application.cacheDir, "local-manga"))

    override fun observeBookPresentation(bookUrl: String) =
        database.bookDao.flowGetBook(bookUrl)
            .map { book ->
                MangaBookPresentation(
                    scrollMode = book?.readConfig?.mangaScrollMode,
                    sidePaddingDp = book?.readConfig?.webtoonSidePaddingDp,
                )
            }
            .distinctUntilChanged()

    override suspend fun openBook(
        bookUrl: String?,
        inBookshelf: Boolean,
        chapterChanged: Boolean,
    ): OpenedMangaBook {
        val book = bookUrl?.takeIf(String::isNotEmpty)?.let(database.bookDao::getBook)
            ?: database.bookDao.lastReadBook
            ?: throw NoStackTraceException(application.getString(R.string.no_book))
        if (!LocalReadingPolicy.acceptsBook(book.type, book.bookUrl)) {
            throw NoStackTraceException(application.getString(R.string.local_reader_only))
        }
        if (!localBookExists(book)) {
            throw NoStackTraceException(application.getString(R.string.no_book))
        }
        var chapterCount = database.bookChapterDao.getChapterCount(book.bookUrl)
        if (chapterCount == 0 || book.isLocalModified()) {
            val chapters = if (localMangaLoader.supports(book)) {
                localMangaLoader.chapters(book).also {
                    book.totalChapterNum = it.size
                    book.latestChapterTitle = it.lastOrNull()?.title.orEmpty()
                }
            } else LocalBook.getChapterList(book)
            database.bookChapterDao.delByBook(book.bookUrl)
            database.bookChapterDao.insert(*chapters.toTypedArray())
            database.bookDao.update(book)
            chapterCount = chapters.size

        }
        if (chapterCount == 0) chapterCount = database.bookChapterDao.getChapterCount(book.bookUrl)
        val simulatedCount = if (book.readSimulating()) {
            book.simulatedTotalChapterNum()
        } else chapterCount
        val safeChapter = book.durChapterIndex.coerceIn(0, (simulatedCount - 1).coerceAtLeast(0))
        val newerProgress: MangaProgressState? = null
        return OpenedMangaBook(
            book = MangaBookState(
                bookUrl = book.bookUrl,
                name = book.name,
                author = book.author,
                coverUrl = book.coverUrl,
                customCoverUrl = book.customCoverUrl,
                sourceOrigin = book.origin,
                sourceName = "",
                sourceType = null,
                inBookshelf = inBookshelf,
                scrollMode = book.readConfig?.mangaScrollMode,
                sidePaddingDp = book.readConfig?.webtoonSidePaddingDp,
                isLocal = book.isLocal,
                chapterTitles = database.bookChapterDao.getChapterList(book.bookUrl).map { it.title },
            ),
            chapterIndex = safeChapter,
            pageIndex = book.durChapterPos.coerceAtLeast(0),
            chapterCount = simulatedCount,
            newerProgress = newerProgress,
        )
    }

    override suspend fun loadChapter(bookUrl: String, chapterIndex: Int): MangaChapterContent {
        val book = database.bookDao.getBook(bookUrl)
            ?: throw NoStackTraceException(application.getString(R.string.no_book))
        if (!LocalReadingPolicy.acceptsBook(book.type, book.bookUrl)) {
            throw NoStackTraceException(application.getString(R.string.local_reader_only))
        }
        val chapter = database.bookChapterDao.getChapter(bookUrl, chapterIndex)
            ?: throw NoStackTraceException(application.getString(R.string.error_load_toc))
        return if (book.isLocal && localMangaLoader.supports(book)) {
            localMangaLoader.load(book, chapter)
        } else chapterPageLoader.load(book, chapter)
    }

    override suspend fun prefetchChapter(bookUrl: String, chapterIndex: Int) = Unit

    override suspend fun retainChapterResources(bookUrl: String, chapterIndexes: Set<Int>) {
        chapterPageLoader.retain(bookUrl, chapterIndexes)
    }

    override suspend fun releaseAllChapterResources() {
        chapterPageLoader.close()
        localMangaLoader.close()
    }

    override suspend fun persistProgress(bookUrl: String, chapterIndex: Int, pageIndex: Int) {
        val book = database.bookDao.getBook(bookUrl) ?: return
        book.durChapterIndex = chapterIndex
        book.durChapterPos = pageIndex
        book.durChapterTime = System.currentTimeMillis()
        database.bookChapterDao.getChapter(bookUrl, chapterIndex)?.let {
            book.durChapterTitle = it.title
        }
        database.bookDao.update(book)
    }

    override suspend fun applyProgress(bookUrl: String, progress: MangaProgressState) {
        persistProgress(bookUrl, progress.chapterIndex, progress.pageIndex)
    }

    override suspend fun resume(bookUrl: String) {
        if (readingStartedAt == null) readingStartedAt = System.currentTimeMillis()
    }

    override suspend fun pause(bookUrl: String, inBookshelf: Boolean) {
        val start = readingStartedAt ?: return
        readingStartedAt = null
        val end = System.currentTimeMillis()
        val book = database.bookDao.getBook(bookUrl) ?: return
        if (end - start >= 10_000) {
            readRecordRepository.saveReadSession(
                ReadRecordSession(
                    bookName = book.name,
                    bookAuthor = book.author,
                    bookUrl = book.bookUrl,
                    startTime = start,
                    endTime = end,
                )
            )
        }
        if (inBookshelf && !BuildConfig.DEBUG) Backup.autoBack(application)
    }

    override suspend fun syncProgress(bookUrl: String): MangaProgressState? = null

    private fun localBookExists(book: Book): Boolean = runCatching {
        LocalBook.getBookInputStream(book)
    }.isSuccess
}
