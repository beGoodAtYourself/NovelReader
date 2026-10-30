package io.legado.app.domain.model

import io.legado.app.constant.BookType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalReadingPolicyTest {
    @Test
    fun acceptsLocalDocumentsAndLegacyFilePaths() {
        listOf("content://documents/book/1", "file:///storage/books/a.epub", "/storage/books/小说.txt", "books/a.txt")
            .forEach { assertTrue(it, LocalReadingPolicy.acceptsBook(BookType.local, it)) }
    }

    @Test
    fun rejectsRemoteAddressesEvenWhenBackupMarksThemLocal() {
        listOf("https://example.test/book", "HTTP://example.test/book", " https://example.test/book", "ftp://host/book", "//host/book", "javascript:run()", "", "  ")
            .forEach { assertFalse(it, LocalReadingPolicy.acceptsBook(BookType.local, it)) }
    }

    @Test
    fun localAddressAloneDoesNotTurnAnOnlineBookIntoALocalBook() {
        assertFalse(LocalReadingPolicy.acceptsBook(BookType.text, "/storage/books/a.txt"))
        assertTrue(LocalReadingPolicy.acceptsBook(BookType.text or BookType.local, "/storage/books/a.txt"))
    }
}
