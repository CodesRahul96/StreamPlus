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
    SYSTEM, LIGHT, DARK, PITCH_BLACK
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
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
    val playlistCache: Boolean = true,
    val defaultAspectRatio: Int = 0, // 0=Fit,1=Fill,2=Zoom
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
    val useBoldTypeface: Boolean = false,

    // Gallery Settings
    val galleryLayout: String = "Grid",
    val folderSortField: String = "Name",
    val folderSortAsc: Boolean = true,
    val videoSortField: String = "Date",
    val videoSortAsc: Boolean = false,
    val showFolderPath: Boolean = true,
    val showFolderCount: Boolean = true,
    val showVideoThumbnail: Boolean = true,
    val showVideoResolution: Boolean = true,
    val showVideoDuration: Boolean = true,
    val showVideoSize: Boolean = true,
    
    // Internal Loading State
    val isLoaded: Boolean = false
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
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
        val DEFAULT_ASPECT_RATIO = intPreferencesKey("default_aspect_ratio")
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

        // Gallery
        val GALLERY_LAYOUT = stringPreferencesKey("gallery_layout")
        val FOLDER_SORT_FIELD = stringPreferencesKey("folder_sort_field")
        val FOLDER_SORT_ASC = booleanPreferencesKey("folder_sort_asc")
        val VIDEO_SORT_FIELD = stringPreferencesKey("video_sort_field")
        val VIDEO_SORT_ASC = booleanPreferencesKey("video_sort_asc")
        val SHOW_FOLDER_PATH = booleanPreferencesKey("show_folder_path")
        val SHOW_FOLDER_COUNT = booleanPreferencesKey("show_folder_count")
        val SHOW_VIDEO_THUMBNAIL = booleanPreferencesKey("show_video_thumbnail")
        val SHOW_VIDEO_RESOLUTION = booleanPreferencesKey("show_video_resolution")
        val SHOW_VIDEO_DURATION = booleanPreferencesKey("show_video_duration")
        val SHOW_VIDEO_SIZE = booleanPreferencesKey("show_video_size")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = try {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) { ThemeMode.SYSTEM },
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
            playlistCache = prefs[Keys.PLAYLIST_CACHE] ?: true,
            defaultAspectRatio = prefs[Keys.DEFAULT_ASPECT_RATIO] ?: 0,
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
            useBoldTypeface = prefs[Keys.USE_BOLD_TYPEFACE] ?: false,

            // Gallery
            galleryLayout = prefs[Keys.GALLERY_LAYOUT] ?: "Grid",
            folderSortField = prefs[Keys.FOLDER_SORT_FIELD] ?: "Name",
            folderSortAsc = prefs[Keys.FOLDER_SORT_ASC] ?: true,
            videoSortField = prefs[Keys.VIDEO_SORT_FIELD] ?: "Date",
            videoSortAsc = prefs[Keys.VIDEO_SORT_ASC] ?: false,
            showFolderPath = prefs[Keys.SHOW_FOLDER_PATH] ?: true,
            showFolderCount = prefs[Keys.SHOW_FOLDER_COUNT] ?: true,
            showVideoThumbnail = prefs[Keys.SHOW_VIDEO_THUMBNAIL] ?: true,
            showVideoResolution = prefs[Keys.SHOW_VIDEO_RESOLUTION] ?: true,
            showVideoDuration = prefs[Keys.SHOW_VIDEO_DURATION] ?: true,
            showVideoSize = prefs[Keys.SHOW_VIDEO_SIZE] ?: true,
            isLoaded = true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
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

    suspend fun setDefaultAspectRatio(mode: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_ASPECT_RATIO] = mode }
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

    suspend fun setGalleryLayout(layout: String) {
        context.dataStore.edit { it[Keys.GALLERY_LAYOUT] = layout }
    }

    suspend fun setFolderSortField(field: String) {
        context.dataStore.edit { it[Keys.FOLDER_SORT_FIELD] = field }
    }

    suspend fun setFolderSortAsc(asc: Boolean) {
        context.dataStore.edit { it[Keys.FOLDER_SORT_ASC] = asc }
    }

    suspend fun setVideoSortField(field: String) {
        context.dataStore.edit { it[Keys.VIDEO_SORT_FIELD] = field }
    }

    suspend fun setVideoSortAsc(asc: Boolean) {
        context.dataStore.edit { it[Keys.VIDEO_SORT_ASC] = asc }
    }

    suspend fun setShowFolderPath(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_FOLDER_PATH] = show }
    }

    suspend fun setShowFolderCount(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_FOLDER_COUNT] = show }
    }

    suspend fun setShowVideoThumbnail(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_VIDEO_THUMBNAIL] = show }
    }

    suspend fun setShowVideoResolution(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_VIDEO_RESOLUTION] = show }
    }

    suspend fun setShowVideoDuration(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_VIDEO_DURATION] = show }
    }

    suspend fun setShowVideoSize(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_VIDEO_SIZE] = show }
    }

    suspend fun factoryReset() {
        context.dataStore.edit { it.clear() }
    }
}
