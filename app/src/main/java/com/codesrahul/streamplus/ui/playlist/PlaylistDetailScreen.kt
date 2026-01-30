package com.codesrahul.streamplus.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val playlist by viewModel.playlist.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("All") }
    
    val groups = remember(channels) {
        listOf("All") + channels.map { it.group }.distinct().filter { it.isNotBlank() }
    }
    
    val filteredChannels = remember(channels, searchQuery, selectedGroup) {
        channels.filter { channel ->
            (selectedGroup == "All" || channel.group == selectedGroup) &&
            (channel.name.contains(searchQuery, ignoreCase = true) || channel.url.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(playlist?.name ?: "Playlist Details") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search channels...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
                
                // Category Filter chips
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups) { group ->
                        FilterChip(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            label = { Text(group) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (filteredChannels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No channels found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredChannels) { channel ->
                        ChannelItem(channel) {
                            val lowerUrl = channel.url.lowercase()
                            val isPlaylist = channel.isPlaylist || 
                                            lowerUrl.endsWith(".m3u") || 
                                            lowerUrl.endsWith(".json") ||
                                            channel.url.contains("m3u=1") ||
                                            (channel.url.contains("cloudplay.pages.dev") && !lowerUrl.contains(".mpd") && !lowerUrl.contains(".m3u8"))

                            if (isPlaylist) {
                                val encodedUrl = java.net.URLEncoder.encode(channel.url, "UTF-8")
                                val encodedName = java.net.URLEncoder.encode(channel.name, "UTF-8")
                                val ua = playlist?.userAgent ?: "Auto"
                                onNavigate("playlist_url_detail/$encodedUrl/$encodedName/$ua")
                            } else {
                                val intent = android.content.Intent(context, com.codesrahul.streamplus.ui.player.PlayerActivity::class.java).apply {
                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_URL, channel.url)
                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_TITLE, channel.name)
                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_CHANNEL_LOGO, channel.logoUrl)
                                    
                                    // Pass all headers as a serializable Map (HashMap)
                                    val allHeaders = java.util.HashMap(channel.headers)
                                    
                                    // Handle User-Agent mapping for the playlist/channel
                                    val playlistUA = playlist?.userAgent ?: "Auto"
                                    val finalUserAgent = if (playlistUA == "Auto") {
                                        channel.headers["User-Agent"] ?: "ExoPlayerLib/2.19.1"
                                    } else if (playlistUA == "Default") {
                                        "ExoPlayerLib/2.19.1"
                                    } else {
                                        playlistUA
                                    }
                                    allHeaders["User-Agent"] = finalUserAgent
                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_USER_AGENT, finalUserAgent)
                                    
                                    channel.headers["Referer"]?.let { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_REFERER, it) }
                                    channel.headers["Cookie"]?.let { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_COOKIE, it) }
                                    channel.headers["Origin"]?.let { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_ORIGIN, it) }
                                    
                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_HEADERS, allHeaders)
                                    
                                    channel.licenseUrl?.let { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_LICENSE_URL, it) }
                                    channel.drmScheme?.let { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_SCHEME, it) }
                                }
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelItem(channel: M3UItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // Fixed height for grid consistency
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                val settingsRepository = hiltViewModel<SettingsViewModel>().settings.collectAsState()
                val hideImage = settingsRepository.value.hideChannelImage

                if (channel.logoUrl.isNotBlank() && !hideImage) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit,
                        error = painterResource(android.R.drawable.ic_menu_report_image) // Fallback
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                minLines = 2,
                modifier = Modifier.padding(8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
