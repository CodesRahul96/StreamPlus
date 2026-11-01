package com.codesrahul.streamplus.ui.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.codesrahul.streamplus.data.settings.PlayerOrientation
import com.codesrahul.streamplus.BuildConfig
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.setContent
import kotlin.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.type
import com.codesrahul.streamplus.ui.util.tvFocusable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.DrmConfiguration
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.media3.exoplayer.drm.*
import androidx.media3.datasource.DataSchemeDataSource
import androidx.media3.datasource.DataSpec
import com.codesrahul.streamplus.ui.theme.StreamPlusTheme
import android.util.Log
import android.media.audiofx.DynamicsProcessing
import androidx.compose.material3.ExperimentalMaterial3Api
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import okhttp3.CookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl
import androidx.media3.extractor.ts.AdtsExtractor
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLContext
import java.security.SecureRandom
import java.security.cert.X509Certificate

import com.codesrahul.streamplus.domain.model.M3UItem
import java.util.UUID
import javax.inject.Inject

import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.DefaultMediaItemConverter
import com.google.android.gms.cast.framework.CastButtonFactory
import com.codesrahul.streamplus.data.drm.UnifiedMediaDrmCallback

@UnstableApi
@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    @Inject
    lateinit var okHttpClientProvider: javax.inject.Provider<OkHttpClient>

    @Inject
    lateinit var settingsRepository: com.codesrahul.streamplus.data.settings.SettingsRepository

    @Inject
    lateinit var historyRepository: com.codesrahul.streamplus.data.repository.HistoryRepository

    @Inject
    lateinit var channelDao: com.codesrahul.streamplus.data.local.ChannelDao

    @Inject
    lateinit var playlistDao: com.codesrahul.streamplus.data.local.PlaylistDao

    @Inject
    lateinit var playlistRepository: com.codesrahul.streamplus.domain.repository.PlaylistRepository

    @Inject
    lateinit var streamIntelligenceEngine: com.codesrahul.streamplus.data.network.StreamIntelligenceEngine

    @Inject
    lateinit var epgManager: com.codesrahul.streamplus.data.epg.EPGManager

    @Inject
    lateinit var streamRecorder: com.codesrahul.streamplus.ui.player.StreamRecorder

    private var castContext: CastContext? = null
    private var castPlayer: CastPlayer? = null
    private var sessionManager: SessionManager? = null
    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStarted(session: CastSession, sessionId: String) { invalidateOptionsMenu() }
        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionEnded(session: CastSession, error: Int) { invalidateOptionsMenu() }
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) { invalidateOptionsMenu() }
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    private val _isInPip = mutableStateOf(false)
    val isInPip: Boolean get() = _isInPip.value

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        _isInPip.value = isInPictureInPictureMode
    }

    private fun updatePipParams() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val builder = android.app.PictureInPictureParams.Builder()
            
            // Set dynamic aspect ratio if player is ready
            // We can't easily access the player here, but we can store the last known ratio
            lastVideoRatio?.let { builder.setAspectRatio(it) }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                lifecycleScope.launch {
                    val autoPip = settingsRepository.settings.first().autoPip
                    builder.setAutoEnterEnabled(autoPip)
                    setPictureInPictureParams(builder.build())
                }
            } else {
                setPictureInPictureParams(builder.build())
            }
        }
    }

    private var lastVideoRatio: android.util.Rational? = null

    fun setLastVideoRatio(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            val ratio = android.util.Rational(width, height)
            // PiP aspect ratio must be between 0.4184 and 2.39
            if (ratio.toFloat() in 0.4184f..2.39f) {
                lastVideoRatio = ratio
                updatePipParams()
            }
        }
    }

    companion object {
        const val EXTRA_STREAM_URL = "extra_stream_url"
        const val EXTRA_STREAM_TITLE = "extra_stream_title"
        const val EXTRA_CHANNEL_LOGO = "extra_channel_logo"
        const val EXTRA_USER_AGENT = "extra_user_agent"
        const val EXTRA_REFERER = "extra_referer"
        const val EXTRA_COOKIE = "extra_cookie"
        const val EXTRA_ORIGIN = "extra_origin"
        const val EXTRA_HEADERS = "extra_headers"
        const val EXTRA_DRM_LICENSE_URL = "extra_drm_license_url"
        const val EXTRA_DRM_SCHEME = "extra_drm_scheme"
        const val EXTRA_DRM_HEADERS = "extra_drm_headers"
        const val EXTRA_PLAYLIST_ID = "extra_playlist_id"
        const val EXTRA_STREAM_GROUP = "extra_stream_group"
        const val EXTRA_IS_VOD = "extra_is_vod"
        const val EXTRA_STREAM_TYPE = "extra_stream_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        com.codesrahul.streamplus.util.RefreshRateHelper.enableHighRefreshRate(this)
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_OPEN,
                com.codesrahul.streamplus.R.anim.player_enter,
                com.codesrahul.streamplus.R.anim.player_exit
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(
                com.codesrahul.streamplus.R.anim.player_enter,
                com.codesrahul.streamplus.R.anim.player_exit
            )
        }
        super.onCreate(savedInstanceState)
        
        try {
            castContext = CastContext.getSharedInstance(this)
            sessionManager = castContext?.sessionManager
            castContext?.let {
                castPlayer = CastPlayer(it, DefaultMediaItemConverter())
            }
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Cast initialization failed", e)
        }

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        var streamUrl = intent.getStringExtra(EXTRA_STREAM_URL)
            ?: intent.getStringExtra("STREAM_URL") ?: ""
        val streamTitle = intent.getStringExtra(EXTRA_STREAM_TITLE)
            ?: intent.getStringExtra("TITLE") ?: "Unknown Stream"
        val channelLogo = intent.getStringExtra(EXTRA_CHANNEL_LOGO)
            ?: intent.getStringExtra("LOGO_URL")
        var userAgent = intent.getStringExtra(EXTRA_USER_AGENT)
            ?: intent.getStringExtra("USER_AGENT") ?: "ExoPlayerLib/2.19.1"
        var referer = intent.getStringExtra(EXTRA_REFERER)
        var cookie = intent.getStringExtra(EXTRA_COOKIE)
        var origin = intent.getStringExtra(EXTRA_ORIGIN)
        val headers = extractMapExtra(intent, EXTRA_HEADERS).ifEmpty { extractMapExtra(intent, "HEADERS") }
        var drmLicenseUrl = intent.getStringExtra(EXTRA_DRM_LICENSE_URL)
            ?: intent.getStringExtra("LICENSE_URL")
        var drmScheme = intent.getStringExtra(EXTRA_DRM_SCHEME)
            ?: intent.getStringExtra("DRM_SCHEME")
        val drmHeaders = extractMapExtra(intent, EXTRA_DRM_HEADERS).ifEmpty { extractMapExtra(intent, "DRM_HEADERS") }
        val playlistId = intent.getIntExtra(EXTRA_PLAYLIST_ID, intent.getIntExtra("PLAYLIST_ID", -1))
        val groupTitle = intent.getStringExtra(EXTRA_STREAM_GROUP)
            ?: intent.getStringExtra("GROUP_TITLE") ?: "Default"
        var isVodExtra = intent.getBooleanExtra(EXTRA_IS_VOD, intent.getBooleanExtra("IS_VOD", false))
        var streamTypeExtra = intent.getStringExtra(EXTRA_STREAM_TYPE) ?: ""
        
        val isAudioExtra = intent.getBooleanExtra("EXTRA_IS_AUDIO", false)
        val mediaTypeExtra = intent.getStringExtra("EXTRA_MEDIA_TYPE")
        val playlistUris = intent.getStringArrayExtra("EXTRA_PLAYLIST_URIS")
        val playlistTitles = intent.getStringArrayExtra("EXTRA_PLAYLIST_TITLES")
        val playlistIndex = intent.getIntExtra("EXTRA_PLAYLIST_INDEX", -1)
        
        Log.d("PlayerActivity", ">>> Intent received: URL=$streamUrl, Title=$streamTitle, Cookie=$cookie, UA=$userAgent, DRM=$drmScheme, isVod=$isVodExtra, type=$streamTypeExtra")

        updatePipParams()

        lifecycleScope.launch {
            val resolvedHeaders = headers.toMutableMap()
            val resolvedDrmHeaders = drmHeaders.toMutableMap()

            val (freshChannel, initialSettings) = withContext(Dispatchers.IO) {
                val fresh = if (playlistId != -1 && streamTitle != "Unknown Stream") {
                    channelDao.getChannelByIdentity(streamTitle, groupTitle, playlistId)
                        ?: channelDao.getChannelByUrl(streamUrl)
                } else if (streamUrl.isNotBlank()) {
                    channelDao.getChannelByUrl(streamUrl)
                } else null

                val settings = settingsRepository.settings.first()
                Pair(fresh, settings)
            }

            if (freshChannel != null) {
                if (streamUrl.isBlank()) streamUrl = freshChannel.url
                if (userAgent.isBlank() || userAgent.startsWith("ExoPlayerLib/")) userAgent = freshChannel.userAgent
                referer = freshChannel.headers["Referer"] ?: freshChannel.headers["referer"] ?: referer
                cookie = freshChannel.headers["Cookie"] ?: freshChannel.headers["cookie"] ?: cookie
                origin = freshChannel.headers["Origin"] ?: freshChannel.headers["origin"] ?: origin
                drmLicenseUrl = freshChannel.licenseUrl ?: drmLicenseUrl
                drmScheme = freshChannel.drmScheme ?: drmScheme
                isVodExtra = freshChannel.isVod
                streamTypeExtra = freshChannel.streamType
                resolvedHeaders.putAll(freshChannel.headers)
                resolvedDrmHeaders.putAll(freshChannel.drmHeaders)
            }

            // Update App Shortcuts with the current channel info (only for valid IPTV channels, never local media)
            val isLocalMedia = playlistId <= 0 || streamUrl.startsWith("content://") ||
                    streamUrl.startsWith("file://") || streamUrl.startsWith("/storage/") || streamUrl.startsWith("/data/")
            if (!isLocalMedia && streamTitle != "Unknown Stream") {
                com.codesrahul.streamplus.util.ShortcutHelper.updateShortcuts(
                    context = this@PlayerActivity,
                    streamTitle = streamTitle,
                    streamUrl = streamUrl,
                    channelLogo = channelLogo,
                    playlistId = playlistId,
                    groupTitle = groupTitle
                )
            }

            setContent {
                val settings by settingsRepository.settings.collectAsState(initial = initialSettings)
                key(streamUrl, streamTitle) {
                    StreamPlusTheme(
                        themeMode = settings.themeMode,
                        accentColor = settings.accentColor
                    ) {
                        PlayerScreen(
                            streamUrl = streamUrl,
                            streamTitle = streamTitle,
                            channelLogo = channelLogo,
                            userAgent = userAgent,
                            referer = referer,
                            cookie = cookie,
                            origin = origin,
                            headers = resolvedHeaders,
                            drmLicenseUrl = drmLicenseUrl,
                            drmScheme = drmScheme,
                            drmHeaders = resolvedDrmHeaders,
                            okHttpClientProvider = okHttpClientProvider,
                            settingsRepository = settingsRepository,
                            historyRepository = historyRepository,
                            channelDao = channelDao,
                            playlistDao = playlistDao,
                            playlistRepository = playlistRepository,
                            streamIntelligenceEngine = streamIntelligenceEngine,
                            playlistId = playlistId,
                            groupTitle = groupTitle,
                            epgManager = epgManager,
                            streamRecorder = streamRecorder,
                            castPlayer = castPlayer,
                            onEnterPip = { enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build()) },
                            isInPip = isInPip,
                            isAudioExtra = isAudioExtra,
                            mediaTypeExtra = mediaTypeExtra,
                            playlistUris = playlistUris,
                            playlistTitles = playlistTitles,
                            playlistIndex = playlistIndex,
                            isVod = isVodExtra,
                            streamType = streamTypeExtra,
                            initialSettings = initialSettings
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // recreate() can cause flicker, but it's safe. Alternatively, we can use a state to refresh.
        recreate()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // On older Android versions, we still need manual trigger if auto-PIP is true
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            lifecycleScope.launch {
                val autoPip = settingsRepository.settings.first().autoPip
                if (autoPip) {
                    enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePipParams()
        sessionManager?.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
    }

    override fun onPause() {
        super.onPause()
        sessionManager?.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
    }

    override fun onDestroy() {
        if (::streamRecorder.isInitialized && streamRecorder.recordingState.value.isRecording) {
            streamRecorder.stopRecording(this)
        }
        castPlayer?.release()
        super.onDestroy()
    }

    override fun finish() {
        try {
            // Restore portrait orientation to match MainActivity and avoid surface tear
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (_: Exception) {}
        super.finish()
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_CLOSE,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
    }
}



private fun extractMapExtra(intent: android.content.Intent, key: String): Map<String, String> {
    val serializable: Any? = try { intent.getSerializableExtra(key) } catch (e: Exception) { null }
    if (serializable is Map<*, *>) {
        val result = mutableMapOf<String, String>()
        for ((k, v) in serializable) {
            if (k != null && v != null) {
                result[k.toString()] = v.toString()
            }
        }
        return result
    }
    val stringExtra = intent.getStringExtra(key)
    if (!stringExtra.isNullOrBlank() && stringExtra.trim().startsWith("{")) {
        try {
            val json = org.json.JSONObject(stringExtra.trim())
            val keysIter = json.keys()
            val map = mutableMapOf<String, String>()
            while (keysIter.hasNext()) {
                val k = keysIter.next()
                map[k] = json.optString(k, "")
            }
            return map
        } catch (e: Exception) {
            // Ignore
        }
    }
    return emptyMap()
}

private fun hexToByteArray(hex: String): ByteArray {
    val s = hex.replace(" ", "").replace(":", "")
    if (s.length % 2 != 0) return ByteArray(0)
    val len = s.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
        i += 2
    }
    return data
}

private fun generateClearKeyJson(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.startsWith("{") && trimmed.contains("\"keys\"")) {
        return trimmed
    }
    val cleanPairStr = trimmed.substringAfter("clearkey.direct/").removePrefix("http://").removePrefix("https://").trim('/')
    val pairs = cleanPairStr.split(",", ";").map { it.trim() }
    val keysJson = pairs.mapNotNull { pair ->
        val parts = pair.split(":")
        if (parts.size == 2) {
            val kid = parts[0].trim()
            val key = parts[1].trim()
            val kidBytes = if (kid.length == 32 && kid.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) hexToByteArray(kid) else try { Base64.decode(kid, Base64.DEFAULT) } catch (e: Exception) { null }
            val keyBytes = if (key.length == 32 && key.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) hexToByteArray(key) else try { Base64.decode(key, Base64.DEFAULT) } catch (e: Exception) { null }
            if (kidBytes != null && keyBytes != null && kidBytes.isNotEmpty() && keyBytes.isNotEmpty()) {
                val kidB64 = Base64.encodeToString(kidBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                val keyB64 = Base64.encodeToString(keyBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                "{\"kty\":\"oct\",\"k\":\"$keyB64\",\"kid\":\"$kidB64\"}"
            } else null
        } else null
    }.joinToString(",")

    return if (keysJson.isNotBlank()) "{\"keys\":[$keysJson],\"type\":\"temporary\"}" else null
}

enum class GestureTarget {
    BRIGHTNESS,
    VOLUME
}

@Composable
fun PlayerScreen(
    streamUrl: String,
    streamTitle: String,
    channelLogo: String?,
    userAgent: String,
    referer: String?,
    cookie: String?,
    origin: String?,
    headers: Map<String, String>,
    drmLicenseUrl: String?,
    drmScheme: String?,
    drmHeaders: Map<String, String>,
    okHttpClientProvider: javax.inject.Provider<okhttp3.OkHttpClient>,
    settingsRepository: com.codesrahul.streamplus.data.settings.SettingsRepository,
    historyRepository: com.codesrahul.streamplus.data.repository.HistoryRepository,
    channelDao: com.codesrahul.streamplus.data.local.ChannelDao,
    playlistDao: com.codesrahul.streamplus.data.local.PlaylistDao,
    playlistRepository: com.codesrahul.streamplus.domain.repository.PlaylistRepository,
    streamIntelligenceEngine: com.codesrahul.streamplus.data.network.StreamIntelligenceEngine,
    playlistId: Int,
    groupTitle: String,
    epgManager: com.codesrahul.streamplus.data.epg.EPGManager,
    streamRecorder: com.codesrahul.streamplus.ui.player.StreamRecorder,
    castPlayer: androidx.media3.cast.CastPlayer? = null,
    onEnterPip: () -> Unit,
    isInPip: Boolean = false,
    isAudioExtra: Boolean = false,
    mediaTypeExtra: String? = null,
    playlistUris: Array<String>? = null,
    playlistTitles: Array<String>? = null,
    playlistIndex: Int = -1,
    isVod: Boolean = false,
    streamType: String = "",
    initialSettings: com.codesrahul.streamplus.data.settings.AppSettings = com.codesrahul.streamplus.data.settings.AppSettings()
) {
    val context = LocalContext.current
    val activityContext = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper && ctx !is PlayerActivity) {
            ctx = ctx.baseContext
        }
        ctx as? PlayerActivity
    }
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val playerHolder = remember { PlayerHolder(context, okHttpClientProvider) }
    val settingsState by settingsRepository.settings.collectAsState(initial = initialSettings)
    
    // Dynamic Stream & DRM State for seamless in-player channel switching
    var currentStreamUrl by remember { mutableStateOf(streamUrl) }
    var currentStreamTitle by remember { mutableStateOf(streamTitle) }
    var currentChannelLogo by remember { mutableStateOf(channelLogo) }
    var currentUserAgent by remember { mutableStateOf(userAgent) }
    var currentReferer by remember { mutableStateOf(referer) }
    var currentCookie by remember { mutableStateOf(cookie) }
    var currentOrigin by remember { mutableStateOf(origin) }
    var currentHeaders by remember { mutableStateOf(headers) }
    var currentDrmLicenseUrl by remember { mutableStateOf(drmLicenseUrl) }
    var currentDrmScheme by remember { mutableStateOf(drmScheme) }
    var currentDrmHeaders by remember { mutableStateOf(drmHeaders) }
    var currentIsVod by remember { mutableStateOf(isVod) }
    var currentStreamType by remember { mutableStateOf(streamType) }
    var currentPlaylistUris by remember { mutableStateOf(playlistUris) }
    var lastPlaylistRefreshTime by remember { mutableLongStateOf(0L) }
    var consecutiveLiveErrors by remember { mutableIntStateOf(0) }
    var liveWatchTimeSeconds by remember(currentStreamUrl) { mutableLongStateOf(0L) }
    var isLiveStreamState by remember(currentStreamUrl) { mutableStateOf<Boolean?>(null) }
    // Increment to force resolvedItem to recompute even if URL string is the same
    var forceRefreshToken by remember { mutableIntStateOf(0) }

    val isLikelyVod = remember(currentStreamUrl, currentIsVod, currentStreamType, currentPlaylistUris, isLiveStreamState) {
        val isLocalUri = currentStreamUrl.startsWith("content://") ||
                currentStreamUrl.startsWith("file://") ||
                currentStreamUrl.startsWith("/storage/") ||
                currentStreamUrl.startsWith("/data/")

        val isLocalQueue = currentPlaylistUris?.firstOrNull()?.let {
            it.startsWith("content://") || it.startsWith("file://") || it.startsWith("/storage/") || it.startsWith("/data/")
        } ?: false

        if (isLocalUri || isLocalQueue) {
            return@remember true
        }

        // If ExoPlayer runtime timeline already determined whether this is a live stream or not
        if (isLiveStreamState != null) {
            return@remember !isLiveStreamState!!
        }

        val urlClean = currentStreamUrl.substringBefore("?")
        val lowerUrl = currentStreamUrl.lowercase()

        val isExplicitVod = currentIsVod ||
                currentStreamType.equals("vod", ignoreCase = true) ||
                currentStreamType.equals("series", ignoreCase = true) ||
                currentStreamType.equals("movie", ignoreCase = true) ||
                lowerUrl.contains("/movie/") ||
                lowerUrl.contains("/series/") ||
                listOf(".mp4", ".mkv", ".avi", ".mov", ".webm").any { urlClean.endsWith(it, ignoreCase = true) }

        if (isExplicitVod) {
            return@remember true
        }

        // Channels from IPTV playlists default to live unless explicitly marked VOD
        val isExplicitLive = currentStreamType.equals("live", ignoreCase = true) ||
                playlistId > 0 || // Any stream originating from an IPTV playlist is a live channel by default!
                lowerUrl.contains("/live/") ||
                lowerUrl.contains("play/live.php") ||
                lowerUrl.contains(".m3u8") ||
                lowerUrl.contains(".ts") ||
                lowerUrl.contains(".mpd") ||
                lowerUrl.contains("wdvlive") ||
                lowerUrl.contains("wvlive") ||
                lowerUrl.contains("/bpk-tv/") ||
                lowerUrl.contains("jiotv") ||
                lowerUrl.contains("jiomblive") ||
                lowerUrl.contains("jiotvmb")

        !isExplicitLive
    }

    // Stream & Codec Quality State
    var videoResolution by remember { mutableStateOf("") }
    var videoCodec by remember { mutableStateOf("") }
    var videoFps by remember { mutableStateOf("") }
    var audioChannels by remember { mutableStateOf("") }
    var activeDecoderMode by remember { mutableIntStateOf(initialSettings.decoderPriority) }

    // Quick Channel Drawer State
    var showChannelDrawer by remember { mutableStateOf(false) }
    var drawerChannels by remember { mutableStateOf<List<com.codesrahul.streamplus.data.local.ChannelEntity>>(emptyList()) }
    var drawerSearchQuery by remember { mutableStateOf("") }

    val isLocalOrDirectPlay = playlistId <= 0 || currentStreamUrl.startsWith("content://") || 
            currentStreamUrl.startsWith("file://") || currentStreamUrl.startsWith("/storage/") || currentStreamUrl.startsWith("/data/")
    val isLocalQueue = currentPlaylistUris != null && currentPlaylistUris!!.isNotEmpty()
    val canShowDrawer = if (isLocalOrDirectPlay) isLocalQueue else drawerChannels.isNotEmpty()

    androidx.activity.compose.BackHandler {
        activityContext?.finish()
    }

    LaunchedEffect(playlistId, currentStreamUrl) {
        withContext(Dispatchers.IO) {
            // Local gallery media or direct play without a playlist must NOT load all IPTV channels from the database
            val list = if (playlistId > 0 && !isLocalOrDirectPlay) {
                channelDao.getChannelsByPlaylist(playlistId)
            } else if (!isLocalOrDirectPlay) {
                val found = channelDao.getChannelByUrl(currentStreamUrl)
                if (found != null && found.playlistId > 0) {
                    channelDao.getChannelsByPlaylist(found.playlistId)
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
            drawerChannels = list
        }
    }
    
    var audioBoost by remember { mutableStateOf(0f) }
    var volumeAccumulator by remember { mutableStateOf(0f) }
    

    
    // Playback Speed State
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    
    // Sleep Timer State
    var sleepTimerMillis by remember { mutableLongStateOf(0L) }
    var sleepTimerActive by remember { mutableStateOf(false) }

    // Subtitle Picker state
    var subtitlePickerLauncher: (() -> Unit)? by remember { mutableStateOf(null) }

    // Bottom Sheet State
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    // Auto-Retry State
    var retryCount by remember { mutableIntStateOf(0) }
    val maxRetries = 5

    val switchChannel: (com.codesrahul.streamplus.data.local.ChannelEntity) -> Unit = { ch ->
        currentPlaylistUris = null
        currentStreamUrl = ch.url
        currentStreamTitle = ch.name
        currentChannelLogo = ch.logoUrl
        currentUserAgent = ch.userAgent.takeIf { it.isNotBlank() } ?: userAgent
        currentHeaders = if (ch.headers.isNotEmpty()) ch.headers else headers
        currentDrmLicenseUrl = ch.licenseUrl
        currentDrmScheme = ch.drmScheme
        currentDrmHeaders = ch.drmHeaders
        currentIsVod = ch.isVod
        currentStreamType = ch.streamType
        consecutiveLiveErrors = 0
        retryCount = 0
        showChannelDrawer = false
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }

    // Resume Logic
    // Using a separate LaunchedEffect for resume so it doesn't re-trigger on other changes
    // We need to wait for player to be created, so we'll do this inside the player creation or a separate effect dependent on player
    
    // Gesture State
    val initialBrightness = remember {
        val lp = activityContext?.window?.attributes?.screenBrightness
        if (lp != null && lp >= 0f) lp else 0.5f
    }
    val initialVolume = remember {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
        val max = am?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 15
        val curr = am?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 7
        if (max > 0) curr.toFloat() / max else 0.5f
    }
    var brightnessLevel by remember { mutableFloatStateOf(initialBrightness) }
    var volumeLevel by remember { mutableFloatStateOf(initialVolume) }
    var activeGestureTarget by remember { mutableStateOf(GestureTarget.BRIGHTNESS) }

    var showGestureOverlay by remember { mutableStateOf(false) }
    var isGestureScrolling by remember { mutableStateOf(false) }
    var gestureLevel by remember { mutableFloatStateOf(0f) }
    var gestureTrigger by remember { mutableLongStateOf(0L) }
    var isTouching by remember { mutableStateOf(false) }
    
    // Seek Overlay State
    var showSeekOverlay by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }
    
    // Lock State
    var isLocked by remember { mutableStateOf(false) }
    var isControlsVisible by remember { mutableStateOf(false) }
    
    // Aspect Ratio State Tracking
    var sessionResizeMode by remember { mutableStateOf<Int?>(null) }
    var currentVideoWidth by remember { mutableStateOf(0) }
    var currentVideoHeight by remember { mutableStateOf(0) }
    
    // Reset session override if default setting changes or finally loads from DataStore
    LaunchedEffect(settingsState.defaultAspectRatio) {
        sessionResizeMode = null
    }
    
    // Debounced Auto-Hide for Gesture Overlay
    LaunchedEffect(gestureTrigger, isTouching) {
        if (showGestureOverlay && !isTouching && gestureTrigger > 0L) {
            kotlinx.coroutines.delay(1000)
            showGestureOverlay = false
        }
    }
    
    LaunchedEffect(showSeekOverlay) {
        if (showSeekOverlay) {
            kotlinx.coroutines.delay(800)
            showSeekOverlay = false
        }
    }

    // Auto-hide controls when locked
    LaunchedEffect(isControlsVisible, isLocked) {
        if (isControlsVisible && isLocked) {
            kotlinx.coroutines.delay(4000)
            isControlsVisible = false
        }
    }

    // Handle Orientation and Immersive Mode
    val activity = context as? android.app.Activity
    
    // Set initial immersive state
    LaunchedEffect(Unit) {
        activity?.let { hideSystemUI(it) }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Sync immersive mode with controls visibility
    LaunchedEffect(isControlsVisible, isLandscape) {
        if (!isControlsVisible || isLandscape) {
            activity?.let { hideSystemUI(it) }
        } else {
            activity?.let { showSystemUI(it) }
        }
    }

    // Combined Time State
    var currentTimeFormatted by remember { mutableStateOf("00:00") }
    var durationFormatted by remember { mutableStateOf("00:00") }
    var playerViewRef by remember { mutableStateOf<androidx.media3.ui.PlayerView?>(null) }

    LaunchedEffect(settingsState.playerOrientation) {
        val activity = context as? android.app.Activity
        activity?.requestedOrientation = when (settingsState.playerOrientation) {
            PlayerOrientation.LANDSCAPE -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            PlayerOrientation.PORTRAIT -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            PlayerOrientation.AUTO -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        if (settingsState.playerOrientation == PlayerOrientation.LANDSCAPE) {
            activity?.let { hideSystemUI(it) }
        }
    }

    // 1. Resolve Item immediately based on Inputs
    val resolvedItem = remember(
        currentStreamUrl, currentStreamTitle, currentChannelLogo, currentUserAgent,
        currentReferer, currentCookie, currentOrigin, currentDrmLicenseUrl,
        currentDrmScheme, currentHeaders, currentDrmHeaders
    ) {
        val isMagUrl = currentStreamUrl.contains("mac=", ignoreCase = true) || 
                       currentStreamUrl.contains("play/live.php", ignoreCase = true) ||
                       currentStreamUrl.contains("/stalker", ignoreCase = true)
        
        val effectiveUA = if (isMagUrl) "MacroHTTP" else currentUserAgent
        
        // Build the starting headers map from all individual intent extras
        val startHeaders = mutableMapOf<String, String>()
        startHeaders.putAll(currentHeaders)
        val ref = currentReferer
        val cook = currentCookie
        val orig = currentOrigin
        if (!ref.isNullOrBlank())  startHeaders["Referer"]    = ref
        if (!cook.isNullOrBlank())   startHeaders["Cookie"]     = cook
        if (!orig.isNullOrBlank())   startHeaders["Origin"]     = orig
        if (isMagUrl) {
            startHeaders["X-User-Agent"] = "Model: MAG250; Link: Ethernet"
        }

        val item = M3UItem(
            name = currentStreamTitle,
            url = currentStreamUrl,
            logoUrl = currentChannelLogo ?: "",
            group = "",
            userAgent = effectiveUA ?: "",
            headers = startHeaders,
            licenseUrl = currentDrmLicenseUrl,
            drmScheme = currentDrmScheme,
            drmHeaders = currentDrmHeaders
        )
        // Use new resolvers
        val headerResolver = com.codesrahul.streamplus.data.parser.resolver.HeaderResolver()
        val drmResolver = com.codesrahul.streamplus.data.parser.resolver.DrmResolver()
        
        val resolvedHeaders = headerResolver.resolve(item)
        val finalResolved = drmResolver.resolve(resolvedHeaders)
        
        finalResolved.also {
            Log.d("PlayerScreen", "Resolved Stream: URL=${it.url}, UA=${it.userAgent}, Headers=${it.headers.keys}, License=${it.licenseUrl}")
        }
    }

    val exoPlayer = remember(resolvedItem, settingsState.skipSslVerification, activeDecoderMode, settingsState.enableTunneling) {
        val okHttpClient = okHttpClientProvider.get()

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient).apply {
            val finalHeaders = mutableMapOf<String, String>()

            // Use headers from resolved item
            finalHeaders.putAll(resolvedItem.headers) 
            if (resolvedItem.userAgent.isNotBlank()) {
                finalHeaders["User-Agent"] = resolvedItem.userAgent
                Log.d("PlayerScreen", "Setting User-Agent: ${resolvedItem.userAgent}")
            }
            // Inject Referer, Cookie, Origin from intent extras into the actual HTTP headers
            val ref = currentReferer
            val cook = currentCookie
            val orig = currentOrigin
            if (!ref.isNullOrBlank()) {
                finalHeaders["Referer"] = ref
                Log.d("PlayerScreen", "Setting Referer: $ref")
            }
            if (!cook.isNullOrBlank()) {
                finalHeaders["Cookie"] = cook
                Log.d("PlayerScreen", "Setting Cookie header")
            }
            if (!orig.isNullOrBlank()) {
                finalHeaders["Origin"] = orig
                Log.d("PlayerScreen", "Setting Origin: $orig")
            }
            
            Log.d("PlayerScreen", "Stream URL: ${resolvedItem.url}")
            
            setDefaultRequestProperties(finalHeaders)
        }
        
        val baseDataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)
        val dataSourceFactory = if (isLikelyVod) {
            com.codesrahul.streamplus.data.cache.CacheManager.createCacheDataSourceFactory(context, baseDataSourceFactory)
        } else {
            baseDataSourceFactory
        }

        val drmSessionManagerProvider = object : DrmSessionManagerProvider {
            override fun get(mediaItem: MediaItem): DrmSessionManager {
                val dConf = mediaItem.localConfiguration?.drmConfiguration ?: return DrmSessionManager.DRM_UNSUPPORTED
                
                // Use headers from DRM configuration (which includes X-ClearKey-Data for embedded keys)
                val allHeaders = mutableMapOf<String, String>()
                allHeaders.putAll(resolvedItem.headers)
                allHeaders.putAll(resolvedItem.drmHeaders)
                if (!referer.isNullOrBlank()) {
                    allHeaders["Referer"] = referer
                }
                if (!cookie.isNullOrBlank()) {
                    allHeaders["Cookie"] = cookie
                }
                if (!origin.isNullOrBlank()) {
                    allHeaders["Origin"] = origin
                }
                if (resolvedItem.userAgent.isNotBlank()) {
                    allHeaders["User-Agent"] = resolvedItem.userAgent
                } else if (!userAgent.isNullOrBlank()) {
                    allHeaders["User-Agent"] = userAgent
                }
                // Add license request headers from DRM configuration (includes X-ClearKey-Data)
                allHeaders.putAll(dConf.licenseRequestHeaders)
                
                Log.d("PlayerScreen", "DRM Session Manager - Headers: ${allHeaders.keys}")
                
                val callback = UnifiedMediaDrmCallback(dConf.licenseUri?.toString(), okHttpDataSourceFactory, okHttpClient, allHeaders)
                
                val drmBuilder = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(dConf.uuid, FrameworkMediaDrm.DEFAULT_PROVIDER)
                    .setMultiSession(dConf.multiSession)
                    .setPlayClearSamplesWithoutKeys(true)
                
                if (dConf.uuid == C.WIDEVINE_UUID && settingsState.forceWidevineL3) {
                    drmBuilder.setKeyRequestParameters(mapOf("securityLevel" to "L3"))
                }
                
                return drmBuilder.build(callback)
            }
        }

        val mediaSourceFactory = com.codesrahul.streamplus.data.player.MediaCodecConfig.createMediaSourceFactory(
            context = context,
            dataSourceFactory = dataSourceFactory,
            drmSessionManagerProvider = drmSessionManagerProvider
        )

        val renderersFactory = com.codesrahul.streamplus.data.player.MediaCodecConfig.createRenderersFactory(
            context = context,
            decoderPriority = activeDecoderMode,
            enableTunneling = settingsState.enableTunneling
        )

        val bufferMs = settingsState.networkBufferSize * 1000
        val playbackStartMs = if (bufferMs < 1000) 500 else 1000
        val rebufferMs = if (bufferMs < 2000) 1000 else 2000
        val loadControl = if (isLikelyVod) {
            androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    bufferMs, // Min buffer
                    (bufferMs * 2.5).toInt(), // Max buffer
                    playbackStartMs, // Buffer for playback start
                    rebufferMs  // Buffer for rebuffer
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        } else {
            androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    1_500,  // 1.5s min buffer for live streams (prevents stalling on small live sliding windows)
                    20_000, // 20s max buffer
                    500,    // 500ms for instant start
                    1_000   // 1s for fast rebuffer
                )
                .setBackBuffer(0, false)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        }

        val audioLanguagesMap = mapOf(
            "English" to "en",
            "Hindi" to "hi",
            "Bengali" to "bn",
            "Telugu" to "te",
            "Marathi" to "mr",
            "Tamil" to "ta",
            "Urdu" to "ur",
            "Gujarati" to "gu",
            "Kannada" to "kn",
            "Odia" to "or",
            "Malayalam" to "ml",
            "Punjabi" to "pa",
            "Sanskrit" to "sa",
            "Assamese" to "as",
            "Maithili" to "mai",
            "Santali" to "sat",
            "Kashmiri" to "ks",
            "Nepali" to "ne",
            "Konkani" to "kok",
            "Sindhi" to "sd",
            "Dogri" to "doi",
            "Manipuri" to "mni",
            "Bodo" to "brx",
            "Spanish" to "es",
            "French" to "fr",
            "German" to "de",
            "Russian" to "ru",
            "Arabic" to "ar",
            "Chinese" to "zh",
            "Japanese" to "ja",
            "Portuguese" to "pt"
        )
        val preferredLang = audioLanguagesMap[settingsState.defaultAudioLanguage] ?: settingsState.defaultAudioLanguage.takeIf { it != "Auto" }?.lowercase()

        val player = playerHolder.getOrCreate(
            mediaSourceFactory = mediaSourceFactory,
            loadControl = loadControl,
            renderersFactory = renderersFactory,
            onReleasePlayer = { oldPlayer ->
                val pos = try { oldPlayer.currentPosition } catch (e: Exception) { 0L }
                val dur = try { oldPlayer.duration } catch (e: Exception) { 0L }
                if (pos > 5000 && resolvedItem.url.isNotBlank()) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            historyRepository.saveProgress(
                                url = resolvedItem.url,
                                name = resolvedItem.name,
                                logo = resolvedItem.logoUrl,
                                group = groupTitle,
                                playlistId = playlistId,
                                userAgent = resolvedItem.userAgent,
                                headers = resolvedItem.headers,
                                cookie = cookie,
                                referer = referer,
                                origin = origin,
                                licenseUrl = resolvedItem.licenseUrl,
                                drmScheme = resolvedItem.drmScheme,
                                drmHeaders = resolvedItem.drmHeaders,
                                position = pos,
                                duration = if (dur == androidx.media3.common.C.TIME_UNSET || dur < 0) 0L else dur
                            )
                        } catch (e: Exception) {
                            Log.e("PlayerScreen", "Failed to save final progress in holder", e)
                        }
                    }
                }
            }
        )
            
        player.setTrackSelectionParameters(
            player.trackSelectionParameters.buildUpon()
                .setPreferredAudioLanguage(preferredLang)
                .build()
        )
            
        player.setPlayWhenReady(true)
        player.setSkipSilenceEnabled(settingsState.skipSilence)
        player.setRepeatMode(if (settingsState.repeatMode) androidx.media3.common.Player.REPEAT_MODE_ALL else androidx.media3.common.Player.REPEAT_MODE_OFF)
        
        var liveBufferingWatchdogJob: kotlinx.coroutines.Job? = null

        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("PlayerScreen", "Playback Error: ${error.message} [Code: ${error.errorCode}]", error)
                
                val isBehindLiveWindow = error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ||
                                         error.cause?.javaClass?.name?.contains("BehindLiveWindowException") == true ||
                                         error.message?.contains("BehindLiveWindowException", ignoreCase = true) == true

                if (isBehindLiveWindow) {
                    Log.w("PlayerScreen", "Behind live window detected: Seamlessly jumping to live edge...")
                    val currentItem = player.currentMediaItem
                    if (currentItem != null) {
                        player.setMediaItem(currentItem.buildUpon().build())
                    } else {
                        player.seekToDefaultPosition()
                    }
                    player.prepare()
                    player.play()
                    return
                }

                val httpException = error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
                val httpCode = httpException?.responseCode ?: 0
                val isAuthError = httpCode == 401 || httpCode == 403
                val isSegmentExpired = !isLikelyVod && (httpCode == 404 || httpCode == 410)
                val isNetworkError = error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                                     error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                                     error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED
                val isHttpError = error.cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException ||
                                  error.cause is androidx.media3.datasource.HttpDataSource.HttpDataSourceException

                // --- AUTH ERRORS (401/403): try playlist refresh, then reconnect ---
                if (isAuthError && !isLikelyVod) {
                    val now = System.currentTimeMillis()
                    if (playlistId != -1 && (now - lastPlaylistRefreshTime > 15_000L)) {
                        lastPlaylistRefreshTime = now
                        Log.d("PlayerScreen", "HTTP $httpCode on live stream — refreshing playlist for fresh tokens...")
                        scope.launch {
                            try {
                                val playlistEntity = withContext(Dispatchers.IO) { playlistDao.getPlaylistSync(playlistId) }
                                if (playlistEntity != null) {
                                    withContext(Dispatchers.IO) {
                                        playlistRepository.loadPlaylist(
                                            url = playlistEntity.url, userAgent = playlistEntity.userAgent,
                                            forceRefresh = true, playlistId = playlistId
                                        )
                                    }
                                    val fresh = withContext(Dispatchers.IO) {
                                        channelDao.getChannelByIdentity(currentStreamTitle, groupTitle, playlistId)
                                    }
                                    if (fresh != null) {
                                        Log.d("PlayerScreen", "Playlist refreshed. URL: ${fresh.url}")
                                        if (fresh.url != currentStreamUrl) {
                                            currentStreamUrl = fresh.url
                                        } else {
                                            // Same URL — bump token to force LaunchedEffect to re-run
                                            forceRefreshToken++
                                        }
                                        return@launch
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("PlayerScreen", "Playlist refresh failed: ${e.message}")
                            }
                            // Fallback reconnect
                            kotlinx.coroutines.delay(2000)
                            val ci = player.currentMediaItem
                            if (ci != null) player.setMediaItem(ci.buildUpon().build())
                            player.prepare(); player.play()
                        }
                        return
                    }
                    // Within cooldown — use exponential backoff reconnect
                    consecutiveLiveErrors++
                    val delayMs = (2000L * consecutiveLiveErrors).coerceIn(2000L, 12000L)
                    Log.w("PlayerScreen", "Auth error in cooldown. Reconnecting in ${delayMs}ms (attempt $consecutiveLiveErrors)...")
                    scope.launch {
                        kotlinx.coroutines.delay(delayMs)
                        val ci = player.currentMediaItem
                        if (ci != null) player.setMediaItem(ci.buildUpon().build())
                        player.prepare(); player.play()
                    }
                    return
                }

                // --- 404/410 SEGMENT EXPIRED: exponential backoff reconnect ---
                if (isSegmentExpired) {
                    consecutiveLiveErrors++
                    val delayMs = (1500L * consecutiveLiveErrors).coerceIn(1000L, 8000L)
                    Log.w("PlayerScreen", "Live segment expired (HTTP $httpCode). Reconnecting in ${delayMs}ms (attempt $consecutiveLiveErrors)...")
                    scope.launch {
                        kotlinx.coroutines.delay(delayMs)
                        val ci = player.currentMediaItem
                        if (ci != null) player.setMediaItem(ci.buildUpon().build())
                        else player.seekToDefaultPosition()
                        player.prepare(); player.play()
                    }
                    return
                }

                // Smart Stream Self-Healing Analysis
                val healing = streamIntelligenceEngine.evaluatePlaybackError(error, currentStreamUrl, currentHeaders)
                when (healing) {
                    is com.codesrahul.streamplus.data.network.HealingAction.UpdateHeaders -> {
                        android.widget.Toast.makeText(context, "Smart Recovery: ${healing.reason}", android.widget.Toast.LENGTH_SHORT).show()
                        currentHeaders = healing.newHeaders
                        if (!isLikelyVod) player.seekToDefaultPosition()
                        player.prepare()
                        player.play()
                        return
                    }
                    is com.codesrahul.streamplus.data.network.HealingAction.FallbackDecoder -> {
                        if (activeDecoderMode != 1) {
                            android.widget.Toast.makeText(context, "Smart Recovery: ${healing.reason}", android.widget.Toast.LENGTH_SHORT).show()
                            activeDecoderMode = 1
                            return
                        }
                    }
                    is com.codesrahul.streamplus.data.network.HealingAction.RestartLiveWindow -> {
                        android.widget.Toast.makeText(context, "Auto-Sync: Aligning live stream edge", android.widget.Toast.LENGTH_SHORT).show()
                        player.seekToDefaultPosition()
                        player.prepare()
                        player.play()
                        return
                    }
                    else -> {}
                }

                // For Live Streams: NEVER permanently stop! Keep reconnecting with exponential backoff!
                if (!isLikelyVod) {
                    consecutiveLiveErrors++
                    val delayMs = (1500L * consecutiveLiveErrors).coerceIn(1000L, 8000L)
                    Log.d("PlayerScreen", "Live stream hiccup. Auto-reconnecting in ${delayMs}ms (attempt $consecutiveLiveErrors)...")
                    scope.launch {
                        kotlinx.coroutines.delay(delayMs)
                        val currentItem = player.currentMediaItem
                        if (currentItem != null) {
                            player.setMediaItem(currentItem.buildUpon().build())
                        } else {
                            player.seekToDefaultPosition()
                        }
                        player.prepare()
                        player.play()
                    }
                    return
                }

                // For VOD Streams: standard finite retry
                if ((isNetworkError || isHttpError) && retryCount < maxRetries) {
                    retryCount++
                    val delayMs = (2000L * retryCount).coerceAtMost(10000L)
                    Log.d("PlayerScreen", "Network error detected. Retrying ($retryCount/$maxRetries) in ${delayMs}ms...")
                    
                    scope.launch {
                        kotlinx.coroutines.delay(delayMs)
                        player.prepare()
                        player.play()
                    }
                    return
                }

                if (settingsState.silentSkipping) {
                    android.widget.Toast.makeText(context, "Playback error: ${error.errorCodeName}. Skipping...", android.widget.Toast.LENGTH_SHORT).show()
                    return
                }

                val msg = if (BuildConfig.DEBUG) {
                    when (val cause = error.cause) {
                        is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException -> "Error: ${cause.responseCode} ${cause.message}"
                        is androidx.media3.datasource.HttpDataSource.HttpDataSourceException -> "Network Error: ${cause.message}"
                        else -> "Playback Error: ${error.errorCodeName}"
                    }
                } else {
                    "Playback error. Retrying..."
                }
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                if (!timeline.isEmpty) {
                    val isLive = player.isCurrentMediaItemLive || player.isCurrentMediaItemDynamic
                    if (isLiveStreamState != isLive) {
                        isLiveStreamState = isLive
                    }
                }
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                val isLive = player.isCurrentMediaItemLive || player.isCurrentMediaItemDynamic
                if (isLiveStreamState != isLive && !player.currentTimeline.isEmpty) {
                    isLiveStreamState = isLive
                }
                for (trackGroup in tracks.groups) {
                    if (trackGroup.type == C.TRACK_TYPE_VIDEO && trackGroup.isSelected) {
                        for (i in 0 until trackGroup.length) {
                            if (trackGroup.isTrackSelected(i)) {
                                val format = trackGroup.getTrackFormat(i)
                                val sampleMime = format.sampleMimeType ?: ""
                                videoCodec = when {
                                    sampleMime.contains("hevc") || sampleMime.contains("h265") -> "HEVC"
                                    sampleMime.contains("avc") || sampleMime.contains("h264") -> "H.264"
                                    sampleMime.contains("av01") || sampleMime.contains("av1") -> "AV1"
                                    sampleMime.contains("vp9") -> "VP9"
                                    sampleMime.contains("vp8") -> "VP8"
                                    else -> sampleMime.substringAfterLast("/").uppercase()
                                }
                                if (format.frameRate > 0) {
                                    videoFps = "${format.frameRate.toInt()}fps"
                                }
                                if (format.width > 0 && format.height > 0) {
                                    videoResolution = when {
                                        format.width >= 3840 || format.height >= 2160 -> "4K"
                                        format.width >= 1920 || format.height >= 1080 -> "1080p"
                                        format.width >= 1280 || format.height >= 720 -> "720p"
                                        else -> "${format.height}p"
                                    }
                                }
                            }
                        }
                    }
                    if (trackGroup.type == C.TRACK_TYPE_AUDIO && trackGroup.isSelected) {
                        for (i in 0 until trackGroup.length) {
                            if (trackGroup.isTrackSelected(i)) {
                                val format = trackGroup.getTrackFormat(i)
                                audioChannels = when (format.channelCount) {
                                    6 -> "5.1"
                                    8 -> "7.1"
                                    2 -> "Stereo"
                                    else -> ""
                                }
                            }
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    androidx.media3.common.Player.STATE_READY -> {
                        liveBufferingWatchdogJob?.cancel()
                        liveBufferingWatchdogJob = null
                        retryCount = 0
                        consecutiveLiveErrors = 0
                    }
                    androidx.media3.common.Player.STATE_BUFFERING -> {
                        if (!isLikelyVod) {
                            liveBufferingWatchdogJob?.cancel()
                            liveBufferingWatchdogJob = scope.launch {
                                kotlinx.coroutines.delay(3500L)
                                Log.w("PlayerScreen", "Live stream buffering stalled for >3.5s. Resyncing to live edge...")
                                player.seekToDefaultPosition()
                                player.prepare()
                                player.play()
                            }
                        }
                    }
                    androidx.media3.common.Player.STATE_ENDED -> {
                        liveBufferingWatchdogJob?.cancel()
                        liveBufferingWatchdogJob = null
                        if (!isLikelyVod) {
                            Log.d("PlayerScreen", "Live stream reached unexpected end. Auto-reconnecting to live edge...")
                            scope.launch {
                                kotlinx.coroutines.delay(1000L)
                                val currentItem = player.currentMediaItem
                                if (currentItem != null) {
                                    player.setMediaItem(currentItem.buildUpon().build())
                                } else {
                                    player.seekToDefaultPosition()
                                }
                                player.prepare()
                                player.play()
                            }
                        }
                    }
                    androidx.media3.common.Player.STATE_IDLE -> {
                        liveBufferingWatchdogJob?.cancel()
                        liveBufferingWatchdogJob = null
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    Log.d("PlayerScreen", "Playback successful. Resetting retry count.")
                    retryCount = 0
                    consecutiveLiveErrors = 0
                }
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                currentVideoWidth = videoSize.width
                currentVideoHeight = videoSize.height
                activityContext?.setLastVideoRatio(videoSize.width, videoSize.height)
                
                // Auto-detect orientation if set to AUTO
                if (settingsState.playerOrientation == PlayerOrientation.AUTO) {
                    val isPortraitVideo = videoSize.height > videoSize.width && (videoSize.width > 0)
                    val isLandscapeVideo = videoSize.width > videoSize.height && (videoSize.height > 0)
                    
                    activityContext?.let { activity ->
                        val target = when {
                            isPortraitVideo -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                            isLandscapeVideo -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            else -> null
                        }
                        
                        if (target != null && activity.requestedOrientation != target) {
                            Log.d("PlayerActivity", "Auto-detecting orientation: target=$target (w=${videoSize.width}, h=${videoSize.height})")
                            activity.requestedOrientation = target
                        }
                    }
                }
            }
        })
        
        player
    }

    // Unified Timer & Ticker for Time, Progress, and Live Status
    LaunchedEffect(exoPlayer, currentStreamUrl, isLikelyVod) {
        liveWatchTimeSeconds = 0L
        while (true) {
            if (!isLikelyVod) {
                if (exoPlayer.isPlaying) {
                    liveWatchTimeSeconds++
                }
                val liveTimeStr = formatElapsedTime(liveWatchTimeSeconds * 1000L)
                playerViewRef?.let { pv ->
                    pv.findViewById<android.widget.TextView>(com.codesrahul.streamplus.R.id.tv_live_badge)?.visibility = android.view.View.VISIBLE
                    pv.findViewById<android.widget.TextView>(com.codesrahul.streamplus.R.id.tv_time_combined)?.text = "🔴 LIVE • $liveTimeStr"
                    pv.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)?.visibility = android.view.View.GONE
                    pv.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_rew)?.visibility = android.view.View.GONE
                    pv.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_ffwd)?.visibility = android.view.View.GONE
                }
            } else {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                currentTimeFormatted = formatElapsedTime(pos)
                durationFormatted = if (dur == C.TIME_UNSET || dur <= 0) "00:00" else formatElapsedTime(dur)
                playerViewRef?.let { pv ->
                    pv.findViewById<android.widget.TextView>(com.codesrahul.streamplus.R.id.tv_live_badge)?.visibility = android.view.View.GONE
                    pv.findViewById<android.widget.TextView>(com.codesrahul.streamplus.R.id.tv_time_combined)?.text = "$currentTimeFormatted / $durationFormatted"
                    pv.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)?.visibility = android.view.View.VISIBLE
                    pv.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_rew)?.visibility = android.view.View.VISIBLE
                    pv.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_ffwd)?.visibility = android.view.View.VISIBLE
                }
            }
            kotlinx.coroutines.delay(1000L)
        }
    }

    // --- Subtitle Picker (defined here to have access to exoPlayer) ---
    val subtitlePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val subtitle = MediaItem.SubtitleConfiguration.Builder(it)
                .setMimeType(if (it.toString().endsWith(".vtt")) androidx.media3.common.MimeTypes.TEXT_VTT else androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
                .setLanguage("en")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            
            val currentMediaItem = exoPlayer.currentMediaItem
            if (currentMediaItem != null) {
                val newMediaItem = currentMediaItem.buildUpon()
                    .setSubtitleConfigurations(listOf(subtitle))
                    .build()
                val currentPos = exoPlayer.currentPosition
                exoPlayer.setMediaItem(newMediaItem, currentPos)
                exoPlayer.prepare()
                exoPlayer.play()
                android.widget.Toast.makeText(context, "Subtitle loaded", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Sync launcher
    LaunchedEffect(Unit) {
        subtitlePickerLauncher = { subtitlePicker.launch("*/*") }
    }

    // --- Active Player Logic (Exo vs Cast) ---
    var isCasting by remember { mutableStateOf(false) }
    val activePlayer = if (isCasting && castPlayer != null) castPlayer else exoPlayer
    
    // Listen for Cast session availability
    DisposableEffect(castPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: androidx.media3.common.Player.PositionInfo,
                newPosition: androidx.media3.common.Player.PositionInfo,
                reason: Int
            ) {
                 // Check if we switched to/from Cast
                 isCasting = castPlayer?.isCastSessionAvailable == true
            }

            override fun onEvents(player: androidx.media3.common.Player, events: androidx.media3.common.Player.Events) {
                if (events.contains(androidx.media3.common.Player.EVENT_MEDIA_ITEM_TRANSITION) || 
                    events.contains(androidx.media3.common.Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
                    isCasting = castPlayer?.isCastSessionAvailable == true
                }
            }
        }
        
        // Initial check
        isCasting = castPlayer?.isCastSessionAvailable == true
        
        castPlayer?.addListener(listener)
        onDispose { castPlayer?.removeListener(listener) }
    }

    // Sync media item to Cast if activePlayer becomes castPlayer
    LaunchedEffect(isCasting) {
        if (isCasting && castPlayer != null) {
            val mediaItem = exoPlayer.currentMediaItem
            if (mediaItem != null) {
                val currentPos = exoPlayer.currentPosition
                // Only sync if cast player doesn't already have it
                if (castPlayer.currentMediaItem?.localConfiguration?.uri != mediaItem.localConfiguration?.uri) {
                    castPlayer.setMediaItem(mediaItem, currentPos)
                    castPlayer.prepare()
                    castPlayer.play()
                    if (exoPlayer.isPlaying) exoPlayer.pause()
                    android.widget.Toast.makeText(context, "Casting to TV...", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } else if (!isCasting) {
            // If we stopped casting, make sure ExoPlayer is ready to resume (UI will switch back via activePlayer)
            // Note: We don't auto-resume ExoPlayer to avoid surprising the user with loud audio
        }
    }

    // Audio Boost Effect Application
    LaunchedEffect(audioBoost) {
        if (audioBoost > 0f) {
            val gainDb = audioBoost * 12f // Max 12dB boost
            try {
                val sessionId = exoPlayer.audioSessionId
                if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
                    val enhancer = android.media.audiofx.LoudnessEnhancer(sessionId)
                    enhancer.setTargetGain((gainDb * 100).toInt()) // gain in milliBels
                    enhancer.enabled = true
                }
            } catch (e: Exception) {
                Log.e("PlayerScreen", "Failed to apply Audio Boost", e)
            }
        }
    }

    // Effect to update player when settings change
    LaunchedEffect(settingsState.skipSilence, settingsState.repeatMode) {
        exoPlayer.setSkipSilenceEnabled(settingsState.skipSilence)
        exoPlayer.setRepeatMode(if (settingsState.repeatMode) androidx.media3.common.Player.REPEAT_MODE_ALL else androidx.media3.common.Player.REPEAT_MODE_OFF)
    }

    // Resume Playback Effect
    LaunchedEffect(resolvedItem, exoPlayer) {
        if (settingsState.resumePlaying && resolvedItem.url.isNotBlank()) {
            val savedPos = try {
                historyRepository.getLastPosition(resolvedItem.name, groupTitle, playlistId)
            } catch (e: Exception) { 0L }
            
            if (savedPos > 5000) { // Only resume if > 5 seconds
                exoPlayer.seekTo(savedPos)
                android.widget.Toast.makeText(context, "Resuming playback", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Playback Speed Effect
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Sleep Timer Effect
    LaunchedEffect(sleepTimerActive, sleepTimerMillis) {
        if (sleepTimerActive && sleepTimerMillis > 0) {
            var remaining = sleepTimerMillis
            while (remaining > 0 && sleepTimerActive) {
                kotlinx.coroutines.delay(1000)
                remaining -= 1000
                if (remaining <= 0) {
                    exoPlayer.pause()
                    sleepTimerActive = false
                    android.widget.Toast.makeText(context, "Sleep timer: Playback stopped", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // History Save Effect — ticks every 10s while player is alive, skips when paused
    LaunchedEffect(resolvedItem, exoPlayer) {
        if (resolvedItem.url.isBlank()) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(10_000)
            // Only persist when actually playing and past the 5s mark
            if (!exoPlayer.isPlaying) continue
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (pos > 5000) {
                try {
                    historyRepository.saveProgress(
                        url = resolvedItem.url,
                        name = resolvedItem.name,
                        logo = resolvedItem.logoUrl,
                        group = groupTitle,
                        playlistId = playlistId,
                        userAgent = resolvedItem.userAgent,
                        headers = resolvedItem.headers,
                        cookie = cookie,
                        referer = referer,
                        origin = origin,
                        licenseUrl = resolvedItem.licenseUrl,
                        drmScheme = resolvedItem.drmScheme,
                        drmHeaders = resolvedItem.drmHeaders,
                        position = pos,
                        duration = if (dur == C.TIME_UNSET) 0L else dur
                    )
                } catch (e: Exception) {
                    Log.e("PlayerScreen", "Failed to save progress", e)
                }
            }
        }
    }

    DisposableEffect(playerHolder) {
        onDispose {
            playerHolder.player?.let { activeExo ->
                val pos = try { activeExo.currentPosition } catch (e: Exception) { 0L }
                val dur = try { activeExo.duration } catch (e: Exception) { 0L }
                if (pos > 5000 && resolvedItem.url.isNotBlank()) {
                     CoroutineScope(Dispatchers.IO).launch {
                         try {
                             historyRepository.saveProgress(
                                 url = resolvedItem.url,
                                 name = resolvedItem.name,
                                 logo = resolvedItem.logoUrl,
                                 group = groupTitle,
                                 playlistId = playlistId,
                                 userAgent = resolvedItem.userAgent,
                                 headers = resolvedItem.headers,
                                 cookie = cookie,
                                 referer = referer,
                                 origin = origin,
                                 licenseUrl = resolvedItem.licenseUrl,
                                 drmScheme = resolvedItem.drmScheme,
                                 drmHeaders = resolvedItem.drmHeaders,
                                 position = pos,
                                 duration = if (dur == C.TIME_UNSET || dur < 0) 0L else dur
                             )
                         } catch (e: Exception) {
                             Log.e("PlayerScreen", "Failed to save final progress on dispose", e)
                         }
                     }
                }
            }
            playerHolder.release()
        }
    }

    LaunchedEffect(resolvedItem, exoPlayer, forceRefreshToken) {
        if (resolvedItem.url.isNotBlank()) {
            val lowerUrl = resolvedItem.url.lowercase()
            val manifestType = resolvedItem.headers["X-Manifest-Type"]?.lowercase() ?: ""
            val inferredMimeType = when {
                manifestType == "mpd" || manifestType == "dash" -> androidx.media3.common.MimeTypes.APPLICATION_MPD
                manifestType == "hls" || manifestType == "m3u8" -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                manifestType == "ism" || manifestType == "smoothstreaming" -> androidx.media3.common.MimeTypes.APPLICATION_SS
                lowerUrl.contains(".m3u8") || lowerUrl.contains("type=hls") || lowerUrl.contains("/hls/") || lowerUrl.contains(".php?m3u=1") -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                lowerUrl.contains(".mpd") || lowerUrl.contains("/mpd/") || lowerUrl.contains("type=dash") || lowerUrl.contains("/dash/") || lowerUrl.contains("manifest.mpd") || lowerUrl.contains("index.mpd") -> androidx.media3.common.MimeTypes.APPLICATION_MPD
                lowerUrl.contains(".ism") || lowerUrl.contains("/manifest") -> androidx.media3.common.MimeTypes.APPLICATION_SS
                else -> null
            }

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(Uri.parse(resolvedItem.url))
                .setMediaId(resolvedItem.url)
                .apply { inferredMimeType?.let { setMimeType(it) } }
                .apply {
                    if (!isLikelyVod) {
                        setLiveConfiguration(
                            MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(4000L) // 4s target offset gives enough buffer cushion to avoid stalling at live edge
                                .setMinPlaybackSpeed(0.98f)
                                .setMaxPlaybackSpeed(1.02f)
                                .build()
                        )
                    }
                }
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(resolvedItem.name)
                        .setArtworkUri(resolvedItem.logoUrl.takeIf { it.isNotBlank() }?.let { Uri.parse(it) })
                        .build()
                )

            // DRM CONFIGURATION
            val effectiveScheme = resolvedItem.drmScheme?.lowercase() ?: ""
            val effectiveLicenseUrl = resolvedItem.licenseUrl
            val hasDrmHeaders = resolvedItem.drmHeaders.isNotEmpty()

            val isExplicitClearKey = effectiveScheme.contains("clearkey") ||
                effectiveLicenseUrl?.lowercase()?.contains("clearkey") == true ||
                resolvedItem.drmHeaders.keys.any { it.contains("clearkey", ignoreCase = true) || it.equals("X-ClearKey-Data", ignoreCase = true) } ||
                resolvedItem.headers.keys.any { it.contains("clearkey", ignoreCase = true) || it.contains("http-clearkey", ignoreCase = true) } ||
                (!effectiveLicenseUrl.isNullOrBlank() && effectiveLicenseUrl.contains(":") && !effectiveLicenseUrl.startsWith("http"))

            // Only enter DRM mode when we have something actionable:
            //  - an explicit DRM scheme, OR
            //  - an explicit license URL, OR
            //  - non-empty DRM headers, OR
            //  - isExplicitClearKey
            val shouldConfigureDrm = isExplicitClearKey || effectiveScheme.isNotEmpty() || !effectiveLicenseUrl.isNullOrBlank() || hasDrmHeaders
            
            if (shouldConfigureDrm) {
                val uuid = when {
                    isExplicitClearKey -> C.CLEARKEY_UUID
                    effectiveScheme.contains("widevine") -> C.WIDEVINE_UUID
                    effectiveScheme.contains("playready") -> C.PLAYREADY_UUID
                    else -> {
                        val licenseUrlLower = effectiveLicenseUrl?.lowercase() ?: ""
                        when {
                            licenseUrlLower.contains("widevine") || licenseUrlLower.contains("proxy/lic/widevine") || licenseUrlLower.contains("lic/wv") || licenseUrlLower.contains("proxy/lic") -> C.WIDEVINE_UUID
                            !effectiveLicenseUrl.isNullOrBlank() -> C.WIDEVINE_UUID // Has a license URL, default to Widevine
                            else -> null
                        }
                    }
                }

                if (uuid != null) {
                    Log.d("PlayerScreen", "DRM UUID detected: $uuid")
                    Log.d("PlayerScreen", "Effective Scheme: $effectiveScheme, Effective License URL: $effectiveLicenseUrl")
                    Log.d("PlayerScreen", "Resolved Item - DRM Scheme: ${resolvedItem.drmScheme}, License URL: ${resolvedItem.licenseUrl}")
                    
                    val drmBuilder = DrmConfiguration.Builder(uuid)
                    
                    if (uuid == C.CLEARKEY_UUID) {
                        // Check if we have embedded key pair or JSON
                        val rawKey = when {
                            !effectiveLicenseUrl.isNullOrBlank() && (
                                (effectiveLicenseUrl.trim().startsWith("{") && effectiveLicenseUrl.contains("\"keys\"")) ||
                                effectiveLicenseUrl.contains("clearkey.direct", ignoreCase = true) ||
                                (effectiveLicenseUrl.contains(":") && !effectiveLicenseUrl.startsWith("http")) ||
                                (effectiveLicenseUrl.contains(":") && !effectiveLicenseUrl.contains("?") && !effectiveLicenseUrl.contains("&") && Regex("([0-9a-zA-Z_\\-]{22,32}):([0-9a-zA-Z_\\-]{22,32})").containsMatchIn(effectiveLicenseUrl))
                            ) -> effectiveLicenseUrl
                            resolvedItem.drmHeaders.containsKey("X-ClearKey-Data") -> resolvedItem.drmHeaders["X-ClearKey-Data"]
                            resolvedItem.drmHeaders.containsKey("clearkey") -> resolvedItem.drmHeaders["clearkey"]
                            resolvedItem.headers.containsKey("clearkey") -> resolvedItem.headers["clearkey"]
                            resolvedItem.headers.containsKey("http-clearkey") -> resolvedItem.headers["http-clearkey"]
                            else -> null
                        }

                        val synthesizedJson = rawKey?.let { generateClearKeyJson(it) }
                        if (!synthesizedJson.isNullOrBlank()) {
                            Log.d("PlayerScreen", "Using synthesized ClearKey JSON: $synthesizedJson")
                            val updatedDrmHeaders = resolvedItem.drmHeaders.toMutableMap()
                            updatedDrmHeaders["X-ClearKey-Data"] = synthesizedJson
                            val dUri = "data:application/json;base64," + Base64.encodeToString(synthesizedJson.toByteArray(), Base64.NO_WRAP)
                            drmBuilder.setLicenseUri(Uri.parse(dUri))
                            drmBuilder.setLicenseRequestHeaders(updatedDrmHeaders)
                        } else if (!effectiveLicenseUrl.isNullOrBlank()) {
                            Log.d("PlayerScreen", "Using remote ClearKey License URL: $effectiveLicenseUrl")
                            drmBuilder.setLicenseUri(Uri.parse(effectiveLicenseUrl))
                            val reqHeaders = resolvedItem.drmHeaders.toMutableMap()
                            if (resolvedItem.userAgent.isNotBlank() && !reqHeaders.containsKey("User-Agent")) {
                                reqHeaders["User-Agent"] = resolvedItem.userAgent
                            }
                            drmBuilder.setLicenseRequestHeaders(reqHeaders)
                        } else {
                            val reqHeaders = resolvedItem.drmHeaders.toMutableMap()
                            drmBuilder.setLicenseRequestHeaders(reqHeaders)
                        }
                    } else {
                        // Widevine or PlayReady
                        if (!effectiveLicenseUrl.isNullOrBlank()) {
                            Log.d("PlayerScreen", "Standard License URL configuration: $effectiveLicenseUrl")
                            drmBuilder.setLicenseUri(Uri.parse(effectiveLicenseUrl))
                        }
                        val reqHeaders = resolvedItem.drmHeaders.toMutableMap()
                        if (resolvedItem.userAgent.isNotBlank() && !reqHeaders.containsKey("User-Agent")) {
                            reqHeaders["User-Agent"] = resolvedItem.userAgent
                        }
                        drmBuilder.setLicenseRequestHeaders(reqHeaders)
                    }

                    drmBuilder.setMultiSession(true)
                    val drmConfig = drmBuilder.build()
                    Log.d("PlayerScreen", "Built DRM Config - License URI: ${drmConfig.licenseUri}, Scheme: ${drmConfig.scheme}")
                    Log.d("PlayerScreen", "DRM Config Headers: ${drmConfig.licenseRequestHeaders}")
                    mediaItemBuilder.setDrmConfiguration(drmConfig)
                    
                    Log.d("PlayerScreen", "Applied DRM Config: UUID=$uuid, Scheme=$effectiveScheme, License=$effectiveLicenseUrl")
                }
            }
            if (currentPlaylistUris != null && currentPlaylistUris!!.isNotEmpty()) {
                val pUris = currentPlaylistUris!!
                val mediaItems = pUris.mapIndexed { idx, url ->
                    val title = playlistTitles?.getOrNull(idx) ?: "Track ${idx + 1}"
                    val itemMimeType = when {
                        url.lowercase().contains(".m3u8") || url.lowercase().contains("type=hls") -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                        url.lowercase().contains(".mpd") || url.lowercase().contains("type=dash") -> androidx.media3.common.MimeTypes.APPLICATION_MPD
                        else -> null
                    }
                    MediaItem.Builder()
                        .setUri(Uri.parse(url))
                        .setMediaId(url)
                        .apply { itemMimeType?.let { setMimeType(it) } }
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(title)
                                .build()
                        )
                        .build()
                }
                exoPlayer.setMediaItems(mediaItems)
                if (playlistIndex >= 0 && playlistIndex < mediaItems.size) {
                    exoPlayer.seekTo(playlistIndex, 0L)
                }
            } else {
                exoPlayer.setMediaItem(mediaItemBuilder.build())
            }
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }




    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            Log.e("PlayerScreen", "Failed to request focus", e)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event: KeyEvent ->
                if (event.type == KeyEventType.KeyDown) {
                    val keyCode = event.nativeKeyEvent.keyCode
                    when {
                        // Dismiss channel drawer if open
                        showChannelDrawer -> {
                            if (keyCode == android.view.KeyEvent.KEYCODE_BACK ||
                                keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT ||
                                event.key == Key.Back || event.key == Key.DirectionRight) {
                                showChannelDrawer = false
                                true
                            } else {
                                false
                            }
                        }
                        // Open channel drawer via D-pad Left, Guide, or Menu
                        keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT ||
                        keyCode == android.view.KeyEvent.KEYCODE_GUIDE ||
                        keyCode == android.view.KeyEvent.KEYCODE_MENU ||
                        event.key == Key.DirectionLeft || event.key == Key.Menu -> {
                            if (canShowDrawer) {
                                showChannelDrawer = true
                                true
                            } else false
                        }
                        // Channel Up (Remote Channel Up or D-pad Up when controls hidden)
                        keyCode == android.view.KeyEvent.KEYCODE_CHANNEL_UP ||
                        (!isControlsVisible && (keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP || event.key == Key.DirectionUp)) -> {
                            if (!isLocalOrDirectPlay && drawerChannels.isNotEmpty()) {
                                val currIdx = drawerChannels.indexOfFirst { it.url == currentStreamUrl }
                                val prevIdx = if (currIdx > 0) currIdx - 1 else drawerChannels.size - 1
                                switchChannel(drawerChannels[prevIdx])
                                true
                            } else if (activePlayer.hasPreviousMediaItem()) {
                                activePlayer.seekToPreviousMediaItem()
                                activePlayer.play()
                                true
                            } else false
                        }
                        // Channel Down (Remote Channel Down or D-pad Down when controls hidden)
                        keyCode == android.view.KeyEvent.KEYCODE_CHANNEL_DOWN ||
                        (!isControlsVisible && (keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN || event.key == Key.DirectionDown)) -> {
                            if (!isLocalOrDirectPlay && drawerChannels.isNotEmpty()) {
                                val currIdx = drawerChannels.indexOfFirst { it.url == currentStreamUrl }
                                val nextIdx = if (currIdx >= 0 && currIdx < drawerChannels.size - 1) currIdx + 1 else 0
                                switchChannel(drawerChannels[nextIdx])
                                true
                            } else if (activePlayer.hasNextMediaItem()) {
                                activePlayer.seekToNextMediaItem()
                                activePlayer.play()
                                true
                            } else false
                        }
                        // Center / Enter toggles controls if not visible
                        !isControlsVisible && (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || event.key == Key.DirectionCenter) -> {
                            isControlsVisible = true
                            false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        val autoResolvedMode = remember(currentVideoWidth, currentVideoHeight, activityContext) {
            val displayMetrics = activityContext?.resources?.displayMetrics
            if (currentVideoWidth <= 0 || currentVideoHeight <= 0 || displayMetrics == null) {
                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            } else {
                val screenW = displayMetrics.widthPixels.toFloat()
                val screenH = displayMetrics.heightPixels.toFloat()
                val screenRatio = if (screenW > screenH) screenW / screenH else screenH / screenW
                val videoRatio = if (currentVideoWidth > currentVideoHeight) {
                    currentVideoWidth.toFloat() / currentVideoHeight.toFloat()
                } else {
                    currentVideoHeight.toFloat() / currentVideoWidth.toFloat()
                }
                val diff = kotlin.math.abs((screenRatio - videoRatio).toDouble()).toFloat() / screenRatio
                if (diff < 0.12f) {
                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        }

        val defaultExoMode = when (settingsState.defaultAspectRatio) {
            1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            2 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
            3 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> autoResolvedMode // 0 = Auto
        }

        AndroidView(
            factory = { _ -> // Ignore the view creation context, use the captured activity context
                // Explicitly wrap the context with the theme to ensure MediaRouteButton works
                val contextThemeWrapper = android.view.ContextThemeWrapper(activityContext, com.codesrahul.streamplus.R.style.Theme_M3UValidator)
                val view = android.view.LayoutInflater.from(contextThemeWrapper).inflate(com.codesrahul.streamplus.R.layout.player_view_wrapper, null)
                val playerView = view.findViewById<androidx.media3.ui.PlayerView>(com.codesrahul.streamplus.R.id.player_view)
                playerView.player = activePlayer
                playerView.keepScreenOn = true

                // Add a listener to sync our Compose state with the actual ExoPlayer controller visibility
                playerView.setControllerVisibilityListener(androidx.media3.ui.PlayerView.ControllerVisibilityListener { visibility ->
                    android.util.Log.d("PlayerScreen", "ControllerVisibilityListener: visibility = $visibility, isLocked = $isLocked")
                    if (!isLocked) {
                        isControlsVisible = (visibility == android.view.View.VISIBLE)
                    }
                    if (visibility == android.view.View.VISIBLE && !isLikelyVod) {
                        playerView.findViewById<android.widget.TextView>(com.codesrahul.streamplus.R.id.tv_live_badge)?.visibility = android.view.View.VISIBLE
                        val liveTimeStr = formatElapsedTime(liveWatchTimeSeconds * 1000L)
                        playerView.findViewById<android.widget.TextView>(com.codesrahul.streamplus.R.id.tv_time_combined)?.text = "🔴 LIVE • $liveTimeStr"
                        playerView.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)?.visibility = android.view.View.GONE
                        playerView.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_rew)?.visibility = android.view.View.GONE
                        playerView.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_ffwd)?.visibility = android.view.View.GONE
                    }
                })

                
                val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
                val detector = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                    val window = activityContext?.window

                    override fun onDown(e: android.view.MotionEvent): Boolean {
                        volumeAccumulator = 0f // Reset for new gesture
                        if (isLocked) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                        return true
                    }

                    override fun onScroll(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                        if (isLocked || e1 == null) return false
                        val width = playerView.width
                        val height = playerView.height
                        if (width <= 0 || height <= 0) return false

                        val totalDy = kotlin.math.abs(e2.y - e1.y)
                        val totalDx = kotlin.math.abs(e2.x - e1.x)

                        // Guard against micro-jitter/tap movement: only trigger when moving vertically beyond touch slop threshold
                        if (!isGestureScrolling) {
                            if (totalDy < touchSlop * 1.5f || totalDy <= totalDx * 1.5f) {
                                return false
                            }
                            isGestureScrolling = true
                        }

                        val isLeft = e1.x < width / 2
                        
                        // Vertical sensitivity
                        if (kotlin.math.abs(distanceY) > kotlin.math.abs(distanceX)) {
                            val delta = distanceY / height
                            
                            if (isLeft && settingsState.brightnessGestures) {
                                window?.let { win ->
                                    val lp = win.attributes
                                    val curr = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                                    val newVal = (curr + delta).coerceIn(0f, 1f)
                                    lp.screenBrightness = newVal
                                    win.attributes = lp
                                    activeGestureTarget = GestureTarget.BRIGHTNESS
                                    brightnessLevel = newVal
                                    gestureLevel = newVal
                                    showGestureOverlay = true
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                }
                                return true
                            } else if (!isLeft && settingsState.volumeGestures) {
                                val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                                val curr = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                                
                                // Accumulate delta for discrete volume steps
                                volumeAccumulator += delta
                                val step = 1f / max
                                
                                if (kotlin.math.abs(volumeAccumulator) >= step) {
                                    val chg = if (volumeAccumulator > 0) 1 else -1
                                    val target = (curr + chg).coerceIn(0, max)
                                    volumeAccumulator -= (chg * step) // Reduce accumulator by one step
                                    
                                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, target, 0)
                                    activeGestureTarget = GestureTarget.VOLUME
                                    val newFraction = target.toFloat() / max
                                    volumeLevel = newFraction
                                    gestureLevel = newFraction
                                    showGestureOverlay = true
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                }
                                return true
                            }
                        }
                        return false
                    }

                    override fun onSingleTapConfirmed(e: android.view.MotionEvent): Boolean {
                        if (isLocked) {
                            // When locked, a single tap toggles the large Unlock icon overlay (isControlsVisible)
                            isControlsVisible = !isControlsVisible
                            return true
                        }
                        if (playerView.isControllerFullyVisible) {
                            playerView.hideController()
                        } else {
                            playerView.showController()
                        }
                        return true
                    }

                    override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                        if (isLocked) return false
                        val w = playerView.width
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        if (e.x < w / 3) {
                            val target = maxOf(0L, exoPlayer.currentPosition - 10000)
                            exoPlayer.seekTo(target)
                            seekDirection = "Rewind"
                            showSeekOverlay = true
                            return true
                        } else if (e.x > w * 2 / 3) {
                            val dur = if (exoPlayer.duration == C.TIME_UNSET) 0L else exoPlayer.duration
                            val target = minOf(dur, exoPlayer.currentPosition + 10000)
                            exoPlayer.seekTo(target)
                            seekDirection = "Forward"
                            showSeekOverlay = true
                            return true
                        }
                        return false
                    }
                })

                playerView.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            isTouching = true
                            isGestureScrolling = false
                            volumeAccumulator = 0f
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            isTouching = false
                            volumeAccumulator = 0f
                            if (isGestureScrolling || showGestureOverlay) {
                                isGestureScrolling = false
                                gestureTrigger = System.currentTimeMillis()
                            }
                        }
                    }
                    detector.onTouchEvent(event)
                    true 
                }
                playerViewRef = playerView
                view
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val playerView = view.findViewById<androidx.media3.ui.PlayerView>(com.codesrahul.streamplus.R.id.player_view)
                playerViewRef = playerView
                if (playerView.player != activePlayer) playerView.player = activePlayer
                
                // Bind the stream title & format quality badge
                playerView.findViewById<android.widget.TextView>(com.codesrahul.streamplus.R.id.tv_player_title)?.text = currentStreamTitle
                
                val badgeView = playerView.findViewById<android.widget.TextView>(com.codesrahul.streamplus.R.id.tv_stream_quality_badge)
                val badgeTokens = listOfNotNull(
                    videoResolution.takeIf { it.isNotBlank() },
                    videoFps.takeIf { it.isNotBlank() },
                    videoCodec.takeIf { it.isNotBlank() },
                    audioChannels.takeIf { it.isNotBlank() }
                )
                if (badgeTokens.isNotEmpty()) {
                    badgeView?.text = badgeTokens.joinToString(" • ")
                    badgeView?.visibility = android.view.View.VISIBLE
                } else {
                    badgeView?.text = "AUTO HD"
                    badgeView?.visibility = android.view.View.VISIBLE
                }

                // Live Badge in Top Bar
                val liveBadgeView = playerView.findViewById<android.widget.TextView>(com.codesrahul.streamplus.R.id.tv_live_badge)
                liveBadgeView?.visibility = if (!isLikelyVod) android.view.View.VISIBLE else android.view.View.GONE

                // Combined Time and Live UI Presentation
                val timeCombinedView = playerView.findViewById<android.widget.TextView>(com.codesrahul.streamplus.R.id.tv_time_combined)
                val progressBarView = playerView.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)
                val rewBtnView = playerView.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_rew)
                val ffwdBtnView = playerView.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_ffwd)

                if (!isLikelyVod) {
                    val liveTimeStr = formatElapsedTime(liveWatchTimeSeconds * 1000L)
                    timeCombinedView?.text = "🔴 LIVE • $liveTimeStr"
                    progressBarView?.visibility = android.view.View.GONE
                    rewBtnView?.visibility = android.view.View.GONE
                    ffwdBtnView?.visibility = android.view.View.GONE
                } else {
                    timeCombinedView?.text = "$currentTimeFormatted / $durationFormatted"
                    progressBarView?.visibility = android.view.View.VISIBLE
                    rewBtnView?.visibility = android.view.View.VISIBLE
                    ffwdBtnView?.visibility = android.view.View.VISIBLE
                }

                playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.btn_close)?.setOnClickListener { activityContext?.finish() }
                
                playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.exo_speed_top)?.setOnClickListener { 
                    showSpeedSheet = true
                }

                val playlistBtn = playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.exo_playlist_top)
                val hasPlaylistContent = (!isLocalOrDirectPlay && drawerChannels.isNotEmpty()) || (currentPlaylistUris != null && currentPlaylistUris!!.size > 1)
                playlistBtn?.visibility = if (hasPlaylistContent) android.view.View.VISIBLE else android.view.View.GONE
                playlistBtn?.setOnClickListener { 
                    showChannelDrawer = !showChannelDrawer
                }

                playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.exo_settings)?.setOnClickListener { 
                    showSettingsSheet = true
                }
                
                val targetMode = if (sessionResizeMode != null) {
                    if (sessionResizeMode == -1) autoResolvedMode else sessionResizeMode!!
                } else {
                    defaultExoMode
                }
                if (playerView.resizeMode != targetMode) {
                    playerView.resizeMode = targetMode
                }

                playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.exo_resize_mode_button)?.setOnClickListener {
                    val currentMode = sessionResizeMode ?: if (settingsState.defaultAspectRatio == 0) -1 else playerView.resizeMode
                    val (newMode, msg) = when (currentMode) {
                        -1 -> Pair(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fit")
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT -> Pair(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL, "Fill")
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL -> Pair(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Zoom")
                        else -> Pair(-1, "Auto")
                    }
                    sessionResizeMode = newMode
                    val resolvedMode = if (newMode == -1) autoResolvedMode else newMode
                    playerView.resizeMode = resolvedMode
                    android.widget.Toast.makeText(playerView.context, "Resize Mode: $msg", android.widget.Toast.LENGTH_SHORT).show()
                }
                playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.btn_landscape)?.setOnClickListener {
                    activityContext?.let { act ->
                        val currentOrientation = act.resources.configuration.orientation
                        if (currentOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                            act.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            act.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                    }
                }
                playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.btn_pip)?.setOnClickListener { onEnterPip() }
                
                // Hide controls in PiP and Lock Mode
                playerView.useController = !isInPip && !isLocked
                if (isInPip || isLocked) playerView.hideController()
            }
        )

        // Robust Lock Mode Overlay
        AnimatedVisibility(
            visible = isLocked && !isInPip && isControlsVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                onClick = { 
                    isLocked = false
                    isControlsVisible = true // Show regular Exo controls immediately when unlocked
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    android.widget.Toast.makeText(context, "Controls Unlocked", android.widget.Toast.LENGTH_SHORT).show()
                },
                color = Color.Black.copy(alpha = 0.4f),
                shape = CircleShape,
                modifier = Modifier.size(80.dp),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Small Lock Toggle Overlay (Only visible when controls are visible AND unlocked)
        AnimatedVisibility(
            visible = isControlsVisible && !isLocked && !isInPip && !showGestureOverlay,
            enter = fadeIn() + if (isLandscape) slideInHorizontally() else slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + if (isLandscape) slideOutHorizontally() else slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(if (isLandscape) Alignment.CenterStart else Alignment.TopStart)
                .padding(
                    start = if (isLandscape) 32.dp else 20.dp,
                    top = if (isLandscape) 0.dp else 84.dp
                )
        ) {
            Surface(
                onClick = { 
                    isLocked = true
                    // When locking, we temporarily keep isControlsVisible = true so the unlock button is shown right away,
                    // and it will auto-hide in 4s via the LaunchedEffect.
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    android.widget.Toast.makeText(context, "Controls Locked", android.widget.Toast.LENGTH_SHORT).show()
                },
                color = Color(0xD90E0E18),
                shape = CircleShape,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                shadowElevation = 8.dp,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Lock Controls",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Seek Animation Overlay
        AnimatedVisibility(
            visible = showSeekOverlay && !isInPip,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(initialScale = 0.85f),
            exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) + scaleOut(targetScale = 0.85f),
            modifier = Modifier.align(if (seekDirection == "Rewind") Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 48.dp)
        ) {
            Surface(
                color = Color(0xD90A0A14),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF06B6D4).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (seekDirection == "Rewind") Icons.Default.FastRewind else Icons.Default.FastForward,
                            contentDescription = seekDirection,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        if (seekDirection == "Rewind") "-10 SEC" else "+10 SEC",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

    // Animated gesture levels
    val animatedBrightnessLevel by animateFloatAsState(
        targetValue = brightnessLevel.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "brightnessLevel"
    )
    val animatedVolumeLevel by animateFloatAsState(
        targetValue = volumeLevel.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "volumeLevel"
    )

    // Brightness Indicator Bar: Triggered on LEFT side -> Displayed on RIGHT side
    AnimatedVisibility(
        visible = showGestureOverlay && !isInPip && activeGestureTarget == GestureTarget.BRIGHTNESS,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) + slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = if (isLandscape) 36.dp else 20.dp)
    ) {
        Surface(
            color = Color(0xD90E0E18),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .height(200.dp)
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val icon = when {
                    animatedBrightnessLevel >= 0.7f -> Icons.Default.BrightnessHigh
                    animatedBrightnessLevel >= 0.3f -> Icons.Default.BrightnessMedium
                    else -> Icons.Default.BrightnessLow
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Brightness",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )

                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .weight(1f)
                        .padding(vertical = 10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedBrightnessLevel)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                )
                            )
                    )
                }

                Text(
                    text = "${(animatedBrightnessLevel * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }

    // Volume Indicator Bar: Triggered on RIGHT side -> Displayed on LEFT side
    AnimatedVisibility(
        visible = showGestureOverlay && !isInPip && activeGestureTarget == GestureTarget.VOLUME,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) + slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(start = if (isLandscape) 36.dp else 20.dp)
    ) {
        Surface(
            color = Color(0xD90E0E18),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .height(200.dp)
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val icon = when {
                    animatedVolumeLevel <= 0.001f -> Icons.Default.VolumeOff
                    animatedVolumeLevel < 0.5f -> Icons.Default.VolumeDown
                    else -> Icons.Default.VolumeUp
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Volume",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )

                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .weight(1f)
                        .padding(vertical = 10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedVolumeLevel)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                )
                            )
                    )
                }

                Text(
                    text = "${(animatedVolumeLevel * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }

        // DVR Stream Recording State
        val recordingState by streamRecorder.recordingState.collectAsState()

        val isLocalMedia = remember(currentStreamUrl, currentPlaylistUris) {
            currentStreamUrl.startsWith("content://") ||
                    currentStreamUrl.startsWith("file://") ||
                    currentStreamUrl.startsWith("/storage/") ||
                    currentStreamUrl.startsWith("/data/") ||
                    (currentPlaylistUris?.firstOrNull()?.let {
                        it.startsWith("content://") || it.startsWith("file://") || it.startsWith("/storage/") || it.startsWith("/data/")
                    } ?: false)
        }
        val isLiveTv = !isLikelyVod && !isLocalMedia

        // Active Recording HUD Pill (Always visible while recording Live TV)
        AnimatedVisibility(
            visible = isLiveTv && recordingState.isRecording && !isInPip,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 28.dp, end = 32.dp)
        ) {
            val mins = recordingState.durationSeconds / 60
            val secs = recordingState.durationSeconds % 60
            val mb = String.format(java.util.Locale.US, "%.1f MB", recordingState.bytesWritten / (1024f * 1024f))
            Surface(
                color = Color(0xE67F1D1D),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(java.util.Locale.US, "REC  %02d:%02d  (%s)", mins, secs, mb),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Quick Controls Floating Action Bar (DVR Record + Multi-View: strictly Live TV channels only)
        AnimatedVisibility(
            visible = isLiveTv && isControlsVisible && !isInPip && !showChannelDrawer,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 24.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // DVR Record Button
                Surface(
                    onClick = {
                        if (recordingState.isRecording) {
                            streamRecorder.stopRecording(context)
                        } else {
                            if (!currentDrmScheme.isNullOrBlank()) {
                                android.widget.Toast.makeText(context, "DRM-protected streams cannot be recorded", android.widget.Toast.LENGTH_LONG).show()
                                return@Surface
                            }
                            streamRecorder.startRecording(
                                context = context,
                                streamUrl = currentStreamUrl,
                                channelName = currentStreamTitle,
                                headers = currentHeaders,
                                userAgent = currentUserAgent
                            )
                        }
                    },
                    color = if (recordingState.isRecording) Color(0xFFEF4444) else Color(0xCC101726),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (recordingState.isRecording) Color.White else Color(0xFF2E3852)),
                    modifier = Modifier.height(36.dp).tvFocusable(cornerRadius = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = "DVR Record",
                            tint = if (recordingState.isRecording) Color.White else Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (recordingState.isRecording) "Stop REC" else "Record",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Multi-View Grid Button
                Surface(
                    onClick = {
                        val intent = android.content.Intent(context, com.codesrahul.streamplus.ui.multiview.MultiViewActivity::class.java).apply {
                            putExtra(PlayerActivity.EXTRA_STREAM_URL, currentStreamUrl)
                            putExtra(PlayerActivity.EXTRA_STREAM_TITLE, currentStreamTitle)
                            putExtra(PlayerActivity.EXTRA_PLAYLIST_ID, playlistId)
                            putExtra(PlayerActivity.EXTRA_USER_AGENT, currentUserAgent)
                            putExtra(PlayerActivity.EXTRA_HEADERS, HashMap(currentHeaders))
                            putExtra(PlayerActivity.EXTRA_DRM_SCHEME, currentDrmScheme)
                            putExtra(PlayerActivity.EXTRA_DRM_LICENSE_URL, currentDrmLicenseUrl)
                            if (currentDrmHeaders.isNotEmpty()) {
                                putExtra(PlayerActivity.EXTRA_DRM_HEADERS, HashMap(currentDrmHeaders))
                            }
                        }
                        context.startActivity(intent)
                    },
                    color = Color(0xCC101726),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E3852)),
                    modifier = Modifier.height(36.dp).tvFocusable(cornerRadius = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = "Multi-View",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Multi-View",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Quick Channels Drawer (Slide-in Frosted Glass Sheet)
        AnimatedVisibility(
            visible = showChannelDrawer && !isInPip && canShowDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(360.dp)
        ) {
            Surface(
                color = Color(0xF50A0E18),
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                border = BorderStroke(1.dp, Color(0xFF1E283D)),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    val drawerTitle = if (isLocalQueue) "Videos (${currentPlaylistUris!!.size})" else "Channels"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            drawerTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { showChannelDrawer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isLocalQueue) {
                        // Search input for IPTV channels
                        OutlinedTextField(
                            value = drawerSearchQuery,
                            onValueChange = { drawerSearchQuery = it },
                            placeholder = { Text("Filter channels...", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (isLocalQueue) {
                        val pUris = currentPlaylistUris!!
                        val currentMediaIdx = activePlayer.currentMediaItemIndex
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                count = pUris.size,
                                key = { idx -> "local_${pUris[idx]}_$idx" }
                            ) { idx ->
                                val vUri = pUris[idx]
                                val vTitle = playlistTitles?.getOrNull(idx) ?: "Video ${idx + 1}"
                                val isCurrent = idx == currentMediaIdx || vUri == currentStreamUrl

                                Surface(
                                    onClick = {
                                        if (activePlayer.mediaItemCount > idx) {
                                            activePlayer.seekTo(idx, 0L)
                                            activePlayer.play()
                                        }
                                        currentStreamUrl = vUri
                                        currentStreamTitle = vTitle
                                        showChannelDrawer = false
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    },
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
                                    ),
                                    modifier = Modifier.fillMaxWidth().tvFocusable()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Movie,
                                            contentDescription = null,
                                            tint = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            vTitle,
                                            color = Color.White,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isCurrent) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Playing",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val filteredChannels = remember(drawerChannels, drawerSearchQuery) {
                            if (drawerSearchQuery.isBlank()) drawerChannels
                            else drawerChannels.filter { it.name.contains(drawerSearchQuery, ignoreCase = true) }
                        }

                        if (filteredChannels.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No channels found", color = Color.Gray, fontSize = 14.sp)
                            }
                        } else {
                            androidx.compose.foundation.lazy.LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    count = filteredChannels.size,
                                    key = { idx ->
                                        val ch = filteredChannels[idx]
                                        if (ch.id != 0) "ch_${ch.id}" else "${ch.name}_${ch.url}_$idx"
                                    }
                                ) { idx ->
                                    val ch = filteredChannels[idx]
                                    val isCurrent = ch.url == currentStreamUrl
                                    val currentProg = remember(ch.epgId) { ch.epgId?.let { epgManager.getCurrentProgram(it) } }
                                    Surface(
                                        onClick = { switchChannel(ch) },
                                        color = if (isCurrent) Color(0xFF14243D) else Color(0xFF101624),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            width = if (isCurrent) 1.5.dp else 0.5.dp,
                                            color = if (isCurrent) Color(0xFF00E5FF) else Color(0xFF1E283D)
                                        ),
                                        modifier = Modifier.fillMaxWidth().tvFocusable(cornerRadius = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (ch.logoUrl.isNotBlank()) {
                                                coil.compose.AsyncImage(
                                                    model = ch.logoUrl,
                                                    contentDescription = ch.name,
                                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF161E2E))
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                            } else {
                                                Box(
                                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF161E2E)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        ch.name,
                                                        color = if (isCurrent) Color(0xFF00E5FF) else Color.White,
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        fontSize = 13.sp,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (isCurrent) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(3.dp))
                                                                .background(Color(0xFFFF1744))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                                                        }
                                                    }
                                                }
                                                if (currentProg != null) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Now: ${currentProg.title}",
                                                        color = Color(0xFF38BDF8),
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                } else if (ch.group.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        if (ch.groupLogo.isNotBlank()) {
                                                            coil.compose.AsyncImage(
                                                                model = ch.groupLogo,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(11.dp).clip(CircleShape)
                                                            )
                                                        }
                                                        Text(
                                                            ch.group,
                                                            color = Color(0xFF64748B),
                                                            fontSize = 11.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                            if (isCurrent) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "Playing",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Modern Settings Bottom Sheet ---
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF121212),
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text(
                    "Player Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Track Sections
                SettingsItem(Icons.Default.HighQuality, "Video Quality") {
                    showSettingsSheet = false
                    showQualitySheet = true
                }
                SettingsItem(Icons.Default.AudioFile, "Audio Tracks") {
                    showSettingsSheet = false
                    showAudioSheet = true
                }
                SettingsItem(Icons.Default.Subtitles, "Subtitles") {
                    showSettingsSheet = false
                    showSubtitleSheet = true
                }
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))

                // Advanced Controls
                SettingsItem(Icons.Default.Speed, "Playback Speed (${playbackSpeed}x)") {
                    showSettingsSheet = false
                    showSpeedSheet = true
                }

                SettingsSliderItem(Icons.Default.VolumeUp, "Audio Boost", audioBoost) {
                    audioBoost = it
                }

                SettingsItem(Icons.Default.Timer, "Sleep Timer ${if (sleepTimerActive) "(Active)" else ""}") {
                    showSettingsSheet = false
                    showSleepTimerSheet = true
                }

                SettingsItem(Icons.Default.FileOpen, "Load External Subtitle") {
                    showSettingsSheet = false
                    subtitlePickerLauncher?.invoke()
                }
            }
        }
    }

    // --- Modern Video Quality Bottom Sheet ---
    if (showQualitySheet) {
        val videoTrackGroups = remember(exoPlayer.currentTracks) {
            exoPlayer.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
        }
        val currentOverrides = exoPlayer.trackSelectionParameters.overrides
        val isAuto = currentOverrides.none { it.value.type == C.TRACK_TYPE_VIDEO }

        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
            containerColor = Color(0xFF14141E),
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HighQuality, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Video Quality",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = { showQualitySheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 8.dp))

                PlayerOptionItem(
                    title = "Auto (Recommended)",
                    subtitle = "Adaptive stream quality based on network speed",
                    isSelected = isAuto,
                    onClick = {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                            .build()
                        showQualitySheet = false
                    }
                )

                videoTrackGroups.forEach { group ->
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isSelected = currentOverrides[group.mediaTrackGroup]?.trackIndices?.contains(i) == true
                        val height = format.height
                        val width = format.width
                        val bitrate = format.bitrate
                        val fps = format.frameRate

                        val title = if (height > 0) "${height}p" else if (width > 0) "${width}x${height}" else format.label ?: "Quality ${i + 1}"
                        val details = buildList {
                            if (bitrate > 0) add("${bitrate / 1000} kbps")
                            if (fps > 0) add("${fps.toInt()} fps")
                            if (format.codecs != null) add(format.codecs)
                        }.joinToString(" • ")

                        PlayerOptionItem(
                            title = title,
                            subtitle = details.ifBlank { null },
                            isSelected = isSelected,
                            onClick = {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i))
                                    .build()
                                showQualitySheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Modern Audio Track Bottom Sheet ---
    if (showAudioSheet) {
        val audioTrackGroups = remember(exoPlayer.currentTracks) {
            exoPlayer.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        }
        val currentOverrides = exoPlayer.trackSelectionParameters.overrides
        val isAuto = currentOverrides.none { it.value.type == C.TRACK_TYPE_AUDIO }

        ModalBottomSheet(
            onDismissRequest = { showAudioSheet = false },
            containerColor = Color(0xFF14141E),
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Audio Tracks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = { showAudioSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 8.dp))

                PlayerOptionItem(
                    title = "Auto (Default)",
                    subtitle = "System default track",
                    isSelected = isAuto,
                    onClick = {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .build()
                        showAudioSheet = false
                    }
                )

                audioTrackGroups.forEach { group ->
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isSelected = currentOverrides[group.mediaTrackGroup]?.trackIndices?.contains(i) == true
                        val friendlyLang = getFriendlyLanguageName(format.language)
                        val title = format.label ?: friendlyLang
                        val details = buildList {
                            if (format.label != null && friendlyLang != format.label) add(friendlyLang)
                            if (format.channelCount > 0) {
                                add(if (format.channelCount == 6) "5.1 Surround" else if (format.channelCount == 2) "Stereo" else "${format.channelCount} ch")
                            }
                            if (format.bitrate > 0) add("${format.bitrate / 1000} kbps")
                        }.joinToString(" • ")

                        PlayerOptionItem(
                            title = title,
                            subtitle = details.ifBlank { null },
                            isSelected = isSelected,
                            onClick = {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i))
                                    .build()
                                showAudioSheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Modern Subtitles Bottom Sheet ---
    if (showSubtitleSheet) {
        val textTrackGroups = remember(exoPlayer.currentTracks) {
            exoPlayer.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        }
        val isTextDisabled = exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
        val currentOverrides = exoPlayer.trackSelectionParameters.overrides

        ModalBottomSheet(
            onDismissRequest = { showSubtitleSheet = false },
            containerColor = Color(0xFF14141E),
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Subtitles, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Subtitles",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = { showSubtitleSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 8.dp))

                PlayerOptionItem(
                    title = "Off",
                    subtitle = "Disable subtitles",
                    isSelected = isTextDisabled,
                    onClick = {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .build()
                        showSubtitleSheet = false
                    }
                )

                textTrackGroups.forEach { group ->
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isSelected = !isTextDisabled && currentOverrides[group.mediaTrackGroup]?.trackIndices?.contains(i) == true
                        val friendlyLang = getFriendlyLanguageName(format.language)
                        val title = format.label ?: friendlyLang

                        PlayerOptionItem(
                            title = title,
                            subtitle = if (format.label != null && friendlyLang != format.label) friendlyLang else null,
                            isSelected = isSelected,
                            onClick = {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                    .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i))
                                    .build()
                                showSubtitleSheet = false
                            }
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                PlayerOptionItem(
                    title = "Load External Subtitle",
                    subtitle = "Import .srt or .vtt file from device storage",
                    isSelected = false,
                    onClick = {
                        showSubtitleSheet = false
                        subtitlePickerLauncher?.invoke()
                    }
                )
            }
        }
    }

    // --- Modern Playback Speed Bottom Sheet ---
    if (showSpeedSheet) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            containerColor = Color(0xFF14141E),
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Playback Speed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = { showSpeedSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 8.dp))

                speeds.forEach { spd ->
                    val isSelected = kotlin.math.abs(playbackSpeed - spd) < 0.01f
                    val label = if (spd == 1.0f) "1.0x (Normal)" else "${spd}x"
                    PlayerOptionItem(
                        title = label,
                        subtitle = null,
                        isSelected = isSelected,
                        onClick = {
                            playbackSpeed = spd
                            exoPlayer.setPlaybackSpeed(spd)
                            showSpeedSheet = false
                        }
                    )
                }
            }
        }
    }

    // --- Modern Sleep Timer Bottom Sheet ---
    if (showSleepTimerSheet) {
        val sleepOptions = listOf(
            0 to "Off",
            15 to "15 Minutes",
            30 to "30 Minutes",
            45 to "45 Minutes",
            60 to "60 Minutes",
            90 to "90 Minutes"
        )
        ModalBottomSheet(
            onDismissRequest = { showSleepTimerSheet = false },
            containerColor = Color(0xFF14141E),
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Sleep Timer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = { showSleepTimerSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 8.dp))

                sleepOptions.forEach { (mins, title) ->
                    val isSelected = if (mins == 0) !sleepTimerActive else (sleepTimerActive && sleepTimerMillis == mins * 60 * 1000L)
                    PlayerOptionItem(
                        title = title,
                        subtitle = if (mins > 0 && isSelected) "Timer active" else null,
                        isSelected = isSelected,
                        onClick = {
                            if (mins == 0) {
                                sleepTimerActive = false
                            } else {
                                sleepTimerMillis = mins * 60 * 1000L
                                sleepTimerActive = true
                            }
                            showSleepTimerSheet = false
                        }
                    )
                }
            }
        }
    }
}

