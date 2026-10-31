package com.codesrahul.streamplus.ui.player

import android.os.Bundle
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.codesrahul.streamplus.ui.theme.StreamPlusTheme
import com.codesrahul.streamplus.ui.theme.ElectricIndigo
import com.codesrahul.streamplus.ui.theme.NeonCyan
import com.codesrahul.streamplus.ui.theme.ObsidianSlate
import com.codesrahul.streamplus.ui.theme.CyberPurple
import com.codesrahul.streamplus.ui.theme.EmeraldGlow
import androidx.compose.foundation.BorderStroke
import com.codesrahul.streamplus.ui.util.glassmorphic
import com.codesrahul.streamplus.ui.util.tvFocusable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.codesrahul.streamplus.ui.player.lyrics.LyricParser
import com.codesrahul.streamplus.ui.player.lyrics.LyricsData
import com.codesrahul.streamplus.ui.player.lyrics.LyricsView

@UnstableApi
@AndroidEntryPoint
class MusicPlayerActivity : AppCompatActivity() {

    private var mediaController: androidx.media3.session.MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
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

        // Read intent extras
        val fromNotification = intent.getBooleanExtra("EXTRA_FROM_NOTIFICATION", false)
        val playlistUris = intent.getStringArrayExtra("EXTRA_PLAYLIST_URIS") ?: emptyArray()
        val playlistTitles = intent.getStringArrayExtra("EXTRA_PLAYLIST_TITLES") ?: emptyArray()
        val playlistArtists = intent.getStringArrayExtra("EXTRA_PLAYLIST_ARTISTS") ?: emptyArray()
        val playlistAlbums = intent.getStringArrayExtra("EXTRA_PLAYLIST_ALBUMS") ?: emptyArray()
        val hasExplicitIndex = intent.hasExtra("EXTRA_PLAYLIST_INDEX")
        val playlistIndex = intent.getIntExtra("EXTRA_PLAYLIST_INDEX", 0)

        Log.d("MusicPlayerActivity", "Initializing music player: fromNotification=$fromNotification, size=${playlistUris.size}, index=$playlistIndex")

        val sessionToken = androidx.media3.session.SessionToken(
            this,
            android.content.ComponentName(this, MusicPlaybackService::class.java)
        )
        val future = androidx.media3.session.MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture = future

        val isControllerReady = mutableStateOf(false)

