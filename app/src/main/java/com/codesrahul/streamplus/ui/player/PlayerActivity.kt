package com.codesrahul.streamplus.ui.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.codesrahul.streamplus.ui.theme.StreamPlusTheme
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import java.util.UUID
import javax.inject.Inject

import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.flow.first

@UnstableApi
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    @Inject
    lateinit var okHttpClientProvider: javax.inject.Provider<OkHttpClient>

    @Inject
    lateinit var settingsRepository: com.codesrahul.streamplus.data.settings.SettingsRepository

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Orientation is handled in specific screens via LaunchedEffect or similar
        // val isForceLandscape = kotlinx.coroutines.runBlocking { settingsRepository.settings.first().forceLandscape }
        // if (isForceLandscape) {
        //    requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        //    hideSystemUI(this)
        // }

        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL) ?: ""
        val streamTitle = intent.getStringExtra(EXTRA_STREAM_TITLE) ?: "Unknown Stream"
        val channelLogo = intent.getStringExtra(EXTRA_CHANNEL_LOGO)
        val userAgent = intent.getStringExtra(EXTRA_USER_AGENT) ?: "ExoPlayerLib/2.19.1"
        val referer = intent.getStringExtra(EXTRA_REFERER)
        val cookie = intent.getStringExtra(EXTRA_COOKIE)
        val origin = intent.getStringExtra(EXTRA_ORIGIN)
        @Suppress("UNCHECKED_CAST")
        val headers = intent.getSerializableExtra(EXTRA_HEADERS) as? Map<String, String> ?: emptyMap()
        val drmLicenseUrl = intent.getStringExtra(EXTRA_DRM_LICENSE_URL)
        val drmScheme = intent.getStringExtra(EXTRA_DRM_SCHEME)
        @Suppress("UNCHECKED_CAST")
        val drmHeaders = intent.getSerializableExtra(EXTRA_DRM_HEADERS) as? Map<String, String> ?: emptyMap()

        setContent {
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
                    onEnterPip = { enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build()) }
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val autoPip = kotlinx.coroutines.runBlocking { settingsRepository.settings.first().autoPip }
        if (autoPip) {
            enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
        }
    }
}

private fun hexToByteArray(hex: String): ByteArray {
    val s = hex.replace(" ", "")
    val len = s.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
        i += 2
    }
    return data
}

