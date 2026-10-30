package io.legado.app.ui.book.read

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalReadMenuPolicyTest {
    @Test
    fun staleOnlineBookStateCannotEnableSourceOrDownloadActions() {
        val staleState = ReadBookUiState(isLocalBook = false, chapterPayable = true)
        listOf("change_source", "download", "refresh", "source_login", "source_pay",
            "source_custom_button", "edit_source", "disable_source").forEach { action ->
            assertFalse(action, isReadMenuActionApplicable(action, staleState))
            assertFalse(action, action in ReadMenuActionIds)
            assertFalse(action, action in MoreActionIds)
        }
        assertTrue(isReadMenuActionApplicable("search", ReadBookUiState()))
        assertTrue(isReadMenuActionApplicable("charset", ReadBookUiState(isLocalBook = true)))
    }
}
