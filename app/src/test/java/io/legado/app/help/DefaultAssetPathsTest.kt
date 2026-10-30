package io.legado.app.help

import android.app.Application
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import splitties.init.injectAsAppCtx

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class DefaultAssetPathsTest {
    @Before
    fun setUp() {
        RuntimeEnvironment.getApplication().injectAsAppCtx()
    }

    @Test
    fun bundledDefaultsLoadWithoutHostFilesystemSeparators() {
        assertTrue(DefaultData.readConfigs.isNotEmpty())
        assertTrue(DefaultData.txtTocRules.isNotEmpty())
        assertTrue(DefaultData.themeConfigs.isNotEmpty())
        assertTrue(DefaultData.keyboardAssists.isNotEmpty())
    }

}