@OptIn(UnstableApi::class)
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
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    val settingsState by settingsRepository.settings.collectAsState(initial = com.codesrahul.streamplus.data.settings.AppSettings())
    
    // Gesture State
    var showGestureOverlay by remember { mutableStateOf(false) }
    var gestureIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }
    var gestureText by remember { mutableStateOf("") }
    
    // Lock State
    var isLocked by remember { mutableStateOf(false) }
    
    // Helper to dim overlay after delay
    LaunchedEffect(showGestureOverlay) {
        if (showGestureOverlay) {
            kotlinx.coroutines.delay(2000)
            showGestureOverlay = false
        }
    }

    // Handle Orientation based on settings
    val activity = context as? android.app.Activity
    LaunchedEffect(settingsState.forceLandscape) {
        if (settingsState.forceLandscape) {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            activity?.let { hideSystemUI(it) }
        } else {
             // Optional: reset to unspecified or sensor, or leave as is
             // activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val exoPlayer = remember(settingsState) {
        // Handle SSL Verification
        val okHttpClient = if (settingsState.skipSslVerification) {
            getUnsafeOkHttpClient()
        } else {
            okHttpClientProvider.get().newBuilder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient).apply {
            val lowerUrl = streamUrl.lowercase()
            val finalHeaders = mutableMapOf<String, String>()
            finalHeaders.putAll(headers)
            
            // Explicit parameters override map headers if set
            if (userAgent.isNotBlank()) finalHeaders["User-Agent"] = userAgent
            referer?.let { if (it.isNotBlank()) finalHeaders["Referer"] = it }
            cookie?.let { if (it.isNotBlank()) finalHeaders["Cookie"] = it }
            origin?.let { if (it.isNotBlank()) finalHeaders["Origin"] = it }
            
            // Professional Logic: Auto-apply headers for known domains
            if (lowerUrl.contains("sony") || lowerUrl.contains("zee5") || lowerUrl.contains("hotstar") || lowerUrl.contains("star")) {
                if (finalHeaders["User-Agent"]?.contains("ExoPlayer") == true || finalHeaders["User-Agent"].isNullOrBlank()) {
                     finalHeaders["User-Agent"] = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }
                if (finalHeaders["Origin"].isNullOrBlank()) {
                     if (lowerUrl.contains("sony")) finalHeaders["Origin"] = "https://www.sonyliv.com"
                     else if (lowerUrl.contains("zee5")) finalHeaders["Origin"] = "https://www.zee5.com"
                     else if (lowerUrl.contains("star") || lowerUrl.contains("hotstar")) finalHeaders["Origin"] = "https://www.hotstar.com"
                }
            }

            // CRITICAL: Normalize Hotstar/Star Origin (no trailing slash)
            val currentOrigin = finalHeaders["Origin"]
            if (currentOrigin != null && currentOrigin.endsWith("/")) {
                finalHeaders["Origin"] = currentOrigin.trimEnd('/')
            }
            
            if (!finalHeaders.containsKey("Accept")) finalHeaders["Accept"] = "*/*"
            if (!finalHeaders.containsKey("Connection")) finalHeaders["Connection"] = "keep-alive"
            
            setDefaultRequestProperties(finalHeaders)
        }
        
        val dataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)

        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setAdtsExtractorFlags(androidx.media3.extractor.ts.AdtsExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(when(settingsState.decoderPriority) {
                1 -> androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                2 -> androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                else -> androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
            })
             // setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            setEnableDecoderFallback(true)
            if (settingsState.enableTunneling) {
                setEnableAudioTrackPlaybackParams(true)
            }
        }

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30000, // Min buffer 30s
                60000, // Max buffer 60s
                2500,  // Buffer for playback 2.5s
                5000   // Buffer for playback after rebuffer 5s
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val bandwidthMeter = androidx.media3.exoplayer.upstream.DefaultBandwidthMeter.Builder(context).build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory))
            .setLoadControl(loadControl)
            .setBandwidthMeter(bandwidthMeter)
            .build().apply {
                playWhenReady = true

                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("PlayerScreen", "Playback Error: ${error.message}", error)
                        if (settingsState.silentSkipping) {
                            android.widget.Toast.makeText(context, "Playback error: ${error.errorCodeName}. Skipping...", android.widget.Toast.LENGTH_SHORT).show()
                            return
                        }
                        val cause = error.cause
                        val message = when {
                            cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException -> {
                                "Error: ${cause.responseCode} ${cause.message}"
                            }
                            cause is androidx.media3.datasource.HttpDataSource.HttpDataSourceException -> {
                                "Network Error: ${cause.message}"
                            }
                            else -> "Playback Error: ${error.errorCodeName}"
                        }
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    }
                })
            }
    }

    LaunchedEffect(streamUrl) {
        if (streamUrl.isNotBlank()) {
            val uri = Uri.parse(streamUrl)
            val lowerUrl = streamUrl.lowercase()
            val inferredMimeType = when {
                lowerUrl.contains(".m3u8") || lowerUrl.contains("type=hls") || lowerUrl.contains("/hls/") || lowerUrl.contains(".php?m3u=1") -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                lowerUrl.contains(".mpd") || lowerUrl.contains("type=dash") || lowerUrl.contains("/dash/") || lowerUrl.contains("manifest.mpd") || lowerUrl.contains("index.mpd") -> androidx.media3.common.MimeTypes.APPLICATION_MPD
                lowerUrl.contains(".ism") || lowerUrl.contains("/manifest") -> androidx.media3.common.MimeTypes.APPLICATION_SS
                else -> null
            }

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(uri)
                .setMediaId(streamUrl)
                .apply {
                    inferredMimeType?.let { setMimeType(it) }
                }
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(streamTitle)
                        .setArtworkUri(channelLogo?.let { Uri.parse(it) })
                        .build()
                )

            if (!drmLicenseUrl.isNullOrBlank()) {
                 val scheme = drmScheme?.lowercase() ?: ""
                 val uuid = when {
                    scheme.contains("widevine") -> C.WIDEVINE_UUID
                    scheme.contains("playready") -> C.PLAYREADY_UUID
                    scheme.contains("clearkey") -> C.CLEARKEY_UUID
                    else -> C.WIDEVINE_UUID
                }
                
                val drmBuilder = DrmConfiguration.Builder(uuid)
                
                // (Detailed ClearKey logic omitted for brevity but preserved in principle if copied fully)
                // Assuming standard DRM setup for now based on original file...
                // Re-implementing the ClearKey logic briefly:
                if (uuid == C.CLEARKEY_UUID && drmLicenseUrl.contains(":") && !drmLicenseUrl.startsWith("http")) {
                    try {
                         val parts = drmLicenseUrl.split(":")
                         if (parts.size == 2) {
                             val kidHex = parts[0].trim()
                             val keyHex = parts[1].trim()
                             val kidB64 = Base64.encodeToString(hexToByteArray(kidHex), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                             val keyB64 = Base64.encodeToString(hexToByteArray(keyHex), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                             val clearKeyJson = """{"keys":[{"kty":"oct","k":"$keyB64","kid":"$kidB64"}],"type":"temporary"}"""
                             val dataUri = "data:application/json;base64," + Base64.encodeToString(clearKeyJson.toByteArray(), Base64.NO_WRAP)
                             drmBuilder.setLicenseUri(Uri.parse(dataUri))
                         } else { drmBuilder.setLicenseUri(Uri.parse(drmLicenseUrl)) }
                    } catch (e: Exception) { drmBuilder.setLicenseUri(Uri.parse(drmLicenseUrl)) }
                } else {
                    drmBuilder.setLicenseUri(Uri.parse(drmLicenseUrl))
                }
                
                val finalDrmHeaders = mutableMapOf<String, String>()
                finalDrmHeaders.putAll(headers) // Stream headers serve as baseline
                finalDrmHeaders.putAll(drmHeaders) // Explicit DRM headers override
                
                // Re-apply explicit overrides and auto-logic
                if (userAgent.isNotBlank()) finalDrmHeaders["User-Agent"] = userAgent
                referer?.let { if (it.isNotBlank()) finalDrmHeaders["Referer"] = it }
                cookie?.let { if (it.isNotBlank()) finalDrmHeaders["Cookie"] = it }
                origin?.let { if (it.isNotBlank()) finalDrmHeaders["Origin"] = it }
                
                val lowerUrl = streamUrl.lowercase()
                if (lowerUrl.contains("sony") || lowerUrl.contains("zee5") || lowerUrl.contains("hotstar") || lowerUrl.contains("star")) {
                    if (finalDrmHeaders["User-Agent"]?.contains("ExoPlayer") == true || finalDrmHeaders["User-Agent"].isNullOrBlank()) {
                         finalDrmHeaders["User-Agent"] = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }
                    if (finalDrmHeaders["Origin"].isNullOrBlank()) {
                         if (lowerUrl.contains("sony")) finalDrmHeaders["Origin"] = "https://www.sonyliv.com"
                         else if (lowerUrl.contains("zee5")) finalDrmHeaders["Origin"] = "https://www.zee5.com"
                         else if (lowerUrl.contains("star") || lowerUrl.contains("hotstar")) finalDrmHeaders["Origin"] = "https://www.hotstar.com"
                    }
                }

                // CRITICAL: Normalize Hotstar/Star Origin (no trailing slash)
                val currentOrigin = finalDrmHeaders["Origin"]
                if (currentOrigin != null && currentOrigin.endsWith("/")) {
                    finalDrmHeaders["Origin"] = currentOrigin.trimEnd('/')
                }

                Log.d("PlayerScreen", "DRM Config: Scheme=$scheme, LicenseUrl=$drmLicenseUrl, Headers=$finalDrmHeaders")
                
                drmBuilder.setLicenseRequestHeaders(finalDrmHeaders)
                mediaItemBuilder.setDrmConfiguration(drmBuilder.build())
            }

            exoPlayer.setMediaItem(mediaItemBuilder.build())
            exoPlayer.prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val view = android.view.LayoutInflater.from(ctx).inflate(com.codesrahul.streamplus.R.layout.player_view_wrapper, null)
                val playerView = view.findViewById<androidx.media3.ui.PlayerView>(com.codesrahul.streamplus.R.id.player_view)
                playerView.player = exoPlayer
                playerView.keepScreenOn = true
                
                // Add Gesture Listener
                val detector = android.view.GestureDetector(ctx, object : android.view.GestureDetector.SimpleOnGestureListener() {
                    val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                    val window = (ctx as? android.app.Activity)?.window

                    override fun onScroll(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                        if (e1 == null) return false
                        val width = playerView.width
                        val isLeft = e1.x < width / 2
                        
                        // Scale sensitivity
                        val delta = distanceY / playerView.height
                        
                        if (isLeft && settingsState.brightnessGestures) {
                            // Brightness
                            window?.let { win ->
                                val lp = win.attributes
                                var current = lp.screenBrightness
                                if (current < 0) current = 0.5f // system default
                                var newBrightness = current + delta
                                newBrightness = newBrightness.coerceIn(0f, 1f)
                                lp.screenBrightness = newBrightness
                                win.attributes = lp
                                
                                gestureText = "Brightness: ${(newBrightness * 100).toInt()}%"
                                gestureIcon = androidx.compose.material.icons.Icons.Default.BrightnessMedium
                                showGestureOverlay = true
                            }
                        } else if (!isLeft && settingsState.volumeGestures) {
                            // Volume
                            val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                            val current = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                            // delta is usually small, accumulate or use a larger multiplier
                            // let's say full swipe = full volume change
                            val change = (delta * max * 3).toInt() // multiplier for sensitivity
                            
                            if (change != 0 || kotlin.math.abs(distanceY) > 10) {
                                val target = (current + change).coerceIn(0, max)
                                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, target, 0)
                                gestureText = "Volume: ${(target * 100 / max)}%"
                                gestureIcon = if (target == 0) androidx.compose.material.icons.Icons.Default.VolumeOff else androidx.compose.material.icons.Icons.Default.VolumeUp
                                showGestureOverlay = true
                            }
                        }
                        return true
                    }

                    override fun onSingleTapConfirmed(e: android.view.MotionEvent): Boolean {
                        playerView.showController()
                         if (playerView.isControllerFullyVisible) {
                            playerView.hideController()
                        } else {
                            playerView.showController()
                        }
                        return true
                    }
                })

                playerView.setOnTouchListener { _, event ->
                    if (isLocked) {
                        detector.onTouchEvent(event) // Still allow tap to show "locked" msg if we want
                        true // consume all
                    } else {
                        detector.onTouchEvent(event)
                        false
                    }
                }

                view
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val playerView = view.findViewById<androidx.media3.ui.PlayerView>(com.codesrahul.streamplus.R.id.player_view)
                val titleView = playerView.findViewById<android.widget.TextView>(com.codesrahul.streamplus.R.id.tv_player_title)
                titleView?.text = streamTitle
                
                val closeButton = playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.btn_close)
                closeButton?.setOnClickListener { (playerView.context as? android.app.Activity)?.finish() }

                val settingsButton = playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.exo_settings)
                settingsButton?.setOnClickListener { showSettingsDialog(playerView.context, exoPlayer) }

                val resizeButton = playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.exo_resize_mode_button)
                // Apply stored resize mode
                try {
                    val storedMode = settingsState.lastResizeMode
                    if (storedMode in 0..2) playerView.resizeMode = storedMode
                } catch (e: Exception) { /* ignore */ }

                resizeButton?.setOnClickListener {
                    val currentMode = playerView.resizeMode
                    playerView.resizeMode = when (currentMode) {
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    val modeName = when(playerView.resizeMode) {
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Fill"
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
                        else -> "Fit"
                    }
                    android.widget.Toast.makeText(playerView.context, "Resize Mode: $modeName", android.widget.Toast.LENGTH_SHORT).show()

                    // persist the resize mode
                    val activity = playerView.context as? ComponentActivity
                    activity?.lifecycleScope?.launchWhenStarted {
                        settingsRepository.setLastResizeMode(playerView.resizeMode)
                    }
                }

                val landscapeButton = playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.btn_landscape)
                landscapeButton?.setOnClickListener {
                    val activity = playerView.context as? android.app.Activity
                    if (activity != null) {
                        val currentOrientation = activity.requestedOrientation
                        if (currentOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            // Show system UI when exiting fullscreen/landscape
                            showSystemUI(activity)
                        } else {
                            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            // Hide system UI when entering fullscreen/landscape
                            hideSystemUI(activity)
                        }
                    }
                }
                
                val pipButton = playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.btn_pip)
                pipButton?.setOnClickListener { onEnterPip() }

                val lockButton = playerView.findViewById<android.view.View>(com.codesrahul.streamplus.R.id.btn_lock)
                lockButton?.setOnClickListener {
                    isLocked = !isLocked
                    if (isLocked) {
                        playerView.hideController()
                        android.widget.Toast.makeText(context, "Controls Locked", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "Controls Unlocked", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                
                 val logoView = playerView.findViewById<android.widget.ImageView>(com.codesrahul.streamplus.R.id.iv_channel_logo)
                if (logoView != null && !channelLogo.isNullOrBlank()) {
                    val request = ImageRequest.Builder(playerView.context).data(channelLogo).target(logoView).build()
                    playerView.context.imageLoader.enqueue(request)
                }
            }
        )

        // Indication Overlay
        if (showGestureOverlay) {
             androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(), 
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        gestureIcon?.let {
                             androidx.compose.material3.Icon(
                                imageVector = it, 
                                contentDescription = null, 
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = gestureText,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// Unsafe OkHttpClient
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

private fun showSettingsDialog(context: Context, player: ExoPlayer) {
    val items = arrayOf("Audio Tracks", "Subtitles")
    android.app.AlertDialog.Builder(context)
        .setTitle("Settings")
        .setItems(items) { _, which ->
            when (which) {
                0 -> showTrackSelectionDialog(context, player, C.TRACK_TYPE_AUDIO)
                1 -> showTrackSelectionDialog(context, player, C.TRACK_TYPE_TEXT)
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
            val label = format.label ?: format.language ?: "Track ${options.size + 1}"
            options.add(label)
            trackSpecs.add(group to i)
        }
    }

    android.app.AlertDialog.Builder(context)
        .setTitle(if (trackType == C.TRACK_TYPE_AUDIO) "Select Audio" else "Select Subtitles")
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
        // ignore on older devices
    }
}

private fun showSystemUI(activity: android.app.Activity) {
    try {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    } catch (e: Exception) {
        // ignore on older devices
    }
}
