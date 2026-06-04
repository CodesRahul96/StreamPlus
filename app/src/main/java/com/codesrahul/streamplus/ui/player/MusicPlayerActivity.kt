package com.codesrahul.streamplus.ui.player

import android.os.Bundle
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.codesrahul.streamplus.ui.util.tvFocusable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class MusicPlayerActivity : AppCompatActivity() {

    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
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
        val playlistUris = intent.getStringArrayExtra("EXTRA_PLAYLIST_URIS") ?: emptyArray()
        val playlistTitles = intent.getStringArrayExtra("EXTRA_PLAYLIST_TITLES") ?: emptyArray()
        val playlistIndex = intent.getIntExtra("EXTRA_PLAYLIST_INDEX", 0)

        Log.d("MusicPlayerActivity", "Initializing music player: size=${playlistUris.size}, index=$playlistIndex")

        // Initialize ExoPlayer
        val player = ExoPlayer.Builder(this).build()
        exoPlayer = player

        // Build media items
        val mediaItems = playlistUris.mapIndexed { idx, url ->
            val title = playlistTitles.getOrNull(idx) ?: "Track ${idx + 1}"
            MediaItem.Builder()
                .setUri(Uri.parse(url))
                .setMediaId(url)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(title)
                        .build()
                )
                .build()
        }

        player.setMediaItems(mediaItems)
        if (playlistIndex >= 0 && playlistIndex < mediaItems.size) {
            player.seekTo(playlistIndex, 0L)
        }
        player.prepare()
        player.playWhenReady = true

        setContent {
            StreamPlusTheme {
                MusicPlayerScreen(
                    player = player,
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun finish() {
        super.finish()
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_CLOSE,
                com.codesrahul.streamplus.R.anim.player_pop_enter,
                com.codesrahul.streamplus.R.anim.player_pop_exit
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(
                com.codesrahul.streamplus.R.anim.player_pop_enter,
                com.codesrahul.streamplus.R.anim.player_pop_exit
            )
        }
    }
}

@Composable
fun MusicPlayerScreen(
    player: ExoPlayer,
    onClose: () -> Unit
) {
    var playingTitle by remember { mutableStateOf("Unknown Track") }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    
    var currentTimeFormatted by remember { mutableStateOf("00:00") }
    var durationFormatted by remember { mutableStateOf("00:00") }

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
                mediaItem?.mediaMetadata?.title?.toString()?.let { title ->
                    playingTitle = title
                }
            }
        }
        player.addListener(listener)
        isPlaying = player.isPlaying
        duration = player.duration.coerceAtLeast(1L)
        player.currentMediaItem?.mediaMetadata?.title?.toString()?.let { title ->
            playingTitle = title
        }

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

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderProgress by remember { mutableFloatStateOf(0f) }
    
    val currentProgress = if (isDraggingSlider) sliderProgress else (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF14131B), Color(0xFF07070A))
                )
            )
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
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
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "NOW PLAYING",
                color = Color.White.copy(0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            // Empty spacer for symmetry
            Spacer(modifier = Modifier.size(48.dp))
        }

        // Center Vinyl Disc
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    .size(260.dp)
                    .clip(CircleShape)
                    .rotate(if (isPlaying) rotationAngle else 0f)
                    .background(Color(0xFF121212))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF262626),
                                    Color(0xFF171717),
                                    Color(0xFF0D0D0D)
                                )
                            )
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF00E5FF), Color(0xFFE040FB))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D0D0D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Music",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Visualizer bars
            MusicAudioVisualizer(isPlaying = isPlaying)
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                text = "StreamPlus Music Player",
                color = Color.White.copy(0.5f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
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
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MusicAudioVisualizer(isPlaying: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(30.dp)
    ) {
        val heights = listOf(0.3f, 0.8f, 0.5f, 0.9f, 0.4f, 0.7f, 0.6f)
        heights.forEachIndexed { index, baseHeight ->
            val animScale = rememberInfiniteTransition(label = "bar_$index")
            val scale by animScale.animateFloat(
                initialValue = 0.2f,
                targetValue = baseHeight,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400 + (index * 80), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(if (isPlaying) scale else 0.2f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFE040FB), Color(0xFF00E5FF))
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
