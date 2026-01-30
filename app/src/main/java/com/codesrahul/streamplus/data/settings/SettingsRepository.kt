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
    DYNAMIC, IGNITE_BLAZE, FROSTY_AURA, EARTHEN_ELEGANCE, ENCHANTED_GROVE, RED_ROSE, PITCH_BLACK
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeStyle: ThemeStyle = ThemeStyle.DYNAMIC,
    val dynamicTheme: Boolean = true,
    val hidePlaylistUri: Boolean = false,
    val hideChannelImage: Boolean = false,
    val userAgent: String = "ExoPlayerLib/2.19.1",
    val allowCleartext: Boolean = true,
    val skipSslVerification: Boolean = false,
    val enableTunneling: Boolean = false,
    val preferSoftwareDecoder: Boolean = false,
    val forceLandscape: Boolean = true,
    val volumeGestures: Boolean = true,
    val brightnessGestures: Boolean = true,
    val resumePlaying: Boolean = false,
    val playlistCache: Boolean = false,
    val lastResizeMode: Int = 0 // 0=Fit,1=Fill,2=Zoom
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
            skipSslVerification = prefs[Keys.SKIP_SSL_VERIFICATION] ?: false,
            enableTunneling = prefs[Keys.ENABLE_TUNNELING] ?: false,
            preferSoftwareDecoder = prefs[Keys.PREFER_SOFTWARE_DECODER] ?: false,
            forceLandscape = prefs[Keys.FORCE_LANDSCAPE] ?: true,
            volumeGestures = prefs[Keys.VOLUME_GESTURES] ?: true,
            brightnessGestures = prefs[Keys.BRIGHTNESS_GESTURES] ?: true,
            resumePlaying = prefs[Keys.RESUME_PLAYING] ?: false,
            playlistCache = prefs[Keys.PLAYLIST_CACHE] ?: false,
            lastResizeMode = prefs[Keys.LAST_RESIZE_MODE] ?: 0
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

    suspend fun factoryReset() {
        context.dataStore.edit { it.clear() }
    }
}
