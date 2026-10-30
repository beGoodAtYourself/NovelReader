package io.legado.app.data.repository

import androidx.room.Room
import io.legado.app.constant.BookType
import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [35])
class LocalBookRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: BookRepository
    private val localUrl = "content://books/local.txt"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = BookRepository(database.bookDao, database.bookChapterDao, database)
        database.bookDao.insert(
            Book(bookUrl = localUrl, name = "本地书", type = BookType.text or BookType.local, durChapterTime = 1),
            Book(bookUrl = "https://example.test/online", name = "远程书", type = BookType.text, durChapterTime = 10),
            Book(bookUrl = "https://example.test/disguised", name = "旧备份远程地址", type = BookType.local, durChapterTime = 20),
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun shelfCountPreviewAndLastReadAllExcludeRemoteBooks() = runBlocking {
        assertEquals(listOf(localUrl), repository.flowBookShelfByGroup(BookGroup.IdAll).first().map { it.bookUrl })
        assertEquals(listOf(localUrl), repository.flowGroupPreview(BookGroup.IdAll).first().map { it.bookUrl })
        assertEquals(1, repository.flowAllBookShelfCount().first())
        assertEquals(1, repository.flowSystemGroupCounts().first().first { it.groupId == BookGroup.IdAll }.count)
        assertEquals(localUrl, repository.getLastReadBook()?.bookUrl)
        assertEquals(listOf(localUrl), repository.getShelfBookSummaries().map { it.bookUrl })
    }

    @Test
    fun restoredOnlineBookCannotBeOpenedByItsOldUrl() = runBlocking {
        assertNull(repository.getBook("https://example.test/online"))
        assertNull(repository.getBook("https://example.test/disguised"))
        assertEquals(listOf(localUrl), repository.getBooksByUrls(setOf(localUrl, "https://example.test/online")).map { it.bookUrl })
    }
}
