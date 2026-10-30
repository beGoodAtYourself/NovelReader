package io.legado.app.ui.main

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.legado.app.R
import io.legado.app.data.repository.AppShellSettingsRepository
import io.legado.app.data.repository.ThemeSettingsRepository
import io.legado.app.domain.gateway.AppShellSettingsGateway
import io.legado.app.domain.gateway.ThemeSettingsGateway
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.help.config.AppConfigStore
import io.legado.app.ui.book.info.BookInfoActions
import io.legado.app.ui.book.read.sheet.ReaderBookTocMenu
import io.legado.app.ui.book.toc.TocIntent
import io.legado.app.ui.book.toc.TocUiState
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenu
import io.legado.app.ui.config.coverConfig.CoverConfigIntent
import io.legado.app.ui.config.coverConfig.CoverConfigScreen
import io.legado.app.ui.config.coverConfig.CoverConfigSheet
import io.legado.app.ui.config.coverConfig.CoverConfigUiState
import io.legado.app.ui.theme.AppTheme
import io.legado.app.ui.widget.components.filePicker.FilePickerSheet
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

import splitties.init.injectAsAppCtx

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w400dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalReaderSurfaceTest {
    @get:Rule val compose = createComposeRule()

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication().also { it.injectAsAppCtx() }
        AppConfigStore.init(context)
        stopKoin()
        startKoin {
            modules(module {
                single<AppShellSettingsGateway> { AppShellSettingsRepository() }
                single<ThemeSettingsGateway> { ThemeSettingsRepository() }
            })
        }
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun bookInformationKeepsLocalActionsWithoutSourceSwitching() {
        val clicks = mutableListOf<String>()
        render {
            BookInfoActions(
                inBookshelf = true, hasShelfDuplicates = false,
                onShelfClick = { clicks.add("shelf") },
                onTocClick = { clicks.add("toc") },
                onGroupClick = { clicks.add("group") },
                onReadRecordClick = { clicks.add("record") },
            )
        }
        node(R.string.change_origin).assertDoesNotExist()
        node(R.string.view_toc).performClick()
        node(R.string.read_record).performClick()
        node(R.string.already_in_bookshelf).performClick()
        compose.runOnIdle { assertEquals(listOf("toc", "record", "shelf"), clicks) }
    }

    @Test
    fun restoredCoverRuleSheetCannotRevealNetworkControls() {
        val intents = mutableListOf<CoverConfigIntent>()
        render {
            CoverConfigScreen(
                state = CoverConfigUiState(activeSheet = CoverConfigSheet.Rule),
                onIntent = intents::add, onBackClick = {}, onNavigateToCoverAlbums = {},
            )
        }
        node(R.string.only_wifi).assertDoesNotExist()
        node(R.string.cover_rule).assertDoesNotExist()
        node(R.string.network_book_badge_setting).assertDoesNotExist()
        node(R.string.default_cover).performClick()
        compose.runOnIdle {
            assertEquals(listOf(CoverConfigIntent.ShowSheet(CoverConfigSheet.Album)), intents)
        }
    }

    @Test
    fun ruleExportOffersLocalFolderWithoutLinkUpload() {
        var selected = false
        render {
            FilePickerSheet(
                show = true, onDismissRequest = {}, onSelectSysDir = { selected = true },
                allowExtensions = arrayOf("json"),
            )
        }
        node(R.string.upload_url).assertDoesNotExist()
        node(R.string.manual_input).assertDoesNotExist()
        node(R.string.sys_folder_picker).performClick()
        compose.runOnIdle { assertEquals(true, selected) }
    }

    @Test
    fun contentsMenuKeepsRefreshWithoutChapterDownloads() {
        val intents = mutableListOf<TocIntent>()
        render {
            RoundDropdownMenu(expanded = true, onDismissRequest = {}) {
                ReaderBookTocMenu(
                    state = TocUiState(), onIntent = intents::add,
                    onDismiss = {}, onEditLocalTocRule = {},
                )
            }
        }
        node(R.string.download_all).assertDoesNotExist()
        node(R.string.update_toc).performClick()
        compose.runOnIdle { assertEquals(listOf(TocIntent.UpdateToc), intents) }
    }

    private fun node(resource: Int) = compose.onNodeWithText(
        RuntimeEnvironment.getApplication().getString(resource)
    )

    private fun render(content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                AppTheme(AppUiConfiguration(), applyBackground = false, content = content)
            }
        }
    }
}
