package io.legado.app.ui.config.backupConfig

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.R
import io.legado.app.domain.gateway.BackupSettingsGateway
import io.legado.app.domain.model.settings.BackupSettings
import io.legado.app.domain.usecase.BackupRestoreUseCase
import io.legado.app.utils.isContentScheme
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupConfigViewModel(
    private val settingsGateway: BackupSettingsGateway,
    private val backupRestoreUseCase: BackupRestoreUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        BackupConfigUiState(
            settings = settingsGateway.currentSettings,
            ignoreItems = loadIgnoreItems(),
            backupIgnoreItems = loadBackupIgnoreItems(),
            dbIgnoreItems = loadDbIgnoreItems(),
            backupDbIgnoreItems = loadBackupDbIgnoreItems(),
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<BackupConfigEffect>(extraBufferCapacity = 16)
    val effects = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsGateway.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onIntent(intent: BackupConfigIntent) {
        when (intent) {
            is BackupConfigIntent.SetOnlyLatestBackup ->
                update { it.copy(onlyLatestBackup = intent.value) }
            is BackupConfigIntent.OpenSheet -> _uiState.update { it.copy(activeSheet = intent.sheet) }
            BackupConfigIntent.DismissSheet -> _uiState.update { it.copy(activeSheet = null) }
            BackupConfigIntent.OpenIgnoreDialog ->
                _uiState.update { it.copy(activeSheet = BackupConfigSheet.IgnoreRestoreItems) }
            is BackupConfigIntent.ToggleIgnoreItem -> toggleIgnoreItem(intent.key, intent.value)
            BackupConfigIntent.SaveIgnoreItems -> saveIgnoreItems()
            BackupConfigIntent.OpenBackupIgnoreDialog ->
                _uiState.update { it.copy(activeSheet = BackupConfigSheet.IgnoreBackupItems) }

            is BackupConfigIntent.ToggleBackupIgnoreItem -> toggleBackupIgnoreItem(
                intent.key,
                intent.value
            )

            BackupConfigIntent.SaveBackupIgnoreItems -> saveBackupIgnoreItems()
            is BackupConfigIntent.ToggleDbIgnoreItem -> toggleDbIgnoreItem(intent.key, intent.value)
            BackupConfigIntent.SaveDbIgnoreItems -> saveDbIgnoreItems()
            is BackupConfigIntent.ToggleBackupDbIgnoreItem -> toggleBackupDbIgnoreItem(
                intent.key,
                intent.value
            )

            BackupConfigIntent.SaveBackupDbIgnoreItems -> saveBackupDbIgnoreItems()
            BackupConfigIntent.DismissDialog -> _uiState.update { it.copy(activeDialog = null) }
            BackupConfigIntent.SelectBackupDirectory -> launchDirectoryPicker(runBackup = false)
            BackupConfigIntent.SelectBackupAndRunDirectory -> launchDirectoryPicker(runBackup = true)
            is BackupConfigIntent.BackupDirectorySelected -> saveBackupPath(intent.path, intent.runBackup)
            is BackupConfigIntent.RequestBackup -> requestBackup(intent.mode)
            is BackupConfigIntent.PerformBackup -> performBackup(intent.path, intent.mode)
            BackupConfigIntent.RequestLocalRestore -> requestLocalRestore()
            is BackupConfigIntent.RestoreLocal -> restoreLocal(intent.uri)
            BackupConfigIntent.RequestImportOldData ->
                _effects.tryEmit(BackupConfigEffect.LaunchImportOldDataPicker)
        }
    }

    private fun update(transform: (BackupSettings) -> BackupSettings) {
        viewModelScope.launch { settingsGateway.update(transform) }
    }

    private fun toggleIgnoreItem(key: String, value: Boolean) {
        _uiState.update { state ->
            state.copy(
                ignoreItems = state.ignoreItems.map { item ->
                    if (item.key == key) item.copy(checked = value) else item
                }.toImmutableList()
            )
        }
    }

    private fun saveIgnoreItems() {
        _uiState.value.ignoreItems.forEach { item ->
            io.legado.app.help.storage.BackupConfig.ignoreConfig[item.key] = item.checked
        }
        _uiState.value.dbIgnoreItems.forEach { item ->
            io.legado.app.help.storage.BackupConfig.dbIgnoreConfig[item.key] = item.checked
        }
        io.legado.app.help.storage.BackupConfig.saveIgnoreConfig()
        io.legado.app.help.storage.BackupConfig.saveDbIgnoreConfig()
        _uiState.update { it.copy(activeSheet = null) }
    }

    private fun toggleBackupIgnoreItem(key: String, value: Boolean) {
        _uiState.update { state ->
            state.copy(
                backupIgnoreItems = state.backupIgnoreItems.map { item ->
                    if (item.key == key) item.copy(checked = value) else item
                }.toImmutableList()
            )
        }
    }

    private fun saveBackupIgnoreItems() {
        _uiState.value.backupIgnoreItems.forEach { item ->
            io.legado.app.help.storage.BackupConfig.backupIgnoreConfig[item.key] = item.checked
        }
        _uiState.value.backupDbIgnoreItems.forEach { item ->
            io.legado.app.help.storage.BackupConfig.backupDbIgnoreConfig[item.key] = item.checked
        }
        io.legado.app.help.storage.BackupConfig.saveBackupIgnoreConfig()
        io.legado.app.help.storage.BackupConfig.saveBackupDbIgnoreConfig()
        _uiState.update { it.copy(activeSheet = null) }
    }

    private fun toggleDbIgnoreItem(key: String, value: Boolean) {
        _uiState.update { state ->
            state.copy(
                dbIgnoreItems = state.dbIgnoreItems.map { item ->
                    if (item.key == key) item.copy(checked = value) else item
                }.toImmutableList()
            )
        }
    }

    private fun saveDbIgnoreItems() {
        _uiState.value.dbIgnoreItems.forEach { item ->
            io.legado.app.help.storage.BackupConfig.dbIgnoreConfig[item.key] = item.checked
        }
        io.legado.app.help.storage.BackupConfig.saveDbIgnoreConfig()
    }

    private fun toggleBackupDbIgnoreItem(key: String, value: Boolean) {
        _uiState.update { state ->
            state.copy(
                backupDbIgnoreItems = state.backupDbIgnoreItems.map { item ->
                    if (item.key == key) item.copy(checked = value) else item
                }.toImmutableList()
            )
        }
    }

    private fun saveBackupDbIgnoreItems() {
        _uiState.value.backupDbIgnoreItems.forEach { item ->
            io.legado.app.help.storage.BackupConfig.backupDbIgnoreConfig[item.key] = item.checked
        }
        io.legado.app.help.storage.BackupConfig.saveBackupDbIgnoreConfig()
    }

    private fun launchDirectoryPicker(runBackup: Boolean) {
        _uiState.update { it.copy(activeSheet = null) }
        _effects.tryEmit(
            if (runBackup) BackupConfigEffect.LaunchBackupAndRunDirectoryPicker
            else BackupConfigEffect.LaunchBackupDirectoryPicker
        )
    }

    private fun saveBackupPath(path: String, runBackup: Boolean) {
        viewModelScope.launch {
            settingsGateway.update { it.copy(backupPath = path) }
            if (runBackup && path.isNotEmpty()) requestBackup("local", path)
        }
    }

    private fun requestBackup(mode: String, selectedPath: String? = null) {
        _uiState.update { it.copy(activeSheet = null) }
        val path = selectedPath ?: _uiState.value.settings.backupPath.orEmpty()
        if (path.isEmpty()) {
            launchDirectoryPicker(runBackup = true)
            return
        }
        if (path.isNotEmpty() && !path.isContentScheme()) {
            _effects.tryEmit(BackupConfigEffect.RequestStoragePermission(path, mode))
        } else {
            performBackup(path, mode)
        }
    }

    private fun performBackup(path: String, mode: String) {
        _uiState.update { it.copy(activeDialog = BackupConfigDialog.Loading(R.string.backup)) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { backupRestoreUseCase.backup(path, "local") }
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(activeDialog = null) }
                        _effects.tryEmit(BackupConfigEffect.ShowMessage(R.string.backup_success))
                    }
                }
                .onFailure { error ->
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(activeDialog = null) }
                        _effects.tryEmit(
                            BackupConfigEffect.ShowMessage(R.string.backup_fail, error.localizedMessage)
                        )
                    }
                }
        }
    }

    private fun requestLocalRestore() {
        _uiState.update { it.copy(activeSheet = null, activeDialog = null) }
        _effects.tryEmit(BackupConfigEffect.LaunchRestoreFilePicker)
    }

    private fun restoreLocal(uri: String) {
        _uiState.update { it.copy(activeDialog = BackupConfigDialog.Loading(R.string.on_restore)) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { backupRestoreUseCase.restoreLocal(Uri.parse(uri).toString()) }
                .fold(
                    onSuccess = { finishRestoreSuccess() },
                    onFailure = { finishRestoreFailure(it.localizedMessage) },
                )
        }
    }

    private suspend fun finishRestoreSuccess() = withContext(Dispatchers.Main) {
        _uiState.update { it.copy(activeDialog = null) }
        _effects.tryEmit(BackupConfigEffect.ShowMessage(R.string.restore_success))
    }

    private suspend fun finishRestoreFailure(message: String?) =
        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(activeDialog = null) }
            _effects.tryEmit(
                BackupConfigEffect.ShowMessage(
                    R.string.restore_fail_with_error,
                    message,
                )
            )
        }

    private companion object {
        fun loadIgnoreItems() = io.legado.app.help.storage.BackupConfig.ignoreKeys
            .mapIndexed { index, key ->
                BackupIgnoreItem(
                    key = key,
                    title = io.legado.app.help.storage.BackupConfig.ignoreTitle[index],
                    checked = io.legado.app.help.storage.BackupConfig.ignoreConfig[key] ?: false,
                )
            }
            .toImmutableList()

        fun loadBackupIgnoreItems() = io.legado.app.help.storage.BackupConfig.backupIgnoreKeys
            .mapIndexed { index, key ->
                BackupIgnoreItem(
                    key = key,
                    title = io.legado.app.help.storage.BackupConfig.backupIgnoreTitle[index],
                    checked = io.legado.app.help.storage.BackupConfig.backupIgnoreConfig[key]
                        ?: false,
                )
            }
            .toImmutableList()

        fun loadDbIgnoreItems() = io.legado.app.help.storage.BackupConfig.dbIgnoreKeys
            .mapIndexed { index, key ->
                BackupIgnoreItem(
                    key = key,
                    title = io.legado.app.help.storage.BackupConfig.dbIgnoreTitle[index],
                    checked = io.legado.app.help.storage.BackupConfig.dbIgnoreConfig[key] ?: false,
                )
            }
            .toImmutableList()

        fun loadBackupDbIgnoreItems() = io.legado.app.help.storage.BackupConfig.backupDbIgnoreKeys
            .mapIndexed { index, key ->
                BackupIgnoreItem(
                    key = key,
                    title = io.legado.app.help.storage.BackupConfig.backupDbIgnoreTitle[index],
                    checked = io.legado.app.help.storage.BackupConfig.backupDbIgnoreConfig[key]
                        ?: false,
                )
            }
            .toImmutableList()
    }
}
