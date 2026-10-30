package io.legado.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.net.toUri
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.domain.gateway.AppShellSettingsGateway
import io.legado.app.domain.gateway.OtherSettingsGateway
import io.legado.app.domain.gateway.ThemeSettingsGateway
import io.legado.app.help.config.LocalConfig
import io.legado.app.help.storage.Restore
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx

class OnboardingViewModel(
    private val themeSettingsGateway: ThemeSettingsGateway,
    private val appShellSettingsGateway: AppShellSettingsGateway,
    private val otherSettingsGateway: OtherSettingsGateway,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            appAccessPassword = LocalConfig.password ?: "",
            bookFolderUri = otherSettingsGateway.currentSettings.defaultBookTreeUri,
            theme = themeSettingsGateway.currentSettings,
            themeMode = appShellSettingsGateway.currentSettings.themeMode,
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<OnboardingEffect>(extraBufferCapacity = 16)
    val effects = _effects.asSharedFlow()

    private var busyJob: Job? = null

    init {
        viewModelScope.launch {
            val privacy = withContext(Dispatchers.IO) {
                runCatching { appCtx.assets.open("privacyPolicy.md").readBytes().decodeToString() }
                    .getOrDefault("")
            }
            val disclaimer = withContext(Dispatchers.IO) {
                runCatching { appCtx.assets.open("disclaimer.md").readBytes().decodeToString() }
                    .getOrDefault("")
            }
            _uiState.update { it.copy(privacyPolicy = privacy, disclaimer = disclaimer) }
        }
        viewModelScope.launch {
            themeSettingsGateway.settings.collect { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
    }

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.Next -> nextPage()
            OnboardingIntent.Prev -> previousPage()
            is OnboardingIntent.UpdateAppAccessPassword -> {
                LocalConfig.password = intent.value
                _uiState.update { it.copy(appAccessPassword = intent.value) }
            }
            OnboardingIntent.StartLocalRestore -> {
                _uiState.update { it.copy(restoreErrorMessage = null) }
                _effects.tryEmit(OnboardingEffect.OpenRestoreFilePicker)
            }
            is OnboardingIntent.RestoreLocalFile -> restoreLocal(intent.uri)
            OnboardingIntent.SelectFolder -> _effects.tryEmit(OnboardingEffect.OpenBookFolderPicker)
            is OnboardingIntent.SelectBookFolder -> viewModelScope.launch {
                otherSettingsGateway.update {
                    it.copy(defaultBookTreeUri = intent.uri)
                }
                _uiState.update { it.copy(bookFolderUri = intent.uri) }
            }
            is OnboardingIntent.SelectTheme -> selectTheme(intent.value)
            is OnboardingIntent.SetThemeMode -> viewModelScope.launch {
                appShellSettingsGateway.update { it.copy(themeMode = intent.value) }
                _uiState.update { it.copy(themeMode = intent.value) }
                _effects.tryEmit(OnboardingEffect.ApplyDayNight)
            }
            OnboardingIntent.DismissRestoreError ->
                _uiState.update { it.copy(restoreErrorMessage = null) }
            OnboardingIntent.CancelBusy -> {
                busyJob?.cancel()
                busyJob = null
                _uiState.update { it.copy(busyText = null) }
            }
        }
    }

    private fun nextPage() {
        val state = _uiState.value
        if (state.page == 0) {
            LocalConfig.privacyPolicyOk = true
        }
        if (state.page >= state.pageCount - 1) {
            _effects.tryEmit(OnboardingEffect.NavigateHome)
        } else {
            _uiState.update { it.copy(page = it.page + 1) }
        }
    }

    private fun previousPage() {
        val state = _uiState.value
        if (state.page > 0) {
            _uiState.update { it.copy(page = it.page - 1) }
        } else {
            _effects.tryEmit(OnboardingEffect.Finish)
        }
    }

    private fun restoreLocal(uriString: String) {
        busyJob?.cancel()
        busyJob = viewModelScope.launch {
            _uiState.update {
                it.copy(busyText = appCtx.getString(R.string.onboarding_restoring))
            }
            try {
                Restore.restore(appCtx, uriString.toUri())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.put("本地恢复出错\n${e.localizedMessage}", e)
                appCtx.toastOnUi(
                    appCtx.getString(
                        R.string.onboarding_local_restore_error,
                        e.localizedMessage ?: ""
                    )
                )
            } finally {
                _uiState.update { it.copy(busyText = null) }
            }
        }
    }

    private fun selectTheme(value: String) {
        val theme = _uiState.value.theme
        if (value == "13" &&
            (theme.backgroundImageLight.isNullOrEmpty() || theme.backgroundImageDark.isNullOrEmpty())
        ) {
            _effects.tryEmit(OnboardingEffect.ShowToast(R.string.transparent_theme_alarm))
            return
        }
        viewModelScope.launch {
            themeSettingsGateway.update {
                it.copy(
                    appTheme = value,
                    containerOpacity = if (value == "13") 0 else it.containerOpacity,
                )
            }
        }
    }
}
