package com.codesrahul.streamplus.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class ThemeStyle {
    DYNAMIC, IGNITE, FROSTY, GROVE, MIDNIGHT, SUNSET, OCEAN, CARBON, AMOLED
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeStyle: ThemeStyle = ThemeStyle.DYNAMIC,
    val dynamicTheme: Boolean = false,
    val hidePlaylistUri: Boolean = false,
    val hideChannelImage: Boolean = false,
    val userAgent: String = "ExoPlayerLib/2.19.1",
    val allowCleartext: Boolean = true,
    val skipSslVerification: Boolean = true,
    val enableTunneling: Boolean = false,
    val preferSoftwareDecoder: Boolean = false,
    val forceLandscape: Boolean = false,
    val volumeGestures: Boolean = false,
    val brightnessGestures: Boolean = false,
    val resumePlaying: Boolean = false,
    val playlistCache: Boolean = false,
    val lastResizeMode: Int = 0, // 0=Fit,1=Fill,2=Zoom
    val decoderPriority: Int = 0, // 0=Device, 1=App, 2=DeviceOnly
    val dolbyVision: Boolean = false,
    val disableFrameRateCheck: Boolean = false,
    val autoPip: Boolean = false,
    val silentSkipping: Boolean = false,
    
    // New Screenshot features
    val hideNavLabels: Boolean = false,
    val defaultTab: String = "playlist",
    val videoQuality: String = "Auto (Recommended)",
    val autoFrameRate: Boolean = false,
    val skipSilence: Boolean = false,
    val repeatMode: Boolean = false,
    val defaultPlaylistIcon: String = "List",
    val playlistSortOrder: String = "Name",
    val defaultAudioLanguage: String = "Auto",
    val networkBufferSize: Int = 30, // seconds
    val forceWidevineL3: Boolean = true,
    
    // Accessibility
    val applyEmbeddedSubtitles: Boolean = true,
    val useBoldTypeface: Boolean = false
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val DYNAMIC_THEME = booleanPreferencesKey("dynamic_theme")
        val HIDE_PLAYLIST_URI = booleanPreferencesKey("hide_playlist_uri")
        val HIDE_CHANNEL_IMAGE = booleanPreferencesKey("hide_channel_image")
        val USER_AGENT = stringPreferencesKey("user_agent")
        val ALLOW_CLEARTEXT = booleanPreferencesKey("allow_cleartext")
        val SKIP_SSL_VERIFICATION = booleanPreferencesKey("skip_ssl_verification")
        val ENABLE_TUNNELING = booleanPreferencesKey("enable_tunneling")
        val PREFER_SOFTWARE_DECODER = booleanPreferencesKey("prefer_software_decoder")
        val FORCE_LANDSCAPE = booleanPreferencesKey("force_landscape")
        val VOLUME_GESTURES = booleanPreferencesKey("volume_gestures")
        val BRIGHTNESS_GESTURES = booleanPreferencesKey("brightness_gestures")
        val RESUME_PLAYING = booleanPreferencesKey("resume_playing")
        val PLAYLIST_CACHE = booleanPreferencesKey("playlist_cache")
        val LAST_RESIZE_MODE = intPreferencesKey("last_resize_mode")
        val DECODER_PRIORITY = intPreferencesKey("decoder_priority")
        val DOLBY_VISION = booleanPreferencesKey("dolby_vision")
        val DISABLE_FRAME_RATE_CHECK = booleanPreferencesKey("disable_frame_rate_check")
        val AUTO_PIP = booleanPreferencesKey("auto_pip")
        val SILENT_SKIPPING = booleanPreferencesKey("silent_skipping")
        
        // New keys
        val HIDE_NAV_LABELS = booleanPreferencesKey("hide_nav_labels")
        val DEFAULT_TAB = stringPreferencesKey("default_tab")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val AUTO_FRAME_RATE = booleanPreferencesKey("auto_frame_rate")
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val REPEAT_MODE = booleanPreferencesKey("repeat_mode")
        val DEFAULT_PLAYLIST_ICON = stringPreferencesKey("default_playlist_icon")
        val PLAYLIST_SORT_ORDER = stringPreferencesKey("playlist_sort_order")
        val DEFAULT_AUDIO_LANGUAGE = stringPreferencesKey("default_audio_language")
        val NETWORK_BUFFER_SIZE = intPreferencesKey("network_buffer_size")
        val FORCE_WIDEVINE_L3 = booleanPreferencesKey("force_widevine_l3")
        
        // Accessibility
        val APPLY_EMBEDDED_SUBTITLES = booleanPreferencesKey("apply_embedded_subtitles")
        val USE_BOLD_TYPEFACE = booleanPreferencesKey("use_bold_typeface")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = try {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) { ThemeMode.SYSTEM },
            themeStyle = try {
                ThemeStyle.valueOf(prefs[Keys.THEME_STYLE] ?: ThemeStyle.DYNAMIC.name)
            } catch (e: Exception) { ThemeStyle.DYNAMIC },
            dynamicTheme = prefs[Keys.DYNAMIC_THEME] ?: true,
            hidePlaylistUri = prefs[Keys.HIDE_PLAYLIST_URI] ?: false,
            hideChannelImage = prefs[Keys.HIDE_CHANNEL_IMAGE] ?: false,
            userAgent = prefs[Keys.USER_AGENT] ?: "ExoPlayerLib/2.19.1",
            allowCleartext = prefs[Keys.ALLOW_CLEARTEXT] ?: true,
            skipSslVerification = prefs[Keys.SKIP_SSL_VERIFICATION] ?: true,
            enableTunneling = prefs[Keys.ENABLE_TUNNELING] ?: false,
            preferSoftwareDecoder = prefs[Keys.PREFER_SOFTWARE_DECODER] ?: false,
            forceLandscape = prefs[Keys.FORCE_LANDSCAPE] ?: false,
            volumeGestures = prefs[Keys.VOLUME_GESTURES] ?: false,
            brightnessGestures = prefs[Keys.BRIGHTNESS_GESTURES] ?: false,
            resumePlaying = prefs[Keys.RESUME_PLAYING] ?: false,
            playlistCache = prefs[Keys.PLAYLIST_CACHE] ?: false,
            lastResizeMode = prefs[Keys.LAST_RESIZE_MODE] ?: 0,
            decoderPriority = prefs[Keys.DECODER_PRIORITY] ?: 0,
            dolbyVision = prefs[Keys.DOLBY_VISION] ?: false,
            disableFrameRateCheck = prefs[Keys.DISABLE_FRAME_RATE_CHECK] ?: false,
            autoPip = prefs[Keys.AUTO_PIP] ?: false,
            silentSkipping = prefs[Keys.SILENT_SKIPPING] ?: false,
            
            // New Screenshot features
            hideNavLabels = prefs[Keys.HIDE_NAV_LABELS] ?: false,
            defaultTab = prefs[Keys.DEFAULT_TAB] ?: "playlist",
            videoQuality = prefs[Keys.VIDEO_QUALITY] ?: "Auto (Recommended)",
            autoFrameRate = prefs[Keys.AUTO_FRAME_RATE] ?: false,
            skipSilence = prefs[Keys.SKIP_SILENCE] ?: false,
            repeatMode = prefs[Keys.REPEAT_MODE] ?: false,
            defaultPlaylistIcon = prefs[Keys.DEFAULT_PLAYLIST_ICON] ?: "List",
            playlistSortOrder = prefs[Keys.PLAYLIST_SORT_ORDER] ?: "Name",
            defaultAudioLanguage = prefs[Keys.DEFAULT_AUDIO_LANGUAGE] ?: "Auto",
            networkBufferSize = prefs[Keys.NETWORK_BUFFER_SIZE] ?: 30,
            forceWidevineL3 = prefs[Keys.FORCE_WIDEVINE_L3] ?: true,
            
            // Accessibility
            applyEmbeddedSubtitles = prefs[Keys.APPLY_EMBEDDED_SUBTITLES] ?: true,
            useBoldTypeface = prefs[Keys.USE_BOLD_TYPEFACE] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setThemeStyle(style: ThemeStyle) {
        context.dataStore.edit { it[Keys.THEME_STYLE] = style.name }
    }

    suspend fun setDynamicTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_THEME] = enabled }
    }

    suspend fun setHidePlaylistUri(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_PLAYLIST_URI] = enabled }
    }
    
    suspend fun setHideChannelImage(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_CHANNEL_IMAGE] = enabled }
    }

    suspend fun setAllowCleartext(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALLOW_CLEARTEXT] = enabled }
    }

    suspend fun setSkipSslVerification(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SKIP_SSL_VERIFICATION] = enabled }
    }

    suspend fun setEnableTunneling(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENABLE_TUNNELING] = enabled }
    }

    suspend fun setPreferSoftwareDecoder(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PREFER_SOFTWARE_DECODER] = enabled }
    }

    suspend fun setForceLandscape(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FORCE_LANDSCAPE] = enabled }
    }

    suspend fun setVolumeGestures(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOLUME_GESTURES] = enabled }
    }

    suspend fun setBrightnessGestures(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BRIGHTNESS_GESTURES] = enabled }
    }

    suspend fun setResumePlaying(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_PLAYING] = enabled }
    }

    suspend fun setPlaylistCache(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PLAYLIST_CACHE] = enabled }
    }

    suspend fun setLastResizeMode(mode: Int) {
        context.dataStore.edit { it[Keys.LAST_RESIZE_MODE] = mode }
    }

    suspend fun setDecoderPriority(priority: Int) {
        context.dataStore.edit { it[Keys.DECODER_PRIORITY] = priority }
    }

    suspend fun setDolbyVision(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DOLBY_VISION] = enabled }
    }

    suspend fun setDisableFrameRateCheck(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DISABLE_FRAME_RATE_CHECK] = enabled }
    }

    suspend fun setAutoPip(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PIP] = enabled }
    }

    suspend fun setSilentSkipping(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SILENT_SKIPPING] = enabled }
    }

    suspend fun setHideNavLabels(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_NAV_LABELS] = enabled }
    }

    suspend fun setDefaultTab(tab: String) {
        context.dataStore.edit { it[Keys.DEFAULT_TAB] = tab }
    }

    suspend fun setVideoQuality(quality: String) {
        context.dataStore.edit { it[Keys.VIDEO_QUALITY] = quality }
    }

    suspend fun setAutoFrameRate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_FRAME_RATE] = enabled }
    }

    suspend fun setSkipSilence(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SKIP_SILENCE] = enabled }
    }

    suspend fun setRepeatMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REPEAT_MODE] = enabled }
    }

    suspend fun setDefaultPlaylistIcon(icon: String) {
        context.dataStore.edit { it[Keys.DEFAULT_PLAYLIST_ICON] = icon }
    }

    suspend fun setPlaylistSortOrder(order: String) {
        context.dataStore.edit { it[Keys.PLAYLIST_SORT_ORDER] = order }
    }

    suspend fun setDefaultAudioLanguage(language: String) {
        context.dataStore.edit { it[Keys.DEFAULT_AUDIO_LANGUAGE] = language }
    }

    suspend fun setNetworkBufferSize(seconds: Int) {
        context.dataStore.edit { it[Keys.NETWORK_BUFFER_SIZE] = seconds }
    }

    suspend fun setForceWidevineL3(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FORCE_WIDEVINE_L3] = enabled }
    }

    suspend fun setApplyEmbeddedSubtitles(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APPLY_EMBEDDED_SUBTITLES] = enabled }
    }

    suspend fun setUseBoldTypeface(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_BOLD_TYPEFACE] = enabled }
    }

    suspend fun factoryReset() {
        context.dataStore.edit { it.clear() }
    }
}
