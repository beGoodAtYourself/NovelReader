package io.legado.app.ui.main.bookshelf

import io.legado.app.constant.BookType
import org.junit.Assert.assertEquals
import org.junit.Test

class BookshelfReadingProgressTest {
    @Test
    fun `unread books start at zero even with a chapter list`() {
        assertProgress(0.0, book)
        assertProgress(0.0, book.copy(totalChapterNum = 1))
    }

    @Test
    fun `reading progress includes the current chapter`() {
        assertProgress(0.01, book.copy(durChapterPos = 120))
        assertProgress(0.25, book.copy(durChapterIndex = 24))
        assertProgress(1.0, book.copy(durChapterIndex = 99))
    }

    @Test
    fun `missing or invalid chapter data falls back to zero`() {
        assertProgress(0.0, book.copy(totalChapterNum = 0, durChapterIndex = 12))
        assertProgress(0.0, book.copy(totalChapterNum = -1, durChapterPos = 120))
        assertProgress(0.0, book.copy(durChapterIndex = -1, durChapterPos = 120))
    }

    @Test
    fun `stale reading positions stay within one hundred percent without overflow`() {
        assertProgress(1.0, book.copy(durChapterIndex = 120))
        assertProgress(1.0, book.copy(durChapterIndex = Int.MAX_VALUE))
        assertProgress(1.0, book.copy(
            durChapterIndex = Int.MAX_VALUE,
            totalChapterNum = Int.MAX_VALUE,
        ))
    }

    private fun assertProgress(expected: Double, item: BookShelfItem) {
        assertEquals(expected, item.getReadProgress(), 0.000001)
    }

    private val book = BookShelfItem(
        bookUrl = "local-book",
        name = "书名",
        author = "作者",
        origin = "local",
        originName = "book.txt",
        coverUrl = null,
        customCoverUrl = null,
        durChapterTitle = null,
        durChapterTime = 0L,
        durChapterPos = 0,
        latestChapterTitle = null,
        latestChapterTime = 0L,
        lastCheckCount = 0,
        totalChapterNum = 100,
        durChapterIndex = 0,
        type = BookType.local,
        group = 0L,
        order = 0,
    )
}
