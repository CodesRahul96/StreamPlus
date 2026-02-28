package com.codesrahul.streamplus.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.data.settings.AppSettings
import com.codesrahul.streamplus.ui.components.PremiumBackground
import com.codesrahul.streamplus.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val playlist by viewModel.playlist.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val settingsState by hiltViewModel<SettingsViewModel>().settings.collectAsState(initial = AppSettings())
    
    val context = LocalContext.current

    PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(playlist?.name ?: "Playlist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                if (!isLoading && viewModel.channels.value.isNotEmpty()) {
                                    Text("${filteredChannels.size} channels", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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

                    // Unified search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 6.dp),
                        placeholder = { Text("Search channels...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedBorderColor    = Color.Transparent,
                            focusedBorderColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )

                    // Category chips
                    if (groups.size > 1) {
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(groups) { group ->
                                FilterChip(
                                    selected = selectedGroup == group,
                                    onClick = { viewModel.setSelectedGroup(group) },
                                    label = { Text(group, style = MaterialTheme.typography.labelMedium) },
                                    shape = CircleShape
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Loading channels...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    filteredChannels.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(if (searchQuery.isNotEmpty()) "No results for \"$searchQuery\"" else "No channels found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 140.dp),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredChannels.size, key = { index -> 
                                val channel = filteredChannels[index]
                                "${channel.url}_${channel.name}_$index" 
                            }) { index ->
                                val channel = filteredChannels[index]
                                ChannelItem(channel, settingsState.hideChannelImage) {
                                    if (isLoading) return@ChannelItem
                                    val lowerUrl = channel.url.lowercase()
                                    val isPlaylist = channel.isPlaylist ||
                                        (channel.licenseUrl.isNullOrBlank() && channel.drmScheme.isNullOrBlank() &&
                                         (lowerUrl.endsWith(".m3u") || lowerUrl.endsWith(".json") || lowerUrl.contains("m3u=1")))

                                    if (isPlaylist) {
                                        val encodedUrl  = java.net.URLEncoder.encode(channel.url, "UTF-8")
                                        val encodedName = java.net.URLEncoder.encode(channel.name, "UTF-8")
                                        val ua = java.net.URLEncoder.encode(viewModel.playlist.value?.userAgent ?: "Auto", "UTF-8")
                                        onNavigate("playlist_url_detail/$encodedUrl/$encodedName/$ua")
                                    } else {
                                        val intent = android.content.Intent(context, com.codesrahul.streamplus.ui.player.PlayerActivity::class.java).apply {
                                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_URL, channel.url)
                                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_TITLE, channel.name)
                                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_CHANNEL_LOGO, channel.logoUrl)

                                            val channelHeaderUA = channel.headers["User-Agent"]?.takeIf { it.isNotBlank() && it != "Auto" }
                                            val channelUA       = channel.userAgent.takeIf { it.isNotBlank() && it != "Auto" }
                                            val playlistUA      = viewModel.playlist.value?.userAgent ?: "Auto"
                                            val finalUA = channelHeaderUA
                                                ?: channelUA
                                                ?: if (playlistUA == "Auto" || playlistUA == "Default")
                                                    com.codesrahul.streamplus.util.UserAgentManager.DEFAULT_UA
                                                   else playlistUA

                                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_USER_AGENT, finalUA)
                                            channel.headers["Referer"]?.let { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_REFERER, it) }
                                            channel.headers["Cookie"]?.let  { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_COOKIE, it) }
                                            channel.headers["Origin"]?.let  { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_ORIGIN, it) }
                                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_HEADERS, java.util.HashMap(channel.headers))
                                            channel.licenseUrl?.let { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_LICENSE_URL, it) }
                                            channel.drmScheme?.let  { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_SCHEME, it) }
                                            if (channel.drmHeaders.isNotEmpty()) {
                                                putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_HEADERS, java.util.HashMap(channel.drmHeaders))
                                            }
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
    }
}

// -- Premium Channel Grid Card ----------------------------------------------

@Composable
fun ChannelItem(channel: M3UItem, hideImage: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Logo / placeholder
            if (channel.logoUrl.isNotBlank() && !hideImage) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(channel.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.70f)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        .background(Color.White),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.68f)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // DRM badge
            if (!channel.drmScheme.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                ) {
                    Text(
                        "DRM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Channel name at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        ),
                        RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