// getUnsafeOkHttpClient removed - using unified OkHttpClient from NetworkModule

private fun showSettingsDialog(context: Context, player: ExoPlayer, currentBoost: Float, onBoostChange: (Float) -> Unit, onLoadSubtitle: () -> Unit) {
    val items = arrayOf("Quality", "Audio Tracks", "Subtitles", "Audio Boost (${(currentBoost * 100).toInt()}%)", "Load External Subtitle")
    android.app.AlertDialog.Builder(context)
        .setTitle("Settings")
        .setItems(items) { _, which ->
            when (which) {
                0 -> showTrackSelectionDialog(context, player, C.TRACK_TYPE_VIDEO)
                1 -> showTrackSelectionDialog(context, player, C.TRACK_TYPE_AUDIO)
                2 -> showTrackSelectionDialog(context, player, C.TRACK_TYPE_TEXT)
                3 -> {
                    val levels = arrayOf("Off", "25%", "50%", "75%", "100% (Max)")
                    android.app.AlertDialog.Builder(context)
                        .setTitle("Audio Boost")
                        .setItems(levels) { _, bWhich ->
                            onBoostChange(bWhich * 0.25f)
                        }.show()
                }
                4 -> onLoadSubtitle()
            }
        }
        .show()
}

private fun showTrackSelectionDialog(context: Context, player: ExoPlayer, trackType: Int) {
    val trackGroups = player.currentTracks.groups.filter { it.type == trackType }
    if (trackGroups.isEmpty()) {
        android.widget.Toast.makeText(context, "No options available", android.widget.Toast.LENGTH_SHORT).show()
        return
    }

    val options = mutableListOf<String>()
    val trackSpecs = mutableListOf<Pair<androidx.media3.common.Tracks.Group, Int>>()

    trackGroups.forEach { group ->
        for (i in 0 until group.length) {
            val format = group.getTrackFormat(i)
            val label = when (trackType) {
                C.TRACK_TYPE_VIDEO -> {
                    val width = format.width
                    val height = format.height
                    val bitrate = format.bitrate
                    if (height > 0) {
                        val res = "${height}p"
                        if (bitrate > 0) "$res (${bitrate / 1000} kbps)" else res
                    } else if (bitrate > 0) {
                        "${bitrate / 1000} kbps"
                    } else {
                        format.label ?: "Auto"
                    }
                }
                C.TRACK_TYPE_AUDIO -> {
                    val lang = format.language
                    val friendlyLang = getFriendlyLanguageName(lang)
                    val labelText = format.label
                    if (labelText != null) "$labelText [$friendlyLang]" else friendlyLang
                }
                C.TRACK_TYPE_TEXT -> {
                    val lang = format.language
                    val friendlyLang = getFriendlyLanguageName(lang)
                    val labelText = format.label
                    if (labelText != null) "$labelText [$friendlyLang]" else friendlyLang
                }
                else -> format.label ?: "Track ${options.size + 1}"
            }
            options.add(label)
            trackSpecs.add(group to i)
        }
    }

    android.app.AlertDialog.Builder(context)
        .setTitle(when(trackType) {
            C.TRACK_TYPE_VIDEO -> "Select Quality"
            C.TRACK_TYPE_AUDIO -> "Select Audio"
            else -> "Select Subtitles"
        })
        .setItems(options.toTypedArray()) { _, which ->
            val (group, trackIndex) = trackSpecs[which]
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                .build()
        }
        .show()
}

