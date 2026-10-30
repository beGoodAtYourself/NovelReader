package io.legado.app.ui.main

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalReaderNavigationTest {
    @Test
    fun bookDetailsAndContentsAcceptOnlyLocalAddresses() {
        for ((address, allowed) in listOf("content://books/local.txt" to true, "https://example.test/book" to false)) {
            listOf(MainRouteBookInfo(null, null, address), MainRouteBookInfoEdit(address),
                MainRouteToc(address), MainRouteSearchContent(address))
                .forEach { assertEquals(it.toString(), allowed, isLocalReaderRoute(it)) }
        }
    }

    @Test
    fun legacyTabOrderKeepsOnlyBookshelfAndMy() {
        assertEquals(
            listOf(MainDestination.My, MainDestination.Bookshelf),
            MainDestination.ordered("rss,my,explore,home,my,bookshelf"),
        )
        assertEquals(listOf(MainDestination.Bookshelf, MainDestination.My), MainDestination.ordered("rss,explore"))
    }

    @Test
    fun localImportReaderAndBackupRemainAvailable() {
        listOf(MainRouteImportLocal, MainRouteReadBook("content://book/1"), MainRouteReadManga("content://book/local.pdf"), MainRouteSettingsBackup,
            MainRouteSettingsRead, MainRouteAllBookmark, MainRouteReadRecord)
            .forEach { assertTrue(it.toString(), isLocalReaderRoute(it)) }
    }

    @Test
    fun remoteRoutesCannotBeRestoredOrOpened() {
        listOf(MainRouteReadManga("https://example.test/book"), MainRouteReadBook("https://example.test/book"), MainRouteImportRemote, MainRouteBookSourceManage(), MainRouteRssSourceManage,
            MainRouteSettingsAi, MainRouteSettingsTranslation, MainRouteRuleSub)
            .forEach { assertFalse(it.toString(), isLocalReaderRoute(it)) }
    }

    @Test
    fun bothCompactAndWideNavigationRejectRetiredPages() {
        for (wide in listOf(false, true)) {
            val stack = mutableListOf<NavKey>(MainRouteHome)
            val navigation = MainEntryNavigation(MainRouteHome, stack, wide, MainNavRouteTracker()) {
                error("已移除的页面不应触发导航")
            }
            navigation.onNavigateToRoute(MainRouteSettingsAi)
            navigation.pushRoute(MainRouteImportRemote)
            assertEquals(listOf(MainRouteHome), stack)
        }
    }
}
