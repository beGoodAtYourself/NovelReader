package io.legado.app.ui.main.my

sealed class PrefClickEvent {
    data class OpenUrl(val url: String) : PrefClickEvent()
    data class CopyUrl(val url: String) : PrefClickEvent()
    data class StartActivity(val destination: Class<*>, val configTag: String? = null) : PrefClickEvent()
    object OpenReadRecord : PrefClickEvent()
    object OpenBookCacheManage : PrefClickEvent()
    object OpenBookSourceManage : PrefClickEvent()
    object OpenReplaceRules : PrefClickEvent()
    object OpenHighlightTagRule : PrefClickEvent()
    object OpenTxtTocRule : PrefClickEvent()
    object OpenDictRule : PrefClickEvent()
    object OpenAllBookmark : PrefClickEvent()
    object OpenAbout : PrefClickEvent()
    object ToggleWebService : PrefClickEvent()
    object ExitApp : PrefClickEvent()
}
