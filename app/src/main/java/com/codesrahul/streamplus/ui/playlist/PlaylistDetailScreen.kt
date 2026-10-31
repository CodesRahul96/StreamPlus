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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.data.settings.AppSettings
import com.codesrahul.streamplus.ui.components.PremiumBackground
import com.codesrahul.streamplus.ui.settings.SettingsViewModel
import com.codesrahul.streamplus.ui.util.tvFocusable
import com.codesrahul.streamplus.ui.util.glassmorphic

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val playlist by viewModel.playlist.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isInitialLoadDone by viewModel.isInitialLoadDone.collectAsState()
    val isBackgroundLoading by viewModel.isBackgroundLoading.collectAsState()
    val favoriteIdentities by viewModel.favoriteIdentities.collectAsState()
    val settingsState by hiltViewModel<SettingsViewModel>().settings.collectAsState(initial = AppSettings())
    val isXtreamPlaylist by viewModel.isXtreamPlaylist.collectAsState()
    val selectedStreamType by viewModel.selectedStreamType.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showGroupSelectionSheet by remember { mutableStateOf(false) }
    var isListView by remember { mutableStateOf(false) }

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
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp
                
                if (screenWidth >= 600) {
                    val selectedTabIndex = groups.indexOf(selectedGroup).coerceAtLeast(0)
                    val listState = rememberLazyListState()
                    
                    LaunchedEffect(selectedTabIndex) {
                        if (selectedTabIndex >= 0 && selectedTabIndex < groups.size) {
                            listState.animateScrollToItem(selectedTabIndex)
                        }
                    }
                    
                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(
                            count = groups.size,
                            key = { index -> groups[index] }
                        ) { index ->
                            val group = groups[index]
                            val isSelected = selectedGroup == group
                            val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .tvFocusable(cornerRadius = 20.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else if (isLight) Color(0xFFE2E8F0)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else if (isLight) Color.Black.copy(alpha = 0.08f)
                                                else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { viewModel.setSelectedGroup(group) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = group,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    playlist?.let { currentPlaylist ->
                                        val resolvedIcon = if (currentPlaylist.icon == "default") settingsState.defaultPlaylistIcon.lowercase() else currentPlaylist.icon.lowercase()
                                        
                                        val sharedTransitionModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                            with(sharedTransitionScope) {
                                                Modifier.sharedElement(
                                                    rememberSharedContentState(key = "playlist_logo_${currentPlaylist.id}"),
                                                    animatedVisibilityScope = animatedVisibilityScope
                                                )
                                            }
                                        } else {
                                            Modifier
                                        }

                                        Box(
                                            modifier = Modifier
                                                .then(sharedTransitionModifier)
                                                .padding(end = 12.dp)
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (currentPlaylist.color != -16777216) Color(currentPlaylist.color).copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (resolvedIcon) {
                                                    "tv"       -> Icons.Default.Tv
                                                    "movie"    -> Icons.Default.Movie
                                                    "sports"   -> Icons.Default.SportsSoccer
                                                    "music"    -> Icons.Default.MusicNote
                                                    "news"     -> Icons.Default.Public
                                                    "game"     -> Icons.Default.Games
                                                    "code"     -> Icons.Default.Code
                                                    "grid"     -> Icons.Default.GridView
                                                    "card"     -> Icons.Default.ContactPage
                                                    "compact"  -> Icons.Default.ViewHeadline
                                                    else       -> Icons.Default.List
                                                },
                                                contentDescription = null,
                                                tint = if (currentPlaylist.color != -16777216) Color(currentPlaylist.color) else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.clickable {
                                            if (groups.size > 1) showGroupSelectionSheet = true
                                        }
                                    ) {
                                        Text(
                                            playlist?.name ?: "Playlist",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                selectedGroup,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (groups.size > 1) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
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
                                IconButton(onClick = { isListView = !isListView }) {
                                    Icon(
                                        imageVector = if (isListView) Icons.Default.GridView else Icons.Default.ViewList,
                                        contentDescription = "Toggle View Mode"
                                    )
                                }
                                if (groups.size > 1) {
                                    IconButton(onClick = { showGroupSelectionSheet = true }) {
                                        Icon(Icons.Default.Folder, contentDescription = "Select Category")
                                    }
                                }
                                IconButton(onClick = { viewModel.refresh() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                                }
                            }
                        )
                        
                        if (isBackgroundLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.Transparent
                            )
                        } else {
                            Spacer(modifier = Modifier.height(2.dp))
                        }

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
                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp
                        if (isXtreamPlaylist && groups.size > 1 && screenWidth >= 600) {
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
                val isVodView = remember(filteredChannels, isXtreamPlaylist, selectedStreamType) {
                    if (isXtreamPlaylist) {
                        selectedStreamType == "vod" || selectedStreamType == "series"
                    } else {
                        filteredChannels.isNotEmpty() && filteredChannels.all { it.isVod }
                    }
                }

                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                val columns = remember(screenWidth, isLandscape) {
                    if (isLandscape) {
                        when {
                            screenWidth >= 1200 -> 6
                            screenWidth >= 900  -> 5
                            screenWidth >= 600  -> 4
                            else -> 3
                        }
                    } else {
                        when {
                            screenWidth >= 1200 -> 5
                            screenWidth >= 900  -> 4
                            screenWidth >= 600  -> 3
                            else -> 2
                        }
                    }
                }

                val cardAspectRatio = remember(isVodView) {
                    if (isVodView) {
                        0.70f
                    } else {
                        0.92f
                    }
                }

                val gridColumns = if (isListView) 1 else columns
                val verticalSpacing = if (isListView) 8.dp else 16.dp
                val horizontalSpacing = if (isListView) 0.dp else 16.dp

                when {
                    (!isInitialLoadDone || isLoading) && filteredChannels.isEmpty() -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                            userScrollEnabled = false
                        ) {
                            items(gridColumns * 3) {
                                if (isListView) {
                                    ShimmerChannelListItem()
                                } else {
                                    com.codesrahul.streamplus.ui.components.ShimmerChannelItem(cardAspectRatio)
                                }
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
                            columns = GridCells.Fixed(gridColumns),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement   = Arrangement.spacedBy(verticalSpacing),
                            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
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
                                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_IS_VOD, channel.isVod || isVodView)
                                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_TYPE, channel.streamType)
                                                }
                                                context.startActivity(intent)
                                            }
                                        }
                                    }
                                }
                                if (isListView) {
                                    ChannelListItem(
                                        channel = channel,
                                        hideImage = settingsState.hideChannelImage,
                                        currentProgram = currentProgram?.title,
                                        isFavorite = isFav,
                                        onToggleFavorite = onToggleFav,
                                        onClick = onItemClick,
                                        isVodView = isVodView
                                    )
                                } else {
                                    ChannelItem(
                                        channel = channel,
                                        hideImage = settingsState.hideChannelImage,
                                        currentProgram = currentProgram?.title,
                                        isFavorite = isFav,
                                        onToggleFavorite = onToggleFav,
                                        onClick = onItemClick,
                                        cardAspectRatio = cardAspectRatio,
                                        isVodView = isVodView,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showGroupSelectionSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showGroupSelectionSheet = false },
            sheetState = sheetState,
            containerColor = if (MaterialTheme.colorScheme.surface == Color(0xFF000000)) Color(0xFF0C0C0C) else MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            var groupSearchQuery by remember { mutableStateOf("") }
            val filteredGroups = remember(groups, groupSearchQuery) {
                if (groupSearchQuery.isBlank()) {
                    groups
                } else {
                    groups.filter { it.contains(groupSearchQuery, ignoreCase = true) }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = groupSearchQuery,
                    onValueChange = { groupSearchQuery = it },
                    placeholder = { Text("Search categories...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredGroups) { group ->
                        val isSelected = selectedGroup == group
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setSelectedGroup(group)
                                    showGroupSelectionSheet = false
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                    else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = group,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                           else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
}
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ChannelItem(
    channel: M3UItem,
    hideImage: Boolean,
    currentProgram: String? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit,
    cardAspectRatio: Float = 0.92f,
    isVodView: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val lowerUrl = channel.url.lowercase()
    val isExplicitLiveStream = lowerUrl.contains(".m3u8") || lowerUrl.contains("/live/") ||
            lowerUrl.contains("play/live.php") || lowerUrl.substringBefore("?").endsWith(".ts") ||
            lowerUrl.contains(".mpd")
    val isItemVod = !isExplicitLiveStream && (
        channel.streamType == "series" || channel.streamType == "movie" || channel.streamType == "vod" ||
        (channel.isVod && !channel.group.contains("tamil", ignoreCase = true) && !channel.name.contains("TV", ignoreCase = true) && !channel.name.contains("HD", ignoreCase = true)) ||
        (isVodView && channel.streamType != "live")
    )
    if (isItemVod) {
        VodPosterItem(
            channel = channel,
            hideImage = hideImage,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onClick = onClick,
            aspectRatio = cardAspectRatio,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    } else {
        LiveChannelItem(
            channel = channel,
            hideImage = hideImage,
            currentProgram = currentProgram,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onClick = onClick,
            aspectRatio = cardAspectRatio
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun VodPosterItem(
    channel: M3UItem,
    hideImage: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: (() -> Unit)?,
    onClick: () -> Unit,
    aspectRatio: Float = 0.92f,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val isSeries = channel.streamType == "series"
    val seriesId = if (isSeries) channel.url.split("/").last().toIntOrNull() ?: channel.id else channel.id
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .tvFocusable(cornerRadius = 18.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF141724)
        ),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.16f),
                    if (isLight) Color.Black.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.03f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Full-bleed Poster Image
            if (channel.logoUrl.isNotBlank() && !hideImage) {
                val sharedTransitionModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            rememberSharedContentState(key = "series_cover_${seriesId}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else {
                    Modifier
                }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(channel.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = channel.name,
                    modifier = Modifier
                        .then(sharedTransitionModifier)
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // High-end cinematic gradient backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1E1B4B),
                                    Color(0xFF0F172A),
                                    Color(0xFF090D16)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSeries) Icons.Default.Tv else Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF818CF8).copy(alpha = 0.35f)
                    )
                }
            }

            // 2. Cinematic Multi-Stop Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // 3. Top-Left Badge (Series / Movie)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSeries) Color(0xDD7C3AED) else Color(0xDDF59E0B),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = if (isSeries) "SERIES" else "MOVIE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    letterSpacing = 0.6.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }

            // 4. Top-Right Frosted Heart Button
            if (onToggleFavorite != null) {
                Surface(
                    shape = CircleShape,
                    color = if (isLight) Color(0xCCFFFFFF) else Color(0x80000000),
                    border = BorderStroke(0.5.dp, if (isLight) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Favorited" else "Favorite",
                            tint = if (isFavorite) Color(0xFFFF2D55) else if (isLight) Color.Black.copy(alpha = 0.7f) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 5. Floating Play Accent (Bottom Right)
            Surface(
                shape = CircleShape,
                color = if (isLight) Color(0xF0FFFFFF) else Color(0xCC0F172A),
                border = BorderStroke(1.dp, if (isLight) Color(0xFF0284C7).copy(alpha = 0.4f) else Color(0xFF06B6D4).copy(alpha = 0.6f)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp)
                    .size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isLight) Color(0xFF0284C7) else Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 6. Title and Metadata (Bottom Left)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.78f)
                    .padding(start = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp
                )
                val groupSubtitle = if (channel.group.isNotBlank() && channel.group != "0") channel.group else if (isSeries) "Series" else "Movie"
                Text(
                    text = groupSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveChannelItem(
    channel: M3UItem,
    hideImage: Boolean,
    currentProgram: String?,
    isFavorite: Boolean,
    onToggleFavorite: (() -> Unit)?,
    onClick: () -> Unit,
    aspectRatio: Float = 0.92f,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .tvFocusable(cornerRadius = 18.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF131522)
        ),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f),
                    if (isLight) Color.Black.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.03f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // 1. Channel Logo Podium Stage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.radialGradient(
                            colors = if (isLight) listOf(
                                Color(0xFFF8FAFC),
                                Color(0xFFEDF2F7)
                            ) else listOf(
                                Color(0xFF222638),
                                Color(0xFF131622)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Subtle inner border for logo podium
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            1.dp,
                            if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(14.dp)
                        )
                )

                // Logo or Monogram
                if (channel.logoUrl.isNotBlank() && !hideImage) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(channel.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = channel.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Luxury initial monogram fallback
                    val initials = remember(channel.name) {
                        val words = channel.name.trim().split(" ").filter { it.isNotBlank() }
                        if (words.size >= 2) {
                            "${words[0].first()}${words[1].first()}".uppercase()
                        } else {
                            channel.name.take(2).uppercase()
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF0284C7),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }

                // Top-Left Badges: LIVE + DRM
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LIVE Pill with pulsing dot
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xDDFF1744),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = pulseAlpha))
                            )
                            Text(
                                "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.White
                            )
                        }
                    }

                    // DRM Lock Pill
                    if (!channel.drmScheme.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLight) Color(0xEEFEF3C7) else Color(0xCC1E293B),
                            border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = if (isLight) 0.6f else 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isLight) Color(0xFFD97706) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    "DRM",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = if (isLight) Color(0xFFD97706) else Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }

                // Top-Right Favorite Heart
                if (onToggleFavorite != null) {
                    Surface(
                        shape = CircleShape,
                        color = if (isLight) Color(0xD9FFFFFF) else Color(0x80000000),
                        border = BorderStroke(0.5.dp, if (isLight) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(30.dp)
                    ) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) "Favorited" else "Favorite",
                                tint = if (isFavorite) Color(0xFFFF2D55) else if (isLight) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Channel Info & EPG Stage (Fixed 42dp for perfectly identical card alignment)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Channel Title
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) MaterialTheme.colorScheme.onSurface else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.5.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // EPG or Category row
                if (!currentProgram.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isLight) Color(0xFF0284C7) else Color(0xFF38BDF8))
                        )
                        Text(
                            text = currentProgram,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLight) Color(0xFF0284C7) else Color(0xFF38BDF8),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                } else {
                    val groupText = if (channel.group.isNotBlank() && channel.group != "0") channel.group else "Broadcast"
                    Text(
                        text = groupText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelListItem(
    channel: M3UItem,
    hideImage: Boolean,
    currentProgram: String? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit,
    isVodView: Boolean = false
) {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val lowerUrl = channel.url.lowercase()
    val isExplicitLiveStream = lowerUrl.contains(".m3u8") || lowerUrl.contains("/live/") ||
            lowerUrl.contains("play/live.php") || lowerUrl.substringBefore("?").endsWith(".ts") ||
            lowerUrl.contains(".mpd")
    val isItemVod = !isExplicitLiveStream && (
        channel.streamType == "series" || channel.streamType == "movie" || channel.streamType == "vod" ||
        (channel.isVod && !channel.group.contains("tamil", ignoreCase = true) && !channel.name.contains("TV", ignoreCase = true) && !channel.name.contains("HD", ignoreCase = true)) ||
        (isVodView && channel.streamType != "live")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .tvFocusable(cornerRadius = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF131522)
        ),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f),
                    if (isLight) Color.Black.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.03f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Logo Podium / Poster Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            if (isLight) listOf(
                                Color(0xFFF8FAFC),
                                Color(0xFFEDF2F7)
                            ) else listOf(
                                Color(0xFF222638),
                                Color(0xFF131622)
                            )
                        )
                    )
                    .border(1.dp, if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl.isNotBlank() && !hideImage) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(channel.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isItemVod) 0.dp else 8.dp),
                        contentScale = if (isItemVod) ContentScale.Crop else ContentScale.Fit
                    )
                } else {
                    val initials = remember(channel.name) {
                        val words = channel.name.trim().split(" ").filter { it.isNotBlank() }
                        if (words.size >= 2) "${words[0].first()}${words[1].first()}".uppercase()
                        else channel.name.take(2).uppercase()
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0F172A)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Center: Channel Information & Meta
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isLight) MaterialTheme.colorScheme.onSurface else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (!isItemVod) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xDDFF1744)
                        ) {
                            Text(
                                "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                fontSize = 8.5.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (channel.streamType == "series") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xDD7C3AED)
                        ) {
                            Text(
                                "SERIES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (!channel.drmScheme.isNullOrBlank()) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "DRM",
                            tint = if (isLight) Color(0xFFD97706) else Color(0xFFF59E0B),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isItemVod) {
                    val subText = when {
                        channel.streamType == "series" -> "Series"
                        channel.group.isNotBlank() && channel.group != "0" -> channel.group
                        else -> "Movie"
                    }
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    val programText = currentProgram ?: channel.group.ifBlank { "Live Broadcast" }
                    Text(
                        text = programText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (currentProgram != null) (if (isLight) Color(0xFF0284C7) else Color(0xFF38BDF8)) else (if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)),
                        fontWeight = if (currentProgram != null) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (currentProgram != null) Modifier.basicMarquee() else Modifier
                    )
                }
            }

            // Right: Actions (Favorite + Play arrow)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFF2D55) else if (isLight) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = if (isLight) Color(0xFF0284C7) else Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerChannelListItem() {
    val shimmerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(shimmerColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
            )
        }
    }
}

