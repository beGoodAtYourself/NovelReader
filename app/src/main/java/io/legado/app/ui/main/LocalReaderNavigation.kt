package io.legado.app.ui.main

import androidx.navigation3.runtime.NavKey
import io.legado.app.domain.model.LocalReadingPolicy

/** 本地版的导航白名单；旧版本的远程目的地不会恢复执行。 */
fun isLocalReaderRoute(route: NavKey): Boolean = when (route) {
    MainRouteHome, MainRouteSettings, MainRouteSettingsOther,
    MainRouteSettingsRead, MainRouteSettingsCover, MainRouteSettingsCoverAlbums,
    MainRouteSettingsTheme, MainRouteSettingsBackup, MainRouteSettingsCustomTheme,
    MainRouteSettingsThemeManage, MainRouteSettingsPrivate, MainRouteImportLocal,
    MainRouteReadRecord, MainRouteReadRecordOverview, MainRouteAllBookmark,
    MainRouteHighlightTagRule, MainRouteAbout, MainRouteReadAloudPlayer -> true
    is MainRouteReadManga -> route.bookUrl == null || LocalReadingPolicy.acceptsAddress(route.bookUrl)
    is MainRouteReadBook -> route.bookUrl == null || LocalReadingPolicy.acceptsAddress(route.bookUrl)
    is MainRouteToc -> LocalReadingPolicy.acceptsAddress(route.bookUrl)
    is MainRouteBookInfo -> LocalReadingPolicy.acceptsAddress(route.bookUrl)
    is MainRouteBookInfoEdit -> LocalReadingPolicy.acceptsAddress(route.bookUrl)
    is MainRouteSearchContent -> LocalReadingPolicy.acceptsAddress(route.bookUrl)
    is MainRouteReplaceRules, is MainRouteReplaceEdit,
    is MainRouteTxtTocRule, is MainRouteTxtTocRulePreview -> true
    else -> false
}
