package io.legado.app.ui.main.bookshelf

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.legado.app.R
import io.legado.app.constant.BookType
import io.legado.app.data.repository.AppShellSettingsRepository
import io.legado.app.data.repository.ThemeSettingsRepository
import io.legado.app.domain.gateway.AppShellSettingsGateway
import io.legado.app.domain.gateway.ThemeSettingsGateway
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.help.config.AppConfigStore
import io.legado.app.ui.theme.AppTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class BookshelfBookActionsTest {
    @get:Rule val compose = createComposeRule()

    private val state = mutableStateOf(BookshelfUiState(
        items = persistentListOf(book("A").toUiItem(), book("B").toUiItem()),
        selectedBookUrls = persistentSetOf("B"),
        activeOverlay = BookshelfOverlay.BookActions("A", "cover-A"),
    ))
    private val deletions = mutableListOf<BookshelfIntent.DeleteBooks>()
    private val details = mutableListOf<Pair<String, String?>>()

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
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                AppTheme(AppUiConfiguration(), applyBackground = false) {
                    BookshelfBookActionOverlays(
                        uiState = state.value,
                        onIntent = { intent ->
                            when (intent) {
                                is BookshelfIntent.ShowOverlay -> state.value = state.value.copy(activeOverlay = intent.overlay)
                                BookshelfIntent.DismissOverlay -> state.value = state.value.copy(activeOverlay = null)
                                is BookshelfIntent.DeleteBooks -> deletions.add(intent)
                                else -> Unit
                            }
                        },
                        onBookInfo = { book, key -> details.add(book.bookUrl to key) },
                    )
                }
            }
        }
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun deleteTargetsLongPressedBookAndKeepsSourceByDefault() {
        click(R.string.delete)
        sourceCheckbox().assertIsOff()
        compose.runOnIdle {
            // 确认期间即使多选状态变化，也只能删除长按的那本书。
            state.value = state.value.copy(selectedBookUrls = persistentSetOf("A", "B"))
        }
        click(R.string.delete)
        compose.runOnIdle {
            assertEquals(listOf(BookshelfIntent.DeleteBooks(setOf("A"), false)), deletions)
            assertEquals(null, state.value.activeOverlay)
        }
    }

    @Test
    fun cancelDoesNotDeleteAndReopeningResetsSourceCheckbox() {
        click(R.string.delete)
        sourceCheckbox().performClick().assertIsOn()
        click(R.string.cancel)
        compose.runOnIdle {
            assertTrue(deletions.isEmpty())
            state.value = state.value.copy(activeOverlay = BookshelfOverlay.BookActions("A"))
        }
        click(R.string.delete)
        sourceCheckbox().assertIsOff()
        sourceCheckbox().performClick()
        click(R.string.delete)
        compose.runOnIdle {
            assertEquals(listOf(BookshelfIntent.DeleteBooks(setOf("A"), true)), deletions)
        }
    }

    @Test
    fun informationActionPreservesBookAndCoverNavigation() {
        click(R.string.book_info)
        compose.runOnIdle {
            assertEquals(listOf("A" to "cover-A"), details)
            assertEquals(null, state.value.activeOverlay)
            assertTrue(deletions.isEmpty())
        }
    }

    private fun click(resource: Int) = compose.onNodeWithText(
        RuntimeEnvironment.getApplication().getString(resource)
    ).performClick()

    private fun sourceCheckbox() = compose.onNodeWithContentDescription(
        RuntimeEnvironment.getApplication().getString(R.string.delete_book_file)
    )

    private fun book(url: String) = BookShelfItem(
        bookUrl = url, name = "Book $url", author = "Author", origin = "local", originName = "local",
        coverUrl = null, customCoverUrl = null, durChapterTitle = null, durChapterTime = 0L,
        durChapterPos = 0, latestChapterTitle = null, latestChapterTime = 0L, lastCheckCount = 0,
        totalChapterNum = 1, durChapterIndex = 0, type = BookType.local, group = 0L, order = 0,
    )
}
