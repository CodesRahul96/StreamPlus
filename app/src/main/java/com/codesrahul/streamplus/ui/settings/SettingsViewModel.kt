package com.codesrahul.streamplus.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codesrahul.streamplus.data.settings.AppSettings
import com.codesrahul.streamplus.data.settings.SettingsRepository
import com.codesrahul.streamplus.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setThemeStyle(style: com.codesrahul.streamplus.data.settings.ThemeStyle) {
        viewModelScope.launch { settingsRepository.setThemeStyle(style) }
    }

    fun setDynamicTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicTheme(enabled) }
    }
    
    fun setHidePlaylistUri(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHidePlaylistUri(enabled) }
    }
    
    fun setHideChannelImage(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHideChannelImage(enabled) }
    }

    fun setAllowCleartext(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAllowCleartext(enabled) }
    }

    fun setSkipSslVerification(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSkipSslVerification(enabled) }
    }

    fun setEnableTunneling(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEnableTunneling(enabled) }
    }

    fun setPreferSoftwareDecoder(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPreferSoftwareDecoder(enabled) }
    }

    fun setForceLandscape(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setForceLandscape(enabled) }
    }

    fun setVolumeGestures(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVolumeGestures(enabled) }
    }

    fun setBrightnessGestures(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBrightnessGestures(enabled) }
    }

    fun setResumePlaying(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setResumePlaying(enabled) }
    }

    fun setPlaylistCache(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPlaylistCache(enabled) }
    }

    fun factoryReset() {
        viewModelScope.launch { settingsRepository.factoryReset() }
    }
}
