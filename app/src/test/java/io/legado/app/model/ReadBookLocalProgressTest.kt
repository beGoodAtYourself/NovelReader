package io.legado.app.model

import android.app.Application
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.service.BaseReadAloudService
import io.legado.app.support.InMemoryAppDatabaseFixture
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import splitties.init.injectAsAppCtx

/** 阅读记录开关不能覆盖当前书籍的阅读位置。 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ReadBookLocalProgressTest {

    private lateinit var savedBook: Book
    private lateinit var currentBook: Book
    private lateinit var databaseFixture: InMemoryAppDatabaseFixture

    @Before
    fun setUp() {
        RuntimeEnvironment.getApplication().injectAsAppCtx()
        databaseFixture = InMemoryAppDatabaseFixture(RuntimeEnvironment.getApplication())
        BaseReadAloudService.requestStop()
        savedBook = Book(
            bookUrl = "file:///books/local-progress.txt",
            name = "本地阅读进度",
            type = BookType.text or BookType.local,
            durChapterIndex = 5,
            durChapterPos = 100,
            durChapterTime = 1_000,
            syncTime = 500,
        )
        currentBook = savedBook.copy(
            durChapterIndex = 8,
            durChapterPos = 850,
            durChapterTime = 2_000,
            durChapterTitle = "Chapter 8",
        )
        appDb.bookDao.insert(currentBook)
        ReadBook.replaceCurrentBook(currentBook)
    }

    @After
    fun tearDown() {
        try {
            ReadBook.clearCurrentBook()
        } finally {
            if (::databaseFixture.isInitialized) databaseFixture.close()
        }
    }

    @Test
    fun readRecordToggleForAnotherCopyDoesNotReplaceOrChangeTheActiveBook() {
        val anotherCopy = currentBook.copy(bookUrl = "file:///books/another-copy.txt")
        ReadBook.setReadRecordDisabled(anotherCopy.bookUrl, true)
        assertSame(currentBook, ReadBook.book)
        assertEquals(false, currentBook.disableReadRecord)

        ReadBook.setReadRecordDisabled(currentBook.bookUrl, true)
        assertSame(currentBook, ReadBook.book)
        assertEquals(true, currentBook.disableReadRecord)
        assertEquals(8, currentBook.durChapterIndex)
        assertEquals(850, currentBook.durChapterPos)
        assertEquals(2_000L, currentBook.durChapterTime)
    }

}
