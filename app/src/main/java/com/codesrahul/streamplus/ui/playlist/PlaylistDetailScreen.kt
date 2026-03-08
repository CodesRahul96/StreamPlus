package com.codesrahul.streamplus.ui.playlist

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.data.settings.AppSettings
import com.codesrahul.streamplus.ui.components.PremiumBackground
import com.codesrahul.streamplus.ui.settings.SettingsViewModel
import com.codesrahul.streamplus.ui.util.tvFocusable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlaylistDetailScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
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
    val favoriteIdentities by viewModel.favoriteIdentities.collectAsState()
    val settingsState by hiltViewModel<SettingsViewModel>().settings.collectAsState(initial = AppSettings())
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.error.collect { errorMessage ->
            snackbarHostState.showSnackbar(errorMessage)
        }
    }
    
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    PremiumBackground {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                ) {
                    TopAppBar(
                        title = {
                            Column {
                                val sharedKey = remember(viewModel.playlistId) { "playlist_${viewModel.playlistId}" }
                                with(sharedTransitionScope) {
                                    Text(
                                        playlist?.name ?: "Playlist",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = if (playlist != null) Modifier.sharedElement(
                                            rememberSharedContentState(key = sharedKey),
                                            animatedVisibilityScope = animatedContentScope
                                        ) else Modifier
                                    )
                                }
                                if (!isLoading && viewModel.channels.value.isNotEmpty()) {
                                    Text("${filteredChannels.size} channels", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ),
                        scrollBehavior = scrollBehavior,
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
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 140.dp),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            userScrollEnabled = false
                        ) {
                            items(12) {
                                com.codesrahul.streamplus.ui.components.ShimmerChannelItem()
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
                        val isVodPlaylist = filteredChannels.any { it.isVod }
                        LazyVerticalGrid(
                            columns = if (isVodPlaylist) GridCells.Fixed(3) else GridCells.Adaptive(minSize = 140.dp),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement   = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                count = filteredChannels.size,
                                key = { index -> 
                                    val ch = filteredChannels[index]
                                    "${ch.playlistId}_${ch.url}_${ch.name}"
                                },
                                contentType = { index -> if (filteredChannels[index].isVod) "vod" else "live" }
                            ) { index ->
                                val channel = filteredChannels[index]
                                val playlistId = playlist?.id
                                
                                val currentProgram = remember(channel.epgId) { viewModel.getCurrentProgram(channel.epgId) }
                                
                                val isFav = remember(channel.name, channel.group, playlistId, favoriteIdentities) {
                                    "${channel.name}|${channel.group}|$playlistId" in favoriteIdentities
                                }

                                val onToggleFav = remember(channel, viewModel) {
                                    { viewModel.toggleFavorite(channel) }
                                }

                                val onItemClick = remember(channel, context, viewModel, onNavigate, isLoading, playlist) {
                                    {
                                        if (!isLoading) {
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
                                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_PLAYLIST_ID, playlist?.id ?: -1)
                                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_GROUP, channel.group)
                                                }
                                                context.startActivity(intent)
                                            }
                                        }
                                    }
                                }

                                ChannelItem(
                                    channel = channel,
                                    hideImage = settingsState.hideChannelImage,
                                    currentProgram = currentProgram?.title,
                                    isFavorite = isFav,
                                    onToggleFavorite = onToggleFav,
                                    onClick = onItemClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelItem(
    channel: M3UItem,
    hideImage: Boolean,
    currentProgram: String? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    if (channel.isVod) {
        VodPosterItem(channel, hideImage, isFavorite, onToggleFavorite, onClick)
    } else {
        LiveChannelItem(channel, hideImage, currentProgram, isFavorite, onToggleFavorite, onClick)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VodPosterItem(channel: M3UItem, hideImage: Boolean, isFavorite: Boolean, onToggleFavorite: (() -> Unit)?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.67f) // Standard 2:3 VOD Poster Ratio
            .tvFocusable(cornerRadius = 14.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (channel.logoUrl.isNotBlank() && !hideImage) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // VOD posters are fully cropped
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                            )
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Sleek Gradient Overlay for perfect text contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent, 
                                Color.Transparent, 
                                Color.Black.copy(alpha = 0.7f), 
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (onToggleFavorite != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                ) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveChannelItem(channel: M3UItem, hideImage: Boolean, currentProgram: String?, isFavorite: Boolean, onToggleFavorite: (() -> Unit)?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // 1:1 Perfect square ensures identical heights for all Live items across grids
            .tvFocusable(cornerRadius = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Universal Gradient Background so transparent logos look premium
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                            )
                        )
                    )
            )

            if (channel.logoUrl.isNotBlank() && !hideImage) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        // Ensure the image fits inside the middle, leaving vertical space for the text gradient
                        .padding(bottom = 60.dp)
                        .padding(20.dp),
                    contentScale = ContentScale.Fit // Fit the logo perfectly without stretching
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            if (!channel.drmScheme.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                ) {
                    Text(
                        "DRM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            if (onToggleFavorite != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                ) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Text overlay seamlessly blends with the glass card regardless of logo shape
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, 
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    if (!currentProgram.isNullOrBlank()) {
                        Text(
                            text = currentProgram,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
