package com.codesrahul.streamplus.ui.epg

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.codesrahul.streamplus.data.epg.EPGProgram
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.ui.player.PlayerActivity
import com.codesrahul.streamplus.ui.util.tvFocusable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

private val SLOT_WIDTH = 220.dp // Width for 30 minutes interval
private val CHANNEL_COL_WIDTH = 230.dp
private val ROW_HEIGHT = 60.dp
private val TIME_FORMAT = SimpleDateFormat("hh:mm a", Locale.getDefault())
private val DATE_FORMAT = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

@Composable
fun EPGGridScreen(
    viewModel: EPGViewModel = hiltViewModel(),
    onBackPressed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val horizontalScrollState = rememberScrollState()
    val density = LocalDensity.current

    // 1-minute live ticker
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000L)
            viewModel.refreshTime()
        }
    }

    var contextMenuChannel by remember { mutableStateOf<M3UItem?>(null) }
    var contextMenuProgram by remember { mutableStateOf<EPGProgram?>(null) }

    // Auto-scroll horizontally to current live broadcast on initial load
    var initialScrollDone by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.currentTime, uiState.timelineStart) {
        if (!initialScrollDone && uiState.timelineStart > 0 && uiState.currentTime > uiState.timelineStart) {
            val offsetMinutes = (uiState.currentTime - uiState.timelineStart) / 60000f
            val slotWidthPx = with(density) { SLOT_WIDTH.toPx() }
            val targetPx = ((offsetMinutes / 30f) * slotWidthPx).toInt() - 220
            if (targetPx > 0) {
                horizontalScrollState.scrollTo(targetPx)
                initialScrollDone = true
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF090D16)
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF00E5FF),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading TV Guide & Schedules...",
                        color = Color(0xFF8E9BAE),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Top Header (Back, Playlist Title, Date & Time)
                TopHeaderBar(
                    currentTime = uiState.currentTime,
                    selectedPlaylistName = uiState.selectedPlaylist?.name ?: "All Channels",
                    onBackPressed = onBackPressed
                )

                // Top Hero Program Detail Card (Dynamic focused item details)
                ProgramDetailCard(
                    program = uiState.focusedProgram,
                    channel = uiState.focusedChannel,
                    isFavorite = uiState.focusedChannel?.let { viewModel.isChannelFavorite(it) } ?: false,
                    onToggleFavorite = { uiState.focusedChannel?.let { viewModel.toggleFavorite(it) } }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Category Filter Pills
                if (uiState.categories.isNotEmpty()) {
                    CategoryFilterBar(
                        categories = uiState.categories,
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (uiState.channels.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No channels found in this category",
                            color = Color(0xFF64748B),
                            fontSize = 15.sp
                        )
                    }
                } else {
                    // Sticky Timeline Header (Fixed Channel Column Header + Synchronized Time Slots)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(Color(0xFF0E131E))
                            .border(width = 0.5.dp, color = Color(0xFF1E283D))
                    ) {
                        // Corner Title
                        Box(
                            modifier = Modifier
                                .width(CHANNEL_COL_WIDTH)
                                .fillMaxHeight()
                                .background(Color(0xFF0B0F19))
                                .border(width = 0.5.dp, color = Color(0xFF1E283D))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CHANNELS (${uiState.channels.size})",
                                    color = Color(0xFF8E9BAE),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Time Slots Row (Horizontal Scrollable)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            TimeHeaderRow(
                                startTime = uiState.timelineStart,
                                endTime = uiState.timelineEnd
                            )
                        }
                    }

                    // Unified Grid (Single vertical LazyColumn for 100% synchronized vertical scrolling)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = uiState.channels,
                                key = { idx, ch -> ch.url.takeIf { it.isNotBlank() } ?: "ch_${ch.id}_$idx" }
                            ) { index, channel ->
                                val isChannelSelected = (uiState.focusedChannel?.url == channel.url)
                                val channelPrograms = uiState.channelPrograms[channel.url] ?: emptyList()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(ROW_HEIGHT)
                                ) {
                                    // Pinned Left Channel Header
                                    ChannelRowHeader(
                                        channelIndex = index + 1,
                                        channel = channel,
                                        isFocused = isChannelSelected,
                                        isFavorite = viewModel.isChannelFavorite(channel),
                                        onChannelClicked = {
                                            contextMenuChannel = channel
                                        },
                                        modifier = Modifier.width(CHANNEL_COL_WIDTH)
                                    )

                                    // Horizontally Scrollable Programs for this Channel
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .horizontalScroll(horizontalScrollState)
                                    ) {
                                        ProgramRow(
                                            programs = channelPrograms,
                                            onProgramFocused = { prog ->
                                                viewModel.setFocusedItem(channel, prog)
                                            },
                                            onProgramClicked = { prog ->
                                                val now = System.currentTimeMillis()
                                                val isPast = prog.stop < now
                                                val isFuture = prog.start > now

                                                val (playbackUrl, titleText) = if (isPast) {
                                                    val durationMinutes = ((prog.stop - prog.start) / 60000).coerceAtLeast(1)
                                                    val startSeconds = prog.start / 1000
                                                    val stopSeconds = prog.stop / 1000

                                                    val xtreamPattern = Regex("(https?://[^/]+)/live/([^/]+)/([^/]+)/([0-9]+)(\\.[a-zA-Z0-9]+)?")
                                                    val match = xtreamPattern.find(channel.url)
                                                    val url = if (match != null) {
                                                        val (base, user, pass, streamId, ext) = match.destructured
                                                        val sdf = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US)
                                                        val startTimeFormatted = sdf.format(Date(prog.start))
                                                        val extension = if (ext.isNotEmpty()) ext else ".ts"
                                                        "$base/timeshift/$user/$pass/$durationMinutes/$startTimeFormatted/$streamId$extension"
                                                    } else {
                                                        val separator = if (channel.url.contains("?")) "&" else "?"
                                                        "${channel.url}${separator}utc=$startSeconds&lutc=$stopSeconds"
                                                    }
                                                    Toast.makeText(context, "Catch-Up: ${prog.title}", Toast.LENGTH_SHORT).show()
                                                    Pair(url, "${channel.name} • ${prog.title} (Catch-Up)")
                                                } else if (isFuture) {
                                                    Toast.makeText(context, "Starts at ${TIME_FORMAT.format(Date(prog.start))}. Playing Live.", Toast.LENGTH_SHORT).show()
                                                    Pair(channel.url, channel.name)
                                                } else {
                                                    Pair(channel.url, channel.name)
                                                }

                                                val intent = Intent(context, PlayerActivity::class.java).apply {
                                                    putExtra(PlayerActivity.EXTRA_STREAM_URL, playbackUrl)
                                                    putExtra(PlayerActivity.EXTRA_STREAM_TITLE, titleText)
                                                    putExtra(PlayerActivity.EXTRA_CHANNEL_LOGO, channel.logoUrl)
                                                    putExtra(PlayerActivity.EXTRA_PLAYLIST_ID, channel.playlistId)
                                                    putExtra(PlayerActivity.EXTRA_STREAM_GROUP, channel.group)
                                                    putExtra(PlayerActivity.EXTRA_USER_AGENT, channel.userAgent)
                                                    putExtra(PlayerActivity.EXTRA_DRM_SCHEME, channel.drmScheme)
                                                    putExtra(PlayerActivity.EXTRA_DRM_LICENSE_URL, channel.licenseUrl)
                                                    putExtra(PlayerActivity.EXTRA_HEADERS, HashMap(channel.headers))
                                                    putExtra(PlayerActivity.EXTRA_DRM_HEADERS, HashMap(channel.drmHeaders))
                                                    putExtra(PlayerActivity.EXTRA_IS_VOD, isPast)
                                                    putExtra("STREAM_URL", playbackUrl)
                                                    putExtra("TITLE", titleText)
                                                    putExtra("SUBTITLE", prog.title)
                                                    putExtra("LOGO_URL", channel.logoUrl)
                                                }
                                                context.startActivity(intent)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Live Time Red Marker Line (across programs area)
                        val now = uiState.currentTime
                        if (now in uiState.timelineStart..uiState.timelineEnd) {
                            val offsetMinutes = (now - uiState.timelineStart) / 60000f
                            val xPosition = (offsetMinutes / 30f) * SLOT_WIDTH.value

                            Box(
                                modifier = Modifier
                                    .padding(start = CHANNEL_COL_WIDTH)
                                    .fillMaxHeight()
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = xPosition.dp)
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .background(Color(0xFFFF1744))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Channel Context Menu Modal Sheet (Hold OK / Click channel)
        if (contextMenuChannel != null) {
            val ch = contextMenuChannel!!
            val isFav = viewModel.isChannelFavorite(ch)

            AlertDialog(
                onDismissRequest = { contextMenuChannel = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(ch.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Toggle Favorite
                        Button(
                            onClick = {
                                viewModel.toggleFavorite(ch)
                                contextMenuChannel = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162136)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp).tvFocusable(cornerRadius = 8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (isFav) "Remove from Favorites" else "Add to Favorites (⭐)", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        // Watch Live Fullscreen
                        Button(
                            onClick = {
                                contextMenuChannel = null
                                val intent = Intent(context, PlayerActivity::class.java).apply {
                                    putExtra(PlayerActivity.EXTRA_STREAM_URL, ch.url)
                                    putExtra(PlayerActivity.EXTRA_STREAM_TITLE, ch.name)
                                    putExtra(PlayerActivity.EXTRA_CHANNEL_LOGO, ch.logoUrl)
                                    putExtra(PlayerActivity.EXTRA_PLAYLIST_ID, ch.playlistId)
                                    putExtra(PlayerActivity.EXTRA_STREAM_GROUP, ch.group)
                                    putExtra(PlayerActivity.EXTRA_USER_AGENT, ch.userAgent)
                                    putExtra(PlayerActivity.EXTRA_DRM_SCHEME, ch.drmScheme)
                                    putExtra(PlayerActivity.EXTRA_DRM_LICENSE_URL, ch.licenseUrl)
                                    putExtra(PlayerActivity.EXTRA_HEADERS, HashMap(ch.headers))
                                    putExtra(PlayerActivity.EXTRA_DRM_HEADERS, HashMap(ch.drmHeaders))
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162136)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp).tvFocusable(cornerRadius = 8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Watch Channel Fullscreen", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        // Open in Multi-View Grid
                        Button(
                            onClick = {
                                contextMenuChannel = null
                                val intent = Intent(context, com.codesrahul.streamplus.ui.multiview.MultiViewActivity::class.java).apply {
                                    putExtra(PlayerActivity.EXTRA_STREAM_URL, ch.url)
                                    putExtra(PlayerActivity.EXTRA_STREAM_TITLE, ch.name)
                                    putExtra(PlayerActivity.EXTRA_PLAYLIST_ID, ch.playlistId)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162136)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp).tvFocusable(cornerRadius = 8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GridView, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Open in Multi-View Grid (2x2)", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { contextMenuChannel = null }) {
                        Text("Close", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF0F1420),
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun TopHeaderBar(
    currentTime: Long,
    selectedPlaylistName: String,
    onBackPressed: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161E2E))
                    .tvFocusable(cornerRadius = 17.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F1A2A))
                    .border(width = 0.5.dp, color = Color(0xFF00E5FF).copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = selectedPlaylistName.uppercase(),
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${DATE_FORMAT.format(Date(currentTime))}  |  ${TIME_FORMAT.format(Date(currentTime))}",
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ProgramDetailCard(
    program: EPGProgram?,
    channel: M3UItem?,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E283D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = program?.title ?: "Select a program with remote to view details",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (program != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        if (program.isLive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFF1744))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        } else if (program.stop < System.currentTimeMillis()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0284C7))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("CATCH-UP", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (program != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${TIME_FORMAT.format(Date(program.start))} – ${TIME_FORMAT.format(Date(program.stop))} (${program.durationMinutes} min)",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (program.category.isNotBlank()) {
                            Text(text = " • ", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(
                                text = program.category,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = program.description.ifBlank { "Live television broadcast. Use remote [OK] to watch or browse adjacent schedules." },
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Remote Hints
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "[OK] Watch Channel   [◀/▶] Browse Time   [▲/▼] Change Channel",
                        color = Color(0xFF475569),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            if (channel != null) {
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0B0F19))
                        .border(1.dp, Color(0xFF1E283D), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (channel.logoUrl.isNotBlank()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = channel.name,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E283D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = channel.name,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(28.dp).tvFocusable(cornerRadius = 14.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFFD700) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterBar(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category.equals(selectedCategory, ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF0284C7)))
                        else Brush.horizontalGradient(listOf(Color(0xFF101726), Color(0xFF0E131E)))
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E283D),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onCategorySelected(category) }
                    .tvFocusable(cornerRadius = 16.dp)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = category,
                    color = if (isSelected) Color.Black else Color(0xFFE2E8F0),
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ChannelRowHeader(
    channelIndex: Int,
    channel: M3UItem,
    isFocused: Boolean,
    isFavorite: Boolean = false,
    onChannelClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isFocused) Color(0xFF162136) else Color(0xFF0B0F19))
            .border(width = 0.5.dp, color = if (isFocused) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color(0xFF1E283D))
            .clickable { onChannelClicked() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format("%03d", channelIndex),
            color = if (isFocused) Color(0xFF00E5FF) else Color(0xFF475569),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )

        if (isFavorite) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Favorite",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        if (channel.logoUrl.isNotBlank()) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF161E2E))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF161E2E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = channel.name,
            color = if (isFocused) Color(0xFF00E5FF) else Color.White,
            fontSize = 13.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TimeHeaderRow(
    startTime: Long,
    endTime: Long
) {
    Row(
        modifier = Modifier.fillMaxHeight()
    ) {
        var cursor = startTime
        val step = 30 * 60 * 1000L // 30 minutes

        while (cursor < endTime) {
            Box(
                modifier = Modifier
                    .width(SLOT_WIDTH)
                    .fillMaxHeight()
                    .border(width = 0.5.dp, color = Color(0xFF1E283D))
                    .padding(start = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = TIME_FORMAT.format(Date(cursor)),
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            cursor += step
        }
    }
}

@Composable
private fun ProgramRow(
    programs: List<EPGProgram>,
    onProgramFocused: (EPGProgram) -> Unit,
    onProgramClicked: (EPGProgram) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .border(width = 0.5.dp, color = Color(0xFF1E283D))
    ) {
        programs.forEach { prog ->
            val durationMinutes = max(10, prog.durationMinutes)
            val rawWidth = (durationMinutes / 30f * SLOT_WIDTH.value).dp
            val blockWidth = if (rawWidth < 80.dp) 80.dp else rawWidth

            var isFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .width(blockWidth)
                    .fillMaxHeight()
                    .padding(1.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            isFocused -> Brush.horizontalGradient(listOf(Color(0xFF16325C), Color(0xFF0F2442)))
                            prog.isLive -> Brush.horizontalGradient(listOf(Color(0xFF132238), Color(0xFF0D1726)))
                            prog.stop < System.currentTimeMillis() -> Brush.horizontalGradient(listOf(Color(0xFF0F1420), Color(0xFF0B0F18)))
                            else -> Brush.horizontalGradient(listOf(Color(0xFF121824), Color(0xFF0E131E)))
                        }
                    )
                    .border(
                        width = if (isFocused) 2.dp else if (prog.isLive) 1.dp else 0.5.dp,
                        color = when {
                            isFocused -> Color(0xFF00E5FF)
                            prog.isLive -> Color(0xFF0284C7).copy(alpha = 0.6f)
                            else -> Color(0xFF1E283D)
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            onProgramFocused(prog)
                        }
                    }
                    .tvFocusable(cornerRadius = 6.dp)
                    .clickable { onProgramClicked(prog) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = prog.title,
                        color = if (isFocused) Color(0xFF00E5FF) else if (prog.isLive) Color.White else Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontWeight = if (prog.isLive || isFocused) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TIME_FORMAT.format(Date(prog.start)),
                            color = if (isFocused) Color(0xFF7DD3FC) else Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (prog.isLive) {
                            LinearProgressIndicator(
                                progress = { prog.progress },
                                modifier = Modifier
                                    .width(38.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF00E5FF),
                                trackColor = Color(0xFF1E293B)
                            )
                        } else if (prog.stop < System.currentTimeMillis()) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "Catchup",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
