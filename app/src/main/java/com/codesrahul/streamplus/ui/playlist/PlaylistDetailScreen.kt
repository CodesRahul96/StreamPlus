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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.data.settings.AppSettings
import com.codesrahul.streamplus.ui.components.PremiumBackground
import com.codesrahul.streamplus.ui.settings.SettingsViewModel
import com.codesrahul.streamplus.ui.util.tvFocusable
import com.codesrahul.streamplus.ui.util.glassmorphic

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
    val favoriteIdentities by viewModel.favoriteIdentities.collectAsState()
    val settingsState by hiltViewModel<SettingsViewModel>().settings.collectAsState(initial = AppSettings())
    val isXtreamPlaylist by viewModel.isXtreamPlaylist.collectAsState()
    val selectedStreamType by viewModel.selectedStreamType.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.error.collect { errorMessage ->
            snackbarHostState.showSnackbar(errorMessage)
        }
    }
    
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isXtreamPlaylist) {
                TabRow(
                    selectedTabIndex = when (selectedStreamType) {
                        "live" -> 0
                        "vod" -> 1
                        "series" -> 2
                        else -> 0
                    },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedStreamType == "live",
                        onClick = { viewModel.setSelectedStreamType("live") },
                        text = { Text("TV", fontWeight = if (selectedStreamType == "live") FontWeight.Bold else FontWeight.Medium) }
                    )
                    Tab(
                        selected = selectedStreamType == "vod",
                        onClick = { viewModel.setSelectedStreamType("vod") },
                        text = { Text("Movies", fontWeight = if (selectedStreamType == "vod") FontWeight.Bold else FontWeight.Medium) }
                    )
                    Tab(
                        selected = selectedStreamType == "series",
                        onClick = { viewModel.setSelectedStreamType("series") },
                        text = { Text("Series", fontWeight = if (selectedStreamType == "series") FontWeight.Bold else FontWeight.Medium) }
                    )
                }
            } else if (groups.size > 1) {
                val selectedTabIndex = groups.indexOf(selectedGroup).coerceAtLeast(0)
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    groups.forEachIndexed { index, group ->
                        Tab(
                            selected = selectedGroup == group,
                            onClick = { viewModel.setSelectedGroup(group) },
                            text = { Text(group, fontWeight = if (selectedGroup == group) FontWeight.Bold else FontWeight.Medium) }
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f).nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Column(
                        modifier = Modifier.background(Color.Transparent)
                    ) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        playlist?.name ?: "Playlist",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!isLoading && viewModel.channels.value.isNotEmpty()) {
                                        Text("${filteredChannels.size} channels", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            ),
                            scrollBehavior = scrollBehavior,
                            windowInsets = WindowInsets(0.dp),
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

                        // For Xtream playlists, display sub-categories below the search bar
                        if (isXtreamPlaylist && groups.size > 1) {
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
                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp
                        val columns = when {
                            screenWidth >= 1200 -> 6
                            screenWidth >= 900  -> 5
                            screenWidth >= 600  -> 4
                            else -> 3
                        }
                        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                        val cardAspectRatio = if (isLandscape) 1.2f else 0.85f
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            userScrollEnabled = false
                        ) {
                            items(columns * 3) {
                                com.codesrahul.streamplus.ui.components.ShimmerChannelItem(cardAspectRatio)
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
                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp
                        val columns = when {
                            screenWidth >= 1200 -> 6
                            screenWidth >= 900  -> 5
                            screenWidth >= 600  -> 4
                            else -> 3 // Mobile: 3 columns for better proportions
                        }
                        
                        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                        val cardAspectRatio = if (isLandscape) 1.2f else 0.85f
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement   = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                count = filteredChannels.size,
                                key = { index -> 
                                    val ch = filteredChannels[index]
                                    if (ch.id != 0) ch.id else "${ch.playlistId}_${ch.group}_${ch.name}_${ch.url}"
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
                                            } else if (channel.streamType == "series") {
                                                val seriesId = channel.url.split("/").last().toIntOrNull() ?: -1
                                                val encodedName = java.net.URLEncoder.encode(channel.name, "UTF-8")
                                                onNavigate("series_detail/${playlist?.id ?: -1}/$seriesId/$encodedName")
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
                                    onClick = onItemClick,
                                    cardAspectRatio = cardAspectRatio
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
fun ChannelItem(
    channel: M3UItem,
    hideImage: Boolean,
    currentProgram: String? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit,
    cardAspectRatio: Float = 0.85f
) {
    if (channel.isVod) {
        VodPosterItem(channel, hideImage, isFavorite, onToggleFavorite, onClick, cardAspectRatio)
    } else {
        LiveChannelItem(channel, hideImage, currentProgram, isFavorite, onToggleFavorite, onClick, cardAspectRatio)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VodPosterItem(channel: M3UItem, hideImage: Boolean, isFavorite: Boolean, onToggleFavorite: (() -> Unit)?, onClick: () -> Unit, aspectRatio: Float = 0.85f, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .tvFocusable(cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .glassmorphic(cornerRadius = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Logo Area (same 1.2 weight as Live)
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (channel.logoUrl.isNotBlank() && !hideImage) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(8.dp), 
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Movie,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Favorite Toggle
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Text Area (same 0.8 weight as Live)
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveChannelItem(channel: M3UItem, hideImage: Boolean, currentProgram: String?, isFavorite: Boolean, onToggleFavorite: (() -> Unit)?, onClick: () -> Unit, aspectRatio: Float = 0.85f, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .tvFocusable(cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .glassmorphic(cornerRadius = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Logo Area
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background for logos to ensure visibility
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (channel.logoUrl.isNotBlank() && !hideImage) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(channel.logoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(8.dp), 
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Tv,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Favorite Toggle (overlay on logo area)
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // DRM Badge
                if (!channel.drmScheme.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    ) {
                        Text(
                            "DRM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
                        )
                    }
                }
            }

            // Text Area
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (!currentProgram.isNullOrBlank()) {
                    Text(
                        text = currentProgram,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.9f,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }
    }
}
