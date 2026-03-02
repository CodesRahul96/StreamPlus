package com.codesrahul.streamplus.ui.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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

        var streamUrl = intent.getStringExtra(EXTRA_STREAM_URL) ?: ""
        val streamTitle = intent.getStringExtra(EXTRA_STREAM_TITLE) ?: "Unknown Stream"
        val channelLogo = intent.getStringExtra(EXTRA_CHANNEL_LOGO)
        var userAgent = intent.getStringExtra(EXTRA_USER_AGENT) ?: "ExoPlayerLib/2.19.1"
        var referer = intent.getStringExtra(EXTRA_REFERER)
        var cookie = intent.getStringExtra(EXTRA_COOKIE)
        var origin = intent.getStringExtra(EXTRA_ORIGIN)
        @Suppress("UNCHECKED_CAST")
        val headers = intent.getSerializableExtra(EXTRA_HEADERS) as? Map<String, String> ?: emptyMap()
        var drmLicenseUrl = intent.getStringExtra(EXTRA_DRM_LICENSE_URL)
        var drmScheme = intent.getStringExtra(EXTRA_DRM_SCHEME)
        @Suppress("UNCHECKED_CAST")
        val drmHeaders = intent.getSerializableExtra(EXTRA_DRM_HEADERS) as? Map<String, String> ?: emptyMap()
        val playlistId = intent.getIntExtra(EXTRA_PLAYLIST_ID, -1)
        val groupTitle = intent.getStringExtra(EXTRA_STREAM_GROUP) ?: "Default"

        updatePipParams()

        lifecycleScope.launch {
            if (playlistId != -1 && streamTitle != "Unknown Stream") {
                withContext(Dispatchers.IO) {
                    val fresh = channelDao.getChannelByIdentity(streamTitle, groupTitle, playlistId)
                    if (fresh != null) {
                        streamUrl = fresh.url
                        userAgent = fresh.userAgent
                        referer = fresh.headers["Referer"] ?: referer
                        cookie = fresh.headers["Cookie"] ?: cookie
                        origin = fresh.headers["Origin"] ?: origin
                        drmLicenseUrl = fresh.licenseUrl ?: drmLicenseUrl
                        drmScheme = fresh.drmScheme ?: drmScheme
                    }
                }
            }

            // Update App Shortcuts with the current channel info
            com.codesrahul.streamplus.util.ShortcutHelper.updateShortcuts(
                context = this@PlayerActivity,
                streamTitle = streamTitle,
                streamUrl = streamUrl,
                channelLogo = channelLogo,
                playlistId = playlistId,
                groupTitle = groupTitle
            )

            setContent {
                key(streamUrl, streamTitle) {
                    StreamPlusTheme {
                        PlayerScreen(
                            streamUrl = streamUrl,
                            streamTitle = streamTitle,
                            channelLogo = channelLogo,
                            userAgent = userAgent,
                            referer = referer,
                            cookie = cookie,
                            origin = origin,
                            headers = headers,
                            drmLicenseUrl = drmLicenseUrl,
                            drmScheme = drmScheme,
                            drmHeaders = drmHeaders,
                            okHttpClientProvider = okHttpClientProvider,
                            settingsRepository = settingsRepository,
                            historyRepository = historyRepository,
                            playlistId = playlistId,
                            groupTitle = groupTitle,
                            castPlayer = castPlayer,
                            onEnterPip = { enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build()) },
                            isInPip = isInPip
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
        castPlayer?.release()
        super.onDestroy()
    }
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
    playlistId: Int,
    groupTitle: String,
    castPlayer: androidx.media3.cast.CastPlayer? = null,
    onEnterPip: () -> Unit,
    isInPip: Boolean = false
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
    val settingsState by settingsRepository.settings.collectAsState(initial = com.codesrahul.streamplus.data.settings.AppSettings())
    
    // Audio Boost State
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
    val sheetState = rememberModalBottomSheetState()

    // Resume Logic
    // Using a separate LaunchedEffect for resume so it doesn't re-trigger on other changes
    // We need to wait for player to be created, so we'll do this inside the player creation or a separate effect dependent on player
    
    // Gesture State
    var showGestureOverlay by remember { mutableStateOf(false) }
    var gestureIcon by remember { mutableStateOf<ImageVector?>(null) }
    var gestureText by remember { mutableStateOf("") }
    var gestureLevel by remember { mutableStateOf(0f) }
    
    // Seek Overlay State
    var showSeekOverlay by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }
    
    // Lock State
    var isLocked by remember { mutableStateOf(false) }
    var isControlsVisible by remember { mutableStateOf(true) }
    
    // Helper to dim overlays after delay
    LaunchedEffect(showGestureOverlay) {
        if (showGestureOverlay) {
            kotlinx.coroutines.delay(2000)
            showGestureOverlay = false
        }
    }
    
    LaunchedEffect(showSeekOverlay) {
        if (showSeekOverlay) {
            kotlinx.coroutines.delay(800)
            showSeekOverlay = false
        }
    }

    LaunchedEffect(isControlsVisible, isLocked) {
        if (isControlsVisible && !isLocked) {
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

    // Sync immersive mode with controls visibility
    LaunchedEffect(isControlsVisible) {
        if (!isControlsVisible) {
            activity?.let { hideSystemUI(it) }
        } else {
            activity?.let { showSystemUI(it) }
        }
    }

    // Combined Time State
    var currentTimeFormatted by remember { mutableStateOf("00:00") }
    var durationFormatted by remember { mutableStateOf("00:00") }

    LaunchedEffect(settingsState.forceLandscape) {
        activity?.requestedOrientation = if (settingsState.forceLandscape) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER
        }
        if (settingsState.forceLandscape) {
            activity?.let { hideSystemUI(it) }
        }
    }

    // 1. Resolve Item immediately based on Inputs
    val resolvedItem = remember(streamUrl, streamTitle, channelLogo, userAgent, referer, cookie, origin, drmLicenseUrl, drmScheme, headers, drmHeaders) {
        // Build the starting headers map from all individual intent extras
        val startHeaders = mutableMapOf<String, String>()
        startHeaders.putAll(headers)
        if (!referer.isNullOrBlank())  startHeaders["Referer"]    = referer
        if (!cookie.isNullOrBlank())   startHeaders["Cookie"]     = cookie
        if (!origin.isNullOrBlank())   startHeaders["Origin"]     = origin

        val item = M3UItem(
            name = streamTitle,
            url = streamUrl,
            logoUrl = channelLogo ?: "",
            group = "",
            userAgent = userAgent,  // preserved by the fixed HeaderResolver
            headers = startHeaders, // referer/cookie/origin now in headers from the start
            licenseUrl = drmLicenseUrl,
            drmScheme = drmScheme,
            drmHeaders = drmHeaders
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

    val exoPlayer = remember(resolvedItem, settingsState.skipSslVerification, settingsState.decoderPriority, settingsState.enableTunneling) {
        val okHttpClient = if (settingsState.skipSslVerification) {
            getUnsafeOkHttpClient()
        } else {
            okHttpClientProvider.get().newBuilder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient).apply {
            val finalHeaders = mutableMapOf<String, String>()
            // Use headers from resolved item
            finalHeaders.putAll(resolvedItem.headers) 
            if (resolvedItem.userAgent.isNotBlank()) {
                finalHeaders["User-Agent"] = resolvedItem.userAgent
                Log.d("PlayerScreen", "Setting User-Agent: ${resolvedItem.userAgent}")
            }
            // Inject Referer, Cookie, Origin from intent extras into the actual HTTP headers
            if (!referer.isNullOrBlank()) {
                finalHeaders["Referer"] = referer
                Log.d("PlayerScreen", "Setting Referer: $referer")
            }
            if (!cookie.isNullOrBlank()) {
                finalHeaders["Cookie"] = cookie
                Log.d("PlayerScreen", "Setting Cookie header")
            }
            if (!origin.isNullOrBlank()) {
                finalHeaders["Origin"] = origin
                Log.d("PlayerScreen", "Setting Origin: $origin")
            }
            
            Log.d("PlayerScreen", "Stream URL: ${resolvedItem.url}")
            
            setDefaultRequestProperties(finalHeaders)
        }
        
        val dataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)

        val drmSessionManagerProvider = object : DrmSessionManagerProvider {
            override fun get(mediaItem: MediaItem): DrmSessionManager {
                val dConf = mediaItem.localConfiguration?.drmConfiguration ?: return DrmSessionManager.DRM_UNSUPPORTED
                
                // Use headers from DRM configuration (which includes X-ClearKey-Data for embedded keys)
                val allHeaders = mutableMapOf<String, String>()
                allHeaders.putAll(resolvedItem.headers)
                allHeaders.putAll(resolvedItem.drmHeaders)
                // Add license request headers from DRM configuration (includes X-ClearKey-Data)
                allHeaders.putAll(dConf.licenseRequestHeaders)
                
                Log.d("PlayerScreen", "DRM Session Manager - Headers: ${allHeaders.keys}")
                
                val callback = UnifiedMediaDrmCallback(okHttpDataSourceFactory, okHttpClient, allHeaders)
                
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

        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setAdtsExtractorFlags(androidx.media3.extractor.ts.AdtsExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)

        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)
            .setDataSourceFactory(dataSourceFactory)
            .setDrmSessionManagerProvider(drmSessionManagerProvider)

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(when(settingsState.decoderPriority) {
                1 -> androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                2 -> androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                else -> androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
            })
            setEnableDecoderFallback(true)
            if (settingsState.enableTunneling) {
                setEnableAudioTrackPlaybackParams(true)
            }
        }

        val bufferMs = settingsState.networkBufferSize * 1000
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                bufferMs, // Min buffer
                (bufferMs * 2.5).toInt(), // Max buffer (e.g. 75s for 30s setting)
                2_500, // Buffer for playback start
                5_000  // Buffer for rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val preferredLang = settingsState.defaultAudioLanguage.takeIf { it != "Auto" }?.lowercase()

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            
        player.setTrackSelectionParameters(
            player.trackSelectionParameters.buildUpon()
                .setPreferredAudioLanguage(preferredLang)
                .build()
        )
            
        player.setPlayWhenReady(true)
        player.setSkipSilenceEnabled(settingsState.skipSilence)
        player.setRepeatMode(if (settingsState.repeatMode) androidx.media3.common.Player.REPEAT_MODE_ALL else androidx.media3.common.Player.REPEAT_MODE_OFF)
        
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("PlayerScreen", "Playback Error: ${error.message}", error)
                if (settingsState.silentSkipping) {
                    android.widget.Toast.makeText(context, "Playback error: ${error.errorCodeName}. Skipping...", android.widget.Toast.LENGTH_SHORT).show()
                    return
                }
                val msg = when (val cause = error.cause) {
                    is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException -> "Error: ${cause.responseCode} ${cause.message}"
                    is androidx.media3.datasource.HttpDataSource.HttpDataSourceException -> "Network Error: ${cause.message}"
                    else -> "Playback Error: ${error.errorCodeName}"
                }
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                activityContext?.setLastVideoRatio(videoSize.width, videoSize.height)
            }
        })
        
        player
    }

    // Ticker for Time and Progress
    LaunchedEffect(exoPlayer, isControlsVisible) {
        if (isControlsVisible) {
            while (true) {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                currentTimeFormatted = formatElapsedTime(pos)
                durationFormatted = if (dur == C.TIME_UNSET || dur <= 0) "00:00" else formatElapsedTime(dur)
                kotlinx.coroutines.delay(500)
            }
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

    DisposableEffect(resolvedItem, exoPlayer) {
        onDispose {
            // Save on exit
            val pos = try { exoPlayer.currentPosition } catch (e: Exception) { 0L }
            val dur = try { exoPlayer.duration } catch (e: Exception) { 0L }
            
            if (pos > 5000 && resolvedItem.url.isNotBlank()) {
                 // Use a dedicated scope that survives the composable disposal
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
                        Log.e("PlayerScreen", "Failed to save final progress", e)
                    }
                }
            }
            try { exoPlayer.release() } catch (e: Exception) {}
        }
    }

    LaunchedEffect(resolvedItem, exoPlayer) {
        if (resolvedItem.url.isNotBlank()) {
            val lowerUrl = resolvedItem.url.lowercase()
            val inferredMimeType = when {
                lowerUrl.contains(".m3u8") || lowerUrl.contains("type=hls") || lowerUrl.contains("/hls/") || lowerUrl.contains(".php?m3u=1") -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                lowerUrl.contains(".mpd") || lowerUrl.contains("type=dash") || lowerUrl.contains("/dash/") || lowerUrl.contains("manifest.mpd") || lowerUrl.contains("index.mpd") -> androidx.media3.common.MimeTypes.APPLICATION_MPD
                lowerUrl.contains(".ism") || lowerUrl.contains("/manifest") -> androidx.media3.common.MimeTypes.APPLICATION_SS
                else -> null
            }

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(Uri.parse(resolvedItem.url))
                .setMediaId(resolvedItem.url)
                .apply { inferredMimeType?.let { setMimeType(it) } }
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(resolvedItem.name)
                        .setArtworkUri(resolvedItem.logoUrl.takeIf { it.isNotBlank() }?.let { Uri.parse(it) })
                        .build()
                )

            // DRM CONFIGURATION
            val effectiveScheme = resolvedItem.drmScheme?.lowercase() ?: ""
            val effectiveLicenseUrl = resolvedItem.licenseUrl
            
            if (effectiveScheme.isNotBlank() || !effectiveLicenseUrl.isNullOrBlank() || resolvedItem.drmHeaders.isNotEmpty()) {
                val uuid = when {
                    effectiveScheme.contains("widevine") -> C.WIDEVINE_UUID
                    effectiveScheme.contains("playready") -> C.PLAYREADY_UUID
                    effectiveScheme.contains("clearkey") -> C.CLEARKEY_UUID
                    else -> {
                        // Inferred from MIME, URL or presence of DRM headers
                        when {
                            inferredMimeType == androidx.media3.common.MimeTypes.APPLICATION_MPD -> C.WIDEVINE_UUID
                            !effectiveLicenseUrl.isNullOrBlank() && (effectiveLicenseUrl.contains("proxy/lic/widevine") || effectiveLicenseUrl.contains("lic/wv")) -> C.WIDEVINE_UUID
                            resolvedItem.url.contains(".mpd") -> C.WIDEVINE_UUID
                            else -> null
                        }
                    }
                }

                if (uuid != null) {
                    Log.d("PlayerScreen", "DRM UUID detected: $uuid")
                    Log.d("PlayerScreen", "Effective Scheme: $effectiveScheme, Effective License URL: $effectiveLicenseUrl")
                    Log.d("PlayerScreen", "Resolved Item - DRM Scheme: ${resolvedItem.drmScheme}, License URL: ${resolvedItem.licenseUrl}")
                    
                    val drmBuilder = DrmConfiguration.Builder(uuid)
                    
                    if (!effectiveLicenseUrl.isNullOrBlank()) {
                        Log.d("PlayerScreen", "License URL is not blank: $effectiveLicenseUrl")
                        
                        // Handle ClearKey Key Pairs (KID:KEY)
                        if (uuid == C.CLEARKEY_UUID && effectiveLicenseUrl.contains(":") && !effectiveLicenseUrl.contains("://")) {
                            try {
                                Log.d("PlayerScreen", "Processing ClearKey hex pair: $effectiveLicenseUrl")
                                val pairs = effectiveLicenseUrl.split(",").map { it.trim() }
                                val keysJson = pairs.mapNotNull { pair ->
                                    val parts = pair.split(":")
                                    if (parts.size == 2) {
                                        val kid = parts[0].trim()
                                        val key = parts[1].trim()
                                        
                                        // Detect if Hex or Base64
                                        val kidBytes = if (kid.length == 32 && kid.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) hexToByteArray(kid) else Base64.decode(kid, Base64.DEFAULT)
                                        val keyBytes = if (key.length == 32 && key.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) hexToByteArray(key) else Base64.decode(key, Base64.DEFAULT)

                                        val kidB64 = Base64.encodeToString(kidBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                                        val keyB64 = Base64.encodeToString(keyBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                                        
                                        Log.d("PlayerScreen", "Converted to Base64 - KID: $kidB64, KEY: $keyB64")
                                        "{\"kty\":\"oct\",\"k\":\"$keyB64\",\"kid\":\"$kidB64\"}"
                                    } else null
                                }.joinToString(",")
                                
                                val ckJson = "{\"keys\":[$keysJson],\"type\":\"temporary\"}"
                                Log.d("PlayerScreen", "Generated ClearKey JSON: $ckJson")
                                
                                // Store the ClearKey JSON in a special header so UnifiedMediaDrmCallback can use it
                                val updatedDrmHeaders = resolvedItem.drmHeaders.toMutableMap()
                                updatedDrmHeaders["X-ClearKey-Data"] = ckJson
                                
                                // Create a data URI
                                val dUri = "data:application/json;base64," + Base64.encodeToString(ckJson.toByteArray(), Base64.NO_WRAP)
                                Log.d("PlayerScreen", "Final data URI: $dUri")
                                drmBuilder.setLicenseUri(Uri.parse(dUri))
                                drmBuilder.setLicenseRequestHeaders(updatedDrmHeaders)
                            } catch (e: Exception) { 
                                Log.e("PlayerScreen", "Failed to parse ClearKey string: $effectiveLicenseUrl", e)
                                drmBuilder.setLicenseUri(Uri.parse(effectiveLicenseUrl))
                                drmBuilder.setLicenseRequestHeaders(resolvedItem.drmHeaders)
                            }
                        } else {
                            Log.d("PlayerScreen", "Standard License URL configuration")
                            drmBuilder.setLicenseUri(Uri.parse(effectiveLicenseUrl))
                            drmBuilder.setLicenseRequestHeaders(resolvedItem.drmHeaders)
                        }
                    } else {
                        Log.d("PlayerScreen", "No License URL, using headers only")
                        drmBuilder.setLicenseRequestHeaders(resolvedItem.drmHeaders)
                    }

                    drmBuilder.setMultiSession(true)
                    val drmConfig = drmBuilder.build()
                    Log.d("PlayerScreen", "Built DRM Config - License URI: ${drmConfig.licenseUri}, Scheme: ${drmConfig.scheme}")
                    Log.d("PlayerScreen", "DRM Config Headers: ${drmConfig.licenseRequestHeaders}")
                    mediaItemBuilder.setDrmConfiguration(drmConfig)
                    
                    Log.d("PlayerScreen", "Applied DRM Config: UUID=$uuid, Scheme=$effectiveScheme, License=$effectiveLicenseUrl")
                }
            }
            exoPlayer.setMediaItem(mediaItemBuilder.build())
            exoPlayer.prepare()
        }
    }




    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { _ -> // Ignore the view creation context, use the captured activity context
                // Explicitly wrap the context with the theme to ensure MediaRouteButton works
                val contextThemeWrapper = android.view.ContextThemeWrapper(activityContext, com.codesrahul.streamplus.R.style.Theme_M3UValidator)
                val view = android.view.LayoutInflater.from(contextThemeWrapper).inflate(com.codesrahul.streamplus.R.layout.player_view_wrapper, null)
                val playerView = view.findViewById<androidx.media3.ui.PlayerView>(com.codesrahul.streamplus.R.id.player_view)
                playerView.player = exoPlayer
                playerView.keepScreenOn = true
                
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
                                    gestureText = "Brightness: ${(newVal * 100).toInt()}%"
                                    gestureIcon = Icons.Default.BrightnessMedium
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
                                    gestureText = "Volume: ${(target * 100 / max)}%"
                                    gestureIcon = if (target == 0) Icons.Default.VolumeOff else Icons.Default.VolumeUp
                                    gestureLevel = target.toFloat() / max
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
                    detector.onTouchEvent(event)
                    true 
                }
                view
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val playerView = view.findViewById<androidx.media3.ui.PlayerView>(com.codesrahul.streamplus.R.id.player_view)
                if (playerView.player != activePlayer) playerView.player = activePlayer
                
                // Bind the stream title to the custom TextView in the controller layout
                playerView.findViewById<android.widget.TextView>(com.codesrahul.streamplus.R.id.tv_player_title)?.text = streamTitle
                
                // Combined Time Update
                playerView.findViewById<android.widget.TextView>(com.codesrahul.streamplus.R.id.tv_time_combined)?.text = 
                    "$currentTimeFormatted / $durationFormatted"

                playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.btn_close)?.setOnClickListener { activityContext?.finish() }
                
                playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.exo_speed_top)?.setOnClickListener { 
                    val speeds = arrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    android.app.AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                        .setTitle("Select Speed")
                        .setItems(speeds.map { "${it}x" }.toTypedArray()) { _, idx ->
                            playbackSpeed = speeds[idx]
                        }.show()
                }

                playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.exo_playlist_top)?.setOnClickListener { 
                    android.widget.Toast.makeText(context, "Channel List coming soon", android.widget.Toast.LENGTH_SHORT).show()
                }

                playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.exo_settings)?.setOnClickListener { 
                    showSettingsSheet = true
                }
                
                
                try {
                    val storedMode = settingsState.lastResizeMode
                    if (storedMode in 0..4) playerView.resizeMode = storedMode
                } catch (e: Exception) {}
                playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.exo_resize_mode_button)?.setOnClickListener {
                    playerView.resizeMode = when (playerView.resizeMode) {
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    val msg = when(playerView.resizeMode) {
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Fill"
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
                        else -> "Fit"
                    }
                    android.widget.Toast.makeText(playerView.context, "Resize Mode: $msg", android.widget.Toast.LENGTH_SHORT).show()
                    val activity = playerView.context as? ComponentActivity
                    activity?.lifecycleScope?.launch { settingsRepository.setLastResizeMode(playerView.resizeMode) }
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
                
                // Hide controls in PiP
                playerView.useController = !isInPip
                if (isInPip) playerView.hideController()
            }
        )

        // Robust Lock Mode Overlay
        AnimatedVisibility(
            visible = isLocked && !isInPip,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                onClick = { 
                    isLocked = false
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

        // Small Lock Toggle Overlay (Only visible when controls are visible)
        AnimatedVisibility(
            visible = isControlsVisible && !isLocked && !isInPip,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp)
        ) {
            Surface(
                onClick = { 
                    isLocked = true
                    isControlsVisible = false
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    android.widget.Toast.makeText(context, "Controls Locked", android.widget.Toast.LENGTH_SHORT).show()
                },
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Lock",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Seek Animation Overlay
        AnimatedVisibility(
            visible = showSeekOverlay && !isInPip,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
            modifier = Modifier.align(if (seekDirection == "Rewind") Alignment.CenterStart else Alignment.CenterEnd)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(topStart = if (seekDirection == "Rewind") 0.dp else 40.dp, bottomStart = if (seekDirection == "Rewind") 0.dp else 40.dp, topEnd = if (seekDirection == "Rewind") 40.dp else 0.dp, bottomEnd = if (seekDirection == "Rewind") 40.dp else 0.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.padding(vertical = 48.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (seekDirection == "Rewind") Icons.Default.FastRewind else Icons.Default.FastForward,
                        contentDescription = seekDirection,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (seekDirection == "Rewind") "RETRY -10s" else "SKIP +10s",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

    // Indication Overlay (Gestures)
    AnimatedVisibility(
        visible = showGestureOverlay && !isInPip,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = Modifier.align(Alignment.Center)
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 40.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                gestureIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = gestureText,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(20.dp))
                // Glassy Progress Bar
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(6.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(gestureLevel)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF64B5F6), Color.White)
                                ),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
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
                    showTrackSelectionDialog(context, exoPlayer, C.TRACK_TYPE_VIDEO)
                }
                SettingsItem(Icons.Default.AudioFile, "Audio Tracks") {
                    showSettingsSheet = false
                    showTrackSelectionDialog(context, exoPlayer, C.TRACK_TYPE_AUDIO)
                }
                SettingsItem(Icons.Default.Subtitles, "Subtitles") {
                    showSettingsSheet = false
                    showTrackSelectionDialog(context, exoPlayer, C.TRACK_TYPE_TEXT)
                }
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))

                // Advanced Controls
                SettingsItem(Icons.Default.Speed, "Playback Speed (${playbackSpeed}x)") {
                    val speeds = arrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    android.app.AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                        .setTitle("Select Speed")
                        .setItems(speeds.map { "${it}x" }.toTypedArray()) { _, idx ->
                            playbackSpeed = speeds[idx]
                            showSettingsSheet = false
                        }.show()
                }

                SettingsSliderItem(Icons.Default.VolumeUp, "Audio Boost", audioBoost) {
                    audioBoost = it
                }

                SettingsItem(Icons.Default.Timer, "Sleep Timer ${if (sleepTimerActive) "(Active)" else ""}") {
                    val times = arrayOf(5, 15, 30, 60, 0)
                    android.app.AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                        .setTitle("Set Sleep Timer")
                        .setItems(times.map { if (it == 0) "Off" else "$it Minutes" }.toTypedArray()) { _, idx ->
                            val mins = times[idx]
                            if (mins == 0) {
                                sleepTimerActive = false
                            } else {
                                sleepTimerMillis = mins * 60 * 1000L
                                sleepTimerActive = true
                            }
                            showSettingsSheet = false
                        }.show()
                }

                SettingsItem(Icons.Default.FileOpen, "Load External Subtitle") {
                    showSettingsSheet = false
                    subtitlePickerLauncher?.invoke()
                }
            }
        }
    }
    }
}

private fun getUnsafeOkHttpClient(): OkHttpClient {
    return try {
        val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
            object : javax.net.ssl.X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            }
        )
        val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}

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
                    val lang = format.language?.uppercase() ?: "Unknown"
                    val labelText = format.label ?: "Audio"
                    "$labelText [$lang]"
                }
                C.TRACK_TYPE_TEXT -> {
                    val lang = format.language?.uppercase() ?: "Unknown"
                    val labelText = format.label ?: "Subtitle"
                    "$labelText [$lang]"
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
        modifier = Modifier.fillMaxWidth()
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
