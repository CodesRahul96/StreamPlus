package com.codesrahul.streamplus.ui.multiview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.util.Base64
import androidx.media3.common.C
import androidx.media3.common.MediaItem.DrmConfiguration
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.codesrahul.streamplus.data.local.ChannelDao
import com.codesrahul.streamplus.data.local.ChannelEntity
import com.codesrahul.streamplus.ui.player.PlayerActivity
import com.codesrahul.streamplus.ui.theme.StreamPlusTheme
import com.codesrahul.streamplus.ui.util.tvFocusable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TileStream(
    val id: Int,
    var channel: ChannelEntity? = null
)

@AndroidEntryPoint
class MultiViewActivity : ComponentActivity() {

    @Inject
    lateinit var channelDao: ChannelDao

    @Inject
    lateinit var okHttpClient: okhttp3.OkHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialUrl = intent.getStringExtra(PlayerActivity.EXTRA_STREAM_URL) ?: ""
        val initialTitle = intent.getStringExtra(PlayerActivity.EXTRA_STREAM_TITLE) ?: "Main Stream"
        val playlistId = intent.getIntExtra(PlayerActivity.EXTRA_PLAYLIST_ID, -1)

        setContent {
            StreamPlusTheme {
                MultiViewScreen(
                    initialUrl = initialUrl,
                    initialTitle = initialTitle,
                    playlistId = playlistId,
                    channelDao = channelDao,
                    okHttpClient = okHttpClient,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun MultiViewScreen(
    initialUrl: String,
    initialTitle: String,
    playlistId: Int,
    channelDao: ChannelDao,
    okHttpClient: okhttp3.OkHttpClient,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isQuadMode by remember { mutableStateOf(false) }
    var activeTileIndex by remember { mutableIntStateOf(0) }
    var availableChannels by remember { mutableStateOf<List<ChannelEntity>>(emptyList()) }
    var channelPickerTileIndex by remember { mutableStateOf<Int?>(null) }

    // Channels loader
    LaunchedEffect(playlistId) {
        withContext(Dispatchers.IO) {
            val list = if (playlistId > 0) {
                channelDao.getChannelsByPlaylist(playlistId)
            } else {
                emptyList()
            }
            availableChannels = list
        }
    }

    // Tiles state (up to 4 streams)
    val tiles = remember {
        mutableStateListOf(
            TileStream(0, ChannelEntity(playlistId = playlistId, name = initialTitle, url = initialUrl)),
            TileStream(1, null),
            TileStream(2, null),
            TileStream(3, null)
        )
    }

    LaunchedEffect(availableChannels) {
        if (availableChannels.isNotEmpty() && tiles[0].channel != null) {
            val matched = availableChannels.find { it.url == initialUrl || it.name == initialTitle }
            if (matched != null) {
                tiles[0] = tiles[0].copy(channel = matched)
            }
        }
    }

    val tileCount = if (isQuadMode) 4 else 2

    Scaffold(
        containerColor = Color(0xFF090D16)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFF0F1420))
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .tvFocusable(cornerRadius = 16.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "MULTI-VIEW LIVE",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Switch Dual / Quad Mode
                        Button(
                            onClick = { isQuadMode = !isQuadMode },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283D)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp).tvFocusable(cornerRadius = 8.dp)
                        ) {
                            Icon(
                                if (isQuadMode) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isQuadMode) "Dual View (2)" else "Quad View (4)", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // Grid Area
                if (!isQuadMode) {
                    // Dual View (Side-by-Side 50% / 50%)
                    Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                        TilePlayerView(
                            tileIndex = 0,
                            tile = tiles[0],
                            isActive = (activeTileIndex == 0),
                            okHttpClient = okHttpClient,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onTileFocused = { activeTileIndex = 0 },
                            onSelectChannel = { channelPickerTileIndex = 0 },
                            onExpand = { launchFullPlayer(context, tiles[0].channel) }
                        )
                        TilePlayerView(
                            tileIndex = 1,
                            tile = tiles[1],
                            isActive = (activeTileIndex == 1),
                            okHttpClient = okHttpClient,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onTileFocused = { activeTileIndex = 1 },
                            onSelectChannel = { channelPickerTileIndex = 1 },
                            onExpand = { launchFullPlayer(context, tiles[1].channel) }
                        )
                    }
                } else {
                    // Quad View (2x2 Grid)
                    Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            TilePlayerView(
                                tileIndex = 0,
                                tile = tiles[0],
                                isActive = (activeTileIndex == 0),
                                okHttpClient = okHttpClient,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onTileFocused = { activeTileIndex = 0 },
                                onSelectChannel = { channelPickerTileIndex = 0 },
                                onExpand = { launchFullPlayer(context, tiles[0].channel) }
                            )
                            TilePlayerView(
                                tileIndex = 1,
                                tile = tiles[1],
                                isActive = (activeTileIndex == 1),
                                okHttpClient = okHttpClient,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onTileFocused = { activeTileIndex = 1 },
                                onSelectChannel = { channelPickerTileIndex = 1 },
                                onExpand = { launchFullPlayer(context, tiles[1].channel) }
                            )
                        }
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            TilePlayerView(
                                tileIndex = 2,
                                tile = tiles[2],
                                isActive = (activeTileIndex == 2),
                                okHttpClient = okHttpClient,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onTileFocused = { activeTileIndex = 2 },
                                onSelectChannel = { channelPickerTileIndex = 2 },
                                onExpand = { launchFullPlayer(context, tiles[2].channel) }
                            )
                            TilePlayerView(
                                tileIndex = 3,
                                tile = tiles[3],
                                isActive = (activeTileIndex == 3),
                                okHttpClient = okHttpClient,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onTileFocused = { activeTileIndex = 3 },
                                onSelectChannel = { channelPickerTileIndex = 3 },
                                onExpand = { launchFullPlayer(context, tiles[3].channel) }
                            )
                        }
                    }
                }
            }

            // Channel Picker Sheet
            if (channelPickerTileIndex != null) {
                val targetIndex = channelPickerTileIndex!!
                ChannelPickerModal(
                    channels = availableChannels,
                    onDismiss = { channelPickerTileIndex = null },
                    onChannelSelected = { selected ->
                        tiles[targetIndex] = tiles[targetIndex].copy(channel = selected)
                        channelPickerTileIndex = null
                    }
                )
            }
        }
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

