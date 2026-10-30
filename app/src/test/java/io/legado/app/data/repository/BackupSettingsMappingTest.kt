package io.legado.app.data.repository

import androidx.datastore.preferences.core.Preferences
import io.legado.app.constant.PreferKey
import io.legado.app.domain.model.settings.BackupSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupSettingsMappingTest {
    @Test
    fun localBackupPathAndRetentionSurviveRestoration() {
        val restored = BackupSettings(backupPath = "content://backups/local", onlyLatestBackup = false)
            .toPrefMap().toTestPreferences().toBackupSettings()
        assertEquals("content://backups/local", restored.backupPath)
        assertEquals(false, restored.onlyLatestBackup)
        assertEquals("local", restored.backupSyncMode)
    }

    @Test
    fun localBackupPathCanBeRemovedAtomically() {
        val values = captureAtomicUpdateValues(
            current = BackupSettings(backupPath = "content://old/path"),
            read = Preferences::toBackupSettings,
            toPrefMap = BackupSettings::toPrefMap,
            transform = { it.copy(backupPath = null) },
        )
        assertEquals(mapOf(PreferKey.backupPath to null), values)
    }
}
