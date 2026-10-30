package io.legado.app.data.repository

import io.legado.app.constant.PreferKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LocalOnlySettingsTest {
    @Test
    fun oldSourcePreferencesCannotReactivateOnlineBookActions() {
        val preferences = mapOf<String, Any?>(
            PreferKey.autoChangeSource to true,
            PreferKey.defaultSourceChangeAll to true,
            PreferKey.bookInfoAuthorSearchFallback to true,
        ).toTestPreferences()
        val repository = ReadSettingsRepository(
            SettingsRepository(), kotlinx.coroutines.flow.MutableStateFlow(preferences)
        )
        val readSettings = with(repository) { preferences.toReadSettings() }
        assertFalse(readSettings.autoChangeSource)
        assertFalse(readSettings.defaultSourceChangeAll)
        assertFalse(preferences.toThemeSettings().bookInfoAuthorSearchFallback)
    }

    @Test
    fun oldBackupCannotReactivateRemoteBackupOrTelemetry() {
        val restored = mapOf<String, Any?>(
            PreferKey.webDavUrl to "https://backup.example.test",
            PreferKey.webDavAccount to "account",
            PreferKey.webDavPassword to "password",
            PreferKey.syncBookProgress to true,
            PreferKey.syncBookProgressPlus to true,
            PreferKey.autoCheckNewBackup to true,
            PreferKey.backupSyncMode to "both",
            PreferKey.backupPath to "content://backups/local",
            PreferKey.webServiceAutoStart to true,
            PreferKey.autoRefresh to true,
            PreferKey.autoCheckUpdateOnStart to true,
            PreferKey.firebaseEnable to true,
        ).toTestPreferences()
        val backup = restored.toBackupSettings()
        assertEquals("", backup.webDavUrl)
        assertEquals("", backup.webDavAccount)
        assertEquals("", backup.webDavPassword)
        assertFalse(backup.syncBookProgress)
        assertFalse(backup.syncBookProgressPlus)
        assertFalse(backup.autoCheckNewBackup)
        assertEquals("local", backup.backupSyncMode)
        assertEquals("content://backups/local", backup.backupPath)
        val other = restored.toOtherSettings()
        assertFalse(other.webServiceAutoStart)
        assertFalse(other.autoRefresh)
        assertFalse(other.autoCheckUpdateOnStart)
        assertFalse(other.firebaseEnable)
    }

    @Test
    fun oldCloudVoiceAndAiAnalysisPreferencesAreIgnored() {
        val settings = mapOf<String, Any?>(
            PreferKey.ttsEngine to "cloud-engine",
            PreferKey.speechAnalysisMode to "ai",
            PreferKey.multiRoleCast to true,
            PreferKey.useMultiSpeaker to true,
            PreferKey.streamReadAloudAudio to true,
            PreferKey.ttsSpeechRate to 17,
        ).toTestPreferences().toReadAloudSettings()
        assertNull(settings.ttsEngine)
        assertEquals("rule", settings.speechAnalysisMode)
        assertFalse(settings.multiRoleCast)
        assertFalse(settings.useMultiSpeaker)
        assertFalse(settings.streamReadAloudAudio)
        assertEquals(17, settings.ttsSpeechRate)
    }
}