private fun launchFullPlayer(context: Context, channel: ChannelEntity?) {
    channel ?: return
    val intent = Intent(context, PlayerActivity::class.java).apply {
        putExtra(PlayerActivity.EXTRA_STREAM_URL, channel.url)
        putExtra(PlayerActivity.EXTRA_STREAM_TITLE, channel.name)
        putExtra(PlayerActivity.EXTRA_CHANNEL_LOGO, channel.logoUrl)
        putExtra(PlayerActivity.EXTRA_PLAYLIST_ID, channel.playlistId)
        putExtra(PlayerActivity.EXTRA_STREAM_GROUP, channel.group)
        putExtra(PlayerActivity.EXTRA_USER_AGENT, channel.userAgent)
        putExtra(PlayerActivity.EXTRA_DRM_SCHEME, channel.drmScheme)
        putExtra(PlayerActivity.EXTRA_DRM_LICENSE_URL, channel.licenseUrl)
        putExtra(PlayerActivity.EXTRA_HEADERS, HashMap(channel.headers))
        putExtra(PlayerActivity.EXTRA_DRM_HEADERS, HashMap(channel.drmHeaders))
    }
    context.startActivity(intent)
}

@Composable
fun TilePlayerView(
    tileIndex: Int,
    tile: TileStream,
    isActive: Boolean,
    okHttpClient: okhttp3.OkHttpClient,
    modifier: Modifier = Modifier,
    onTileFocused: () -> Unit,
    onSelectChannel: () -> Unit,
    onExpand: () -> Unit
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(tile.channel?.url) {
        val ch = tile.channel
        if (ch != null && ch.url.isNotBlank()) {
            val lowerUrl = ch.url.lowercase()
            val manifestType = ch.headers["X-Manifest-Type"]?.lowercase() ?: ""
            val inferredMimeType = when {
                manifestType == "mpd" || manifestType == "dash" -> androidx.media3.common.MimeTypes.APPLICATION_MPD
                manifestType == "hls" || manifestType == "m3u8" -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                lowerUrl.contains(".m3u8") || lowerUrl.contains("type=hls") -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                lowerUrl.contains(".mpd") || lowerUrl.contains("type=dash") -> androidx.media3.common.MimeTypes.APPLICATION_MPD
                else -> null
            }

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(Uri.parse(ch.url))
                .apply { inferredMimeType?.let { setMimeType(it) } }

            val effectiveScheme = ch.drmScheme?.lowercase() ?: ""
            val effectiveLicenseUrl = ch.licenseUrl
            val isClearKey = effectiveScheme.contains("clearkey") ||
                effectiveLicenseUrl?.lowercase()?.contains("clearkey") == true ||
                ch.drmHeaders.keys.any { it.contains("clearkey", ignoreCase = true) || it.equals("X-ClearKey-Data", ignoreCase = true) } ||
                ch.headers.keys.any { it.contains("clearkey", ignoreCase = true) || it.contains("http-clearkey", ignoreCase = true) } ||
                (!effectiveLicenseUrl.isNullOrBlank() && effectiveLicenseUrl.contains(":") && !effectiveLicenseUrl.startsWith("http"))

            val shouldDrm = isClearKey || effectiveScheme.isNotEmpty() || !effectiveLicenseUrl.isNullOrBlank() || ch.drmHeaders.isNotEmpty()
            if (shouldDrm) {
                val uuid = when {
                    isClearKey -> C.CLEARKEY_UUID
                    effectiveScheme.contains("widevine") -> C.WIDEVINE_UUID
                    effectiveScheme.contains("playready") -> C.PLAYREADY_UUID
                    else -> if (!effectiveLicenseUrl.isNullOrBlank()) C.WIDEVINE_UUID else null
                }
                if (uuid != null) {
                    val drmBuilder = DrmConfiguration.Builder(uuid)
                    if (uuid == C.CLEARKEY_UUID) {
                        val rawKey = when {
                            !effectiveLicenseUrl.isNullOrBlank() && (
                                (effectiveLicenseUrl.trim().startsWith("{") && effectiveLicenseUrl.contains("\"keys\"")) ||
                                effectiveLicenseUrl.contains("clearkey.direct", ignoreCase = true) ||
                                (effectiveLicenseUrl.contains(":") && !effectiveLicenseUrl.startsWith("http")) ||
                                (effectiveLicenseUrl.contains(":") && !effectiveLicenseUrl.contains("?") && !effectiveLicenseUrl.contains("&") && Regex("([0-9a-zA-Z_\\-]{22,32}):([0-9a-zA-Z_\\-]{22,32})").containsMatchIn(effectiveLicenseUrl))
                            ) -> effectiveLicenseUrl
                            ch.drmHeaders.containsKey("X-ClearKey-Data") -> ch.drmHeaders["X-ClearKey-Data"]
                            ch.drmHeaders.containsKey("clearkey") -> ch.drmHeaders["clearkey"]
                            ch.headers.containsKey("clearkey") -> ch.headers["clearkey"]
                            ch.headers.containsKey("http-clearkey") -> ch.headers["http-clearkey"]
                            else -> null
                        }
                        val ckJson = rawKey?.let { generateClearKeyJson(it) }
                        if (!ckJson.isNullOrBlank()) {
                            val dUri = "data:application/json;base64," + Base64.encodeToString(ckJson.toByteArray(), Base64.NO_WRAP)
                            drmBuilder.setLicenseUri(Uri.parse(dUri))
                            val dHeaders = ch.drmHeaders.toMutableMap()
                            dHeaders["X-ClearKey-Data"] = ckJson
                            drmBuilder.setLicenseRequestHeaders(dHeaders)
                        } else if (!effectiveLicenseUrl.isNullOrBlank()) {
                            drmBuilder.setLicenseUri(Uri.parse(effectiveLicenseUrl))
                            val dHeaders = ch.drmHeaders.toMutableMap()
                            if (ch.userAgent.isNotBlank()) dHeaders["User-Agent"] = ch.userAgent
                            drmBuilder.setLicenseRequestHeaders(dHeaders)
                        }
                    } else {
                        if (!effectiveLicenseUrl.isNullOrBlank()) {
                            drmBuilder.setLicenseUri(Uri.parse(effectiveLicenseUrl))
                        }
                        val dHeaders = ch.drmHeaders.toMutableMap()
                        if (ch.userAgent.isNotBlank()) dHeaders["User-Agent"] = ch.userAgent
                        drmBuilder.setLicenseRequestHeaders(dHeaders)
                    }
                    drmBuilder.setMultiSession(true)
                    mediaItemBuilder.setDrmConfiguration(drmBuilder.build())
                }
            }

            val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient).apply {
                val httpHeaders = mutableMapOf<String, String>()
                httpHeaders.putAll(ch.headers)
                if (ch.userAgent.isNotBlank()) {
                    httpHeaders["User-Agent"] = ch.userAgent
                }
                setDefaultRequestProperties(httpHeaders)
            }
            val dataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)
            val mediaSourceFactory = com.codesrahul.streamplus.data.player.MediaCodecConfig.createMediaSourceFactory(context, dataSourceFactory, null)

            val player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
                    setMediaItem(mediaItemBuilder.build())
                    prepare()
                    playWhenReady = true
                    volume = if (isActive) 1f else 0f
                }
            exoPlayer = player
        }

        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    LaunchedEffect(isActive) {
        exoPlayer?.volume = if (isActive) 1f else 0f
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .border(
                width = if (isActive) 2.5.dp else 1.dp,
                color = if (isActive) Color(0xFF00E5FF) else Color(0xFF1E283D),
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { if (it.isFocused) onTileFocused() }
            .tvFocusable(cornerRadius = 8.dp)
    ) {
        if (tile.channel != null && tile.channel!!.url.isNotBlank()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        player = exoPlayer
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xE60A0E18))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${tileIndex + 1} • ${tile.channel?.name}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onSelectChannel,
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xCC0E1420))
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Change", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xCC0E1420))
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Expand", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            // Empty Tile placeholder
            Column(
                modifier = Modifier.fillMaxSize().clickable { onSelectChannel() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Assign Channel ${tileIndex + 1}",
                    color = Color(0xFF8E9BAE),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ChannelPickerModal(
    channels: List<ChannelEntity>,
    onDismiss: () -> Unit,
    onChannelSelected: (ChannelEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Channel for Multi-View", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            if (channels.isEmpty()) {
                Text("No channels found in current playlist.", color = Color.Gray)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(channels) { ch ->
                        Surface(
                            onClick = { onChannelSelected(ch) },
                            color = Color(0xFF101726),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E283D)),
                            modifier = Modifier.fillMaxWidth().tvFocusable(cornerRadius = 8.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tv, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(ch.name, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color.White) }
        },
        containerColor = Color(0xFF0E131E)
    )
}
