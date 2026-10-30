package io.legado.app.ui.config.backupConfig

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import io.legado.app.domain.model.settings.BackupSettings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Stable
data class BackupConfigUiState(
    val settings: BackupSettings = BackupSettings(),
    val activeSheet: BackupConfigSheet? = null,
    val activeDialog: BackupConfigDialog? = null,
    val backupNames: ImmutableList<String> = persistentListOf(),
    val ignoreItems: ImmutableList<BackupIgnoreItem> = persistentListOf(),
    val backupIgnoreItems: ImmutableList<BackupIgnoreItem> = persistentListOf(),
    val dbIgnoreItems: ImmutableList<BackupIgnoreItem> = persistentListOf(),
    val backupDbIgnoreItems: ImmutableList<BackupIgnoreItem> = persistentListOf(),
)

@Stable
data class BackupIgnoreItem(
    val key: String,
    val title: String,
    val checked: Boolean,
)

sealed interface BackupConfigSheet {
    data object ChooseBackupPath : BackupConfigSheet
    data object ChooseBackupAndRun : BackupConfigSheet
    data object BackupOptions : BackupConfigSheet
    data object RestoreOptions : BackupConfigSheet
    data object IgnoreRestoreItems : BackupConfigSheet
    data object IgnoreBackupItems : BackupConfigSheet
}

sealed interface BackupConfigDialog {
    data class Loading(@StringRes val titleRes: Int) : BackupConfigDialog
}

sealed interface BackupConfigIntent {
    data class SetOnlyLatestBackup(val value: Boolean) : BackupConfigIntent
    data class OpenSheet(val sheet: BackupConfigSheet) : BackupConfigIntent
    data object DismissSheet : BackupConfigIntent
    data object OpenIgnoreDialog : BackupConfigIntent
    data class ToggleIgnoreItem(val key: String, val value: Boolean) : BackupConfigIntent
    data object SaveIgnoreItems : BackupConfigIntent
    data object OpenBackupIgnoreDialog : BackupConfigIntent
    data class ToggleBackupIgnoreItem(val key: String, val value: Boolean) : BackupConfigIntent
    data object SaveBackupIgnoreItems : BackupConfigIntent
    data class ToggleDbIgnoreItem(val key: String, val value: Boolean) : BackupConfigIntent
    data object SaveDbIgnoreItems : BackupConfigIntent
    data class ToggleBackupDbIgnoreItem(val key: String, val value: Boolean) : BackupConfigIntent
    data object SaveBackupDbIgnoreItems : BackupConfigIntent
    data object DismissDialog : BackupConfigIntent
    data object SelectBackupDirectory : BackupConfigIntent
    data object SelectBackupAndRunDirectory : BackupConfigIntent
    data class BackupDirectorySelected(val path: String, val runBackup: Boolean) : BackupConfigIntent
    data class RequestBackup(val mode: String) : BackupConfigIntent
    data class PerformBackup(val path: String, val mode: String) : BackupConfigIntent
    data object RequestLocalRestore : BackupConfigIntent
    data class RestoreLocal(val uri: String) : BackupConfigIntent
    data object RequestImportOldData : BackupConfigIntent
}

sealed interface BackupConfigEffect {
    data object LaunchBackupDirectoryPicker : BackupConfigEffect
    data object LaunchBackupAndRunDirectoryPicker : BackupConfigEffect
    data object LaunchRestoreFilePicker : BackupConfigEffect
    data object LaunchImportOldDataPicker : BackupConfigEffect
    data class RequestStoragePermission(val path: String, val mode: String) : BackupConfigEffect
    data class ShowMessage(@StringRes val messageRes: Int, val argument: String? = null) : BackupConfigEffect
}
