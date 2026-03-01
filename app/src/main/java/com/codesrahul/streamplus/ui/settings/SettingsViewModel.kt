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

import com.codesrahul.streamplus.data.local.PlaylistEntity
import com.codesrahul.streamplus.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.first

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    /** One-shot snapshot of all playlists for export. */
    suspend fun getAllPlaylistsOnce(): List<PlaylistEntity> =
        playlistRepository.getAllPlaylists().first()

    /** Insert a single playlist from an import operation. */
    fun importPlaylist(name: String, url: String, userAgent: String, icon: String, color: Int) {
        viewModelScope.launch {
            playlistRepository.insertPlaylist(
                PlaylistEntity(name = name, url = url, userAgent = userAgent, icon = icon, color = color)
            )
        }
    }


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

    fun setDecoderPriority(priority: Int) {
        viewModelScope.launch { settingsRepository.setDecoderPriority(priority) }
    }

    fun setDolbyVision(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDolbyVision(enabled) }
    }

    fun setDisableFrameRateCheck(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDisableFrameRateCheck(enabled) }
    }

    fun setAutoPip(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPip(enabled) }
    }

    fun setSilentSkipping(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSilentSkipping(enabled) }
    }

    fun setHideNavLabels(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHideNavLabels(enabled) }
    }

    fun setDefaultTab(tab: String) {
        viewModelScope.launch { settingsRepository.setDefaultTab(tab) }
    }

    fun setVideoQuality(quality: String) {
        viewModelScope.launch { settingsRepository.setVideoQuality(quality) }
    }

    fun setAutoFrameRate(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoFrameRate(enabled) }
    }

    fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSkipSilence(enabled) }
    }

    fun setRepeatMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRepeatMode(enabled) }
    }

    fun setDefaultPlaylistIcon(icon: String) {
        viewModelScope.launch { settingsRepository.setDefaultPlaylistIcon(icon) }
    }

    fun setPlaylistSortOrder(order: String) {
        viewModelScope.launch { settingsRepository.setPlaylistSortOrder(order) }
    }

    fun setDefaultAudioLanguage(language: String) {
        viewModelScope.launch { settingsRepository.setDefaultAudioLanguage(language) }
    }

    fun setNetworkBufferSize(seconds: Int) {
        viewModelScope.launch { settingsRepository.setNetworkBufferSize(seconds) }
    }

    fun setForceWidevineL3(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setForceWidevineL3(enabled) }
    }

    fun setApplyEmbeddedSubtitles(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setApplyEmbeddedSubtitles(enabled) }
    }

    fun setUseBoldTypeface(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUseBoldTypeface(enabled) }
    }

    fun setGalleryLayout(layout: String) {
        viewModelScope.launch { settingsRepository.setGalleryLayout(layout) }
    }

    fun setFolderSortField(field: String) {
        viewModelScope.launch { settingsRepository.setFolderSortField(field) }
    }

    fun setFolderSortAsc(asc: Boolean) {
        viewModelScope.launch { settingsRepository.setFolderSortAsc(asc) }
    }

    fun setVideoSortField(field: String) {
        viewModelScope.launch { settingsRepository.setVideoSortField(field) }
    }

    fun setVideoSortAsc(asc: Boolean) {
        viewModelScope.launch { settingsRepository.setVideoSortAsc(asc) }
    }

    fun setShowFolderPath(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowFolderPath(show) }
    }

    fun setShowFolderCount(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowFolderCount(show) }
    }

    fun setShowVideoThumbnail(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowVideoThumbnail(show) }
    }

    fun setShowVideoResolution(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowVideoResolution(show) }
    }

    fun setShowVideoDuration(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowVideoDuration(show) }
    }

    fun setShowVideoSize(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowVideoSize(show) }
    }
}