        future.addListener({
            try {
                val controller = future.get()
                mediaController = controller

                if (controller.mediaItemCount == 0 && playlistUris.isNotEmpty()) {
                    val mediaItems = playlistUris.mapIndexed { idx, url ->
                        val title = playlistTitles.getOrNull(idx) ?: "Track ${idx + 1}"
                        val artist = playlistArtists.getOrNull(idx) ?: "Unknown Artist"
                        val album = playlistAlbums.getOrNull(idx) ?: "Unknown Album"
                        MediaItem.Builder()
                            .setUri(Uri.parse(url))
                            .setMediaId(url)
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(title)
                                    .setArtist(artist)
                                    .setAlbumTitle(album)
                                    .build()
                            )
                            .build()
                    }
                    controller.setMediaItems(mediaItems)
                    if (playlistIndex >= 0 && playlistIndex < mediaItems.size) {
                        controller.seekTo(playlistIndex, 0L)
                    }
                    controller.prepare()
                    controller.playWhenReady = true
                } else if (!fromNotification && hasExplicitIndex && playlistIndex >= 0 && playlistIndex < controller.mediaItemCount) {
                    // If already loaded but user explicitly selected a different track from local list
                    controller.seekTo(playlistIndex, 0L)
                    controller.playWhenReady = true
                }
                isControllerReady.value = true
            } catch (e: Exception) {
                Log.e("MusicPlayerActivity", "Failed to get MediaController", e)
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(this))

        setContent {
            StreamPlusTheme {
                if (isControllerReady.value) {
                    mediaController?.let { controller ->
                        MusicPlayerScreen(
                            player = controller,
                            onClose = { finish() }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF14131B)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaController?.let { controller ->
            if (!controller.isPlaying) {
                val serviceIntent = android.content.Intent(this, MusicPlaybackService::class.java)
                stopService(serviceIntent)
            }
        }
        controllerFuture?.let { future ->
            androidx.media3.session.MediaController.releaseFuture(future)
        }
        mediaController = null
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val fromNotification = intent.getBooleanExtra("EXTRA_FROM_NOTIFICATION", false)
        val hasExplicitIndex = intent.hasExtra("EXTRA_PLAYLIST_INDEX")
        val playlistIndex = intent.getIntExtra("EXTRA_PLAYLIST_INDEX", -1)
        if (!fromNotification && hasExplicitIndex && playlistIndex >= 0 && playlistIndex < (mediaController?.mediaItemCount ?: 0)) {
            mediaController?.seekTo(playlistIndex, 0L)
            mediaController?.playWhenReady = true
        }
    }

    override fun finish() {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    player: Player,
    onClose: () -> Unit
) {
    var playingTitle by remember { mutableStateOf("Unknown Track") }
    var playingArtist by remember { mutableStateOf("Unknown Artist") }
    var playingAlbum by remember { mutableStateOf("Unknown Album") }
    var currentTrackIndex by remember { mutableIntStateOf(player.currentMediaItemIndex) }
    
    var isPlaying by remember { mutableStateOf(false) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    
    var currentTimeFormatted by remember { mutableStateOf("00:00") }
    var durationFormatted by remember { mutableStateOf("00:00") }
    
    var isShuffleEnabled by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }
    
    var showQueue by remember { mutableStateOf(false) }
    var sleepTimerRemainingSec by remember { mutableIntStateOf(0) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var showLyrics by remember { mutableStateOf(false) }
    var lyricsData by remember { mutableStateOf<LyricsData?>(null) }

    fun loadLyricsForMedia(item: MediaItem?) {
        val uri = item?.localConfiguration?.uri
        if (uri != null) {
            val companion = LyricParser.findCompanionLrcFile(context, uri)
            if (companion != null) {
                lyricsData = LyricParser.parseLrc(companion)
                return
            }
        }
        val intentExtra = (context as? android.app.Activity)?.intent?.getStringExtra("EXTRA_LYRICS")
        if (!intentExtra.isNullOrBlank()) {
            lyricsData = LyricParser.parseLrc(intentExtra)
        } else {
            lyricsData = null
        }
    }

    LaunchedEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                duration = player.duration.coerceAtLeast(1L)
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                duration = player.duration.coerceAtLeast(1L)
                currentTrackIndex = player.currentMediaItemIndex
                mediaItem?.mediaMetadata?.title?.toString()?.let { title ->
                    playingTitle = title
                }
                mediaItem?.mediaMetadata?.artist?.toString()?.let { artist ->
                    playingArtist = artist
                }
                mediaItem?.mediaMetadata?.albumTitle?.toString()?.let { album ->
                    playingAlbum = album
                }
                loadLyricsForMedia(mediaItem)
            }
            override fun onMetadata(metadata: androidx.media3.common.Metadata) {
                if (lyricsData == null) {
                    val extracted = LyricParser.extractLyricsFromMetadata(metadata)
                    if (!extracted.isNullOrBlank()) {
                        lyricsData = LyricParser.parseLrc(extracted)
                    }
                }
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                isShuffleEnabled = shuffleModeEnabled
            }
            override fun onRepeatModeChanged(rm: Int) {
                repeatMode = rm
            }
        }
        player.addListener(listener)
        isPlaying = player.isPlaying
        duration = player.duration.coerceAtLeast(1L)
        currentTrackIndex = player.currentMediaItemIndex
        isShuffleEnabled = player.shuffleModeEnabled
        repeatMode = player.repeatMode
        
        player.currentMediaItem?.mediaMetadata?.title?.toString()?.let { title ->
            playingTitle = title
        }
        player.currentMediaItem?.mediaMetadata?.artist?.toString()?.let { artist ->
            playingArtist = artist
        }
        player.currentMediaItem?.mediaMetadata?.albumTitle?.toString()?.let { album ->
            playingAlbum = album
        }
        loadLyricsForMedia(player.currentMediaItem)

        try {
            while (true) {
                val pos = player.currentPosition
                val dur = player.duration
                currentPos = pos
                duration = dur.coerceAtLeast(1L)
                currentTimeFormatted = formatMusicTime(pos)
                durationFormatted = if (dur == C.TIME_UNSET || dur <= 0) "00:00" else formatMusicTime(dur)
                delay(250)
            }
        } finally {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(sleepTimerRemainingSec) {
        if (sleepTimerRemainingSec > 0) {
            delay(1000)
            sleepTimerRemainingSec -= 1
            if (sleepTimerRemainingSec == 0) {
                player.pause()
            }
        }
    }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderProgress by remember { mutableFloatStateOf(0f) }
    
    val currentProgress = if (isDraggingSlider) sliderProgress else (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val effectiveTopPadding = if (statusBarHeight < 28.dp) 48.dp else (statusBarHeight + 14.dp)
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val effectiveBottomPadding = if (navBarHeight < 16.dp) 24.dp else (navBarHeight + 8.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF14131B), Color(0xFF07070A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = effectiveTopPadding, bottom = effectiveBottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.tvFocusable(cornerRadius = 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = "NOW PLAYING",
                    color = Color.White.copy(0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Lyrics Toggle Button
                    IconButton(
                        onClick = { showLyrics = !showLyrics },
                        modifier = Modifier.tvFocusable(cornerRadius = 20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (showLyrics) NeonCyan else Color.White.copy(alpha = if (lyricsData != null) 0.95f else 0.5f),
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    if (sleepTimerRemainingSec > 0) {
                        Text(
                            text = formatSleepTime(sleepTimerRemainingSec),
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.tvFocusable(cornerRadius = 20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerRemainingSec > 0) Color(0xFF00E5FF) else Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    IconButton(
                        onClick = { showQueue = !showQueue },
                        modifier = Modifier.tvFocusable(cornerRadius = 20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = if (showQueue) Color(0xFF00E5FF) else Color.White,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
            }

            var selectedSoundProfile by remember { mutableStateOf("Studio") }

            // Atmospheric Pulsing Glow behind Vinyl Disc
            val pulseTransition = rememberInfiniteTransition(label = "Vinyl pulse")
            val pulseScale by pulseTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )
            val glowAlpha by pulseTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.55f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow_alpha"
            )

            // Center Content: Vinyl Disc OR Synced Lyrics (Weighted)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (showLyrics) {
                    LyricsView(
                        lyricsData = lyricsData,
                        currentPositionMs = currentPos,
                        onSeekTo = { player.seekTo(it) },
                        onClose = { showLyrics = false },
                        onManualPasteLrc = { text ->
                            lyricsData = LyricParser.parseLrc(text)
                        },
                        onLoadDemoLrc = {
                            lyricsData = LyricParser.parseLrc(
                                LyricParser.getSampleLrc(playingTitle, playingArtist)
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Vinyl Record with Atmospheric Aura
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(280.dp)
                        ) {
                            // Pulsing Halo
                            Box(
                                modifier = Modifier
                                    .size((250 * pulseScale).dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                NeonCyan.copy(alpha = if (isPlaying) glowAlpha else 0.12f),
                                                ElectricIndigo.copy(alpha = if (isPlaying) glowAlpha * 0.7f else 0.05f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            // Vinyl Record Container
                            val infiniteTransition = rememberInfiniteTransition(label = "Vinyl rotation")
                            val rotationAngle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(15000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "Angle"
                            )

                            Box(
                                modifier = Modifier
                                    .size(230.dp)
                                    .clip(CircleShape)
                                    .rotate(if (isPlaying) rotationAngle else 0f)
                                    .background(Color(0xFF0F0F12))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Outer Groove Ring
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF282830),
                                                    Color(0xFF16161B),
                                                    Color(0xFF0B0B0E)
                                                )
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Inner Grooves
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape)
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Center Record Label
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(ElectricIndigo, NeonCyan)
                                                    )
                                                )
                                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Center Spindle Metallic Hole
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF08080C))
                                                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = "Music",
                                                    tint = NeonCyan,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Lossless Audio Spec Badge & Lyrics Indicator Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NeonCyan.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "24-BIT • 48 kHz • LOSSLESS MASTER",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                            if (lyricsData != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    onClick = { showLyrics = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = ElectricIndigo.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Lyrics, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("LYRICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Visualizer bars
                        MusicAudioVisualizer(isPlaying = isPlaying)
                    }
                }
            }

            // Bottom Controls Container
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Sound Equalizer Profile Chips
            val profiles = listOf("Studio", "Bass Boost", "Vocal+", "Spatial 3D")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(profiles) { profile ->
                    val isSelected = selectedSoundProfile == profile
                    Surface(
                        onClick = { selectedSoundProfile = profile },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, if (isSelected) NeonCyan else Color.White.copy(alpha = 0.12f))
                    ) {
                        Text(
                            text = profile,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Song title and details
            Text(
                text = playingTitle,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (playingArtist.isNotEmpty() && playingArtist != "Unknown Artist") "$playingArtist • $playingAlbum" else playingAlbum,
                color = Color.White.copy(0.5f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentTimeFormatted,
                    color = Color.White.copy(0.6f),
                    fontSize = 12.sp
                )
                Slider(
                    value = currentProgress,
                    onValueChange = {
                        isDraggingSlider = true
                        sliderProgress = it
                    },
                    onValueChangeFinished = {
                        player.seekTo((sliderProgress * duration).toLong())
                        isDraggingSlider = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .tvFocusable(cornerRadius = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color.White.copy(0.2f)
                    )
                )
                Text(
                    text = durationFormatted,
                    color = Color.White.copy(0.6f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Playback controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle button
                IconButton(
                    onClick = { player.shuffleModeEnabled = !isShuffleEnabled },
                    modifier = Modifier
                        .size(54.dp)
                        .tvFocusable(cornerRadius = 27.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleEnabled) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Previous button
                IconButton(
                    onClick = { player.seekToPreviousMediaItem() },
                    modifier = Modifier
                        .size(54.dp)
                        .tvFocusable(cornerRadius = 27.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause Circle Button
                IconButton(
                    onClick = {
                        if (isPlaying) player.pause() else player.play()
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFFE040FB))
                              )
                        )
                        .tvFocusable(cornerRadius = 36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Next button
                IconButton(
                    onClick = { player.seekToNextMediaItem() },
                    modifier = Modifier
                        .size(54.dp)
                        .tvFocusable(cornerRadius = 27.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat button
                val repeatIcon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                val repeatTint = if (repeatMode != Player.REPEAT_MODE_OFF) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f)
                IconButton(
                    onClick = {
                        player.repeatMode = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .tvFocusable(cornerRadius = 27.dp)
                ) {
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = repeatTint,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text(text = "Studio Sleep Timer", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = Color(0xFF0C0D14),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(
                        "Turn Off" to 0,
                        "5 Minutes" to 300,
                        "15 Minutes" to 900,
                        "30 Minutes" to 1800,
                        "45 Minutes" to 2700,
                        "60 Minutes" to 3600
                    )
                    options.forEach { (label, secs) ->
                        TextButton(
                            onClick = {
                                sleepTimerRemainingSec = secs
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label, 
                                color = if (sleepTimerRemainingSec == secs) NeonCyan else Color.White.copy(alpha = 0.85f), 
                                fontSize = 15.sp,
                                fontWeight = if (sleepTimerRemainingSec == secs) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Dismiss", color = NeonCyan)
                }
            }
        )
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            containerColor = Color(0xFF0C0D14),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(0.3f)) },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Play Queue",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val mediaItemCount = player.mediaItemCount
                    items(count = mediaItemCount, key = { it }) { index ->
                        val item = player.getMediaItemAt(index)
                        val title = item.mediaMetadata.title?.toString() ?: "Track ${index + 1}"
                        val artist = item.mediaMetadata.artist?.toString() ?: "Unknown Artist"
                        val isCurrent = index == currentTrackIndex
                        
                        Surface(
                            onClick = {
                                player.seekTo(index, 0L)
                                player.play()
                                showQueue = false
                            },
                            color = if (isCurrent) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = if (isCurrent) Color(0xFF00E5FF) else Color.White,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = artist,
                                        color = if (isCurrent) Color(0xFF00E5FF).copy(0.7f) else Color.White.copy(0.5f),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isCurrent && isPlaying) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Playing",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(20.dp)
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

@Composable
fun MusicAudioVisualizer(isPlaying: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.5.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(34.dp)
    ) {
        val heights = listOf(0.25f, 0.45f, 0.75f, 0.95f, 0.65f, 1.0f, 0.65f, 0.95f, 0.75f, 0.45f, 0.25f)
        heights.forEachIndexed { index, baseHeight ->
            val animScale = rememberInfiniteTransition(label = "bar_$index")
            val scale by animScale.animateFloat(
                initialValue = 0.15f,
                targetValue = baseHeight,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 350 + (index * 60), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight(if (isPlaying) scale else 0.15f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonCyan, ElectricIndigo)
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

private fun formatMusicTime(timeMs: Long): String {
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

private fun formatSleepTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)
}