private fun hideSystemUI(activity: android.app.Activity) {
    try {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    } catch (e: Exception) {
        Log.e("PlayerActivity", "Failed to hide System UI", e)
    }
}

private fun showSystemUI(activity: android.app.Activity) {
    try {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    } catch (e: Exception) {
        Log.e("PlayerActivity", "Failed to show System UI", e)
    }
}
@Composable
fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().tvFocusable(cornerRadius = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(20.dp))
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun SettingsSliderItem(icon: ImageVector, title: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(20.dp))
            Text("$title (${(value * 100).toInt()}%)", color = Color.White, style = MaterialTheme.typography.bodyLarge)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.padding(top = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun SettingsSwitchItem(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().tvFocusable(cornerRadius = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(20.dp))
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF64B5F6),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
private fun PlayerOptionItem(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .tvFocusable(cornerRadius = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatElapsedTime(timeMs: Long): String {
    if (timeMs < 0) return "00:00"
    val totalSeconds = timeMs / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        java.util.Locale.getDefault().let { locale ->
            String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
        }
    } else {
        java.util.Locale.getDefault().let { locale ->
            String.format(locale, "%02d:%02d", minutes, seconds)
        }
    }
}

private fun getFriendlyLanguageName(langCode: String?): String {
    if (langCode.isNullOrBlank()) return "Unknown"
    return when(langCode.lowercase()) {
        "en", "eng" -> "English"
        "hi", "hin" -> "Hindi"
        "bn", "ben" -> "Bengali"
        "te", "tel" -> "Telugu"
        "mr", "mar" -> "Marathi"
        "ta", "tam" -> "Tamil"
        "ur", "urd" -> "Urdu"
        "gu", "guj" -> "Gujarati"
        "kn", "kan" -> "Kannada"
        "or", "ori" -> "Odia"
        "ml", "mal" -> "Malayalam"
        "pa", "pan" -> "Punjabi"
        "sa", "san" -> "Sanskrit"
        "as", "asm" -> "Assamese"
        "mai" -> "Maithili"
        "sat" -> "Santali"
        "ks", "kas" -> "Kashmiri"
        "ne", "nep" -> "Nepali"
        "kok" -> "Konkani"
        "sd", "snd" -> "Sindhi"
        "doi" -> "Dogri"
        "mni" -> "Manipuri"
        "brx" -> "Bodo"
        "es", "spa" -> "Spanish"
        "fr", "fra", "fre" -> "French"
        "de", "deu", "ger" -> "German"
        "ru", "rus" -> "Russian"
        "ar", "ara" -> "Arabic"
        "zh", "zho", "chi" -> "Chinese"
        "ja", "jpn" -> "Japanese"
        "pt", "por" -> "Portuguese"
        else -> langCode.uppercase()
    }
}

private class PlayerHolder(
    private val context: android.content.Context,
    private val okHttpClientProvider: javax.inject.Provider<okhttp3.OkHttpClient>
) {
    var player: androidx.media3.exoplayer.ExoPlayer? = null
        private set

    fun getOrCreate(
        mediaSourceFactory: androidx.media3.exoplayer.source.MediaSourceFactory,
        loadControl: androidx.media3.exoplayer.LoadControl,
        renderersFactory: androidx.media3.exoplayer.RenderersFactory,
        onReleasePlayer: (androidx.media3.exoplayer.ExoPlayer) -> Unit
    ): androidx.media3.exoplayer.ExoPlayer {
        player?.let {
            try {
                onReleasePlayer(it)
            } catch (e: Exception) {
                android.util.Log.e("PlayerHolder", "Error in onReleasePlayer callback", e)
            }
            try {
                it.release()
            } catch (e: Exception) {
                android.util.Log.e("PlayerHolder", "Error releasing old player", e)
            }
        }
        
        val newPlayer = androidx.media3.exoplayer.ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()
        newPlayer.playWhenReady = true
            
        player = newPlayer
        return newPlayer
    }

    fun release() {
        player?.let {
            try {
                it.release()
            } catch (e: Exception) {
                android.util.Log.e("PlayerHolder", "Error releasing player", e)
            }
        }
        player = null
    }
}



