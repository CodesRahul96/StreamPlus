package com.codesrahul.streamplus.ui.playlist

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codesrahul.streamplus.ui.theme.NeonCyan
import com.codesrahul.streamplus.ui.theme.ElectricIndigo
import com.codesrahul.streamplus.ui.theme.ObsidianSlate
import androidx.hilt.navigation.compose.hiltViewModel
import com.codesrahul.streamplus.data.local.FavoriteEntity
import com.codesrahul.streamplus.data.local.PlaylistEntity
import com.codesrahul.streamplus.data.local.HistoryEntity
import com.codesrahul.streamplus.data.settings.AppSettings
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.ui.components.PremiumBackground
import com.codesrahul.streamplus.ui.settings.SettingsViewModel
import com.codesrahul.streamplus.ui.player.PlayerActivity
import com.codesrahul.streamplus.util.UserAgentManager
import com.codesrahul.streamplus.ui.util.tvFocusable
import com.codesrahul.streamplus.ui.util.glassmorphic
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlaylistScreen(
    onPlaylistClick: (Int) -> Unit,
    onChannelClick: (M3UItem) -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showXtreamDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }
    val playlistsState by viewModel.playlists.collectAsState()
    val playlists = playlistsState ?: emptyList()
    val favorites by viewModel.favorites.collectAsState()
    val recentlyWatched by viewModel.recentlyWatched.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showFabMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // Always hoist SettingsViewModel at top-level — never inside a conditional branch
    val settingsState by hiltViewModel<SettingsViewModel>().settings.collectAsState(initial = AppSettings())
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.importLocalPlaylist(context, it) }
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    var selectedTab by remember { mutableIntStateOf(0) }

    PremiumBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (selectedTab != 0) {
                            IconButton(onClick = { selectedTab = 0 }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when (selectedTab) {
                                    0 -> stringResource(com.codesrahul.streamplus.R.string.playlists_tab)
                                    1 -> stringResource(com.codesrahul.streamplus.R.string.favorites_tab)
                                    else -> stringResource(com.codesrahul.streamplus.R.string.recent_tab)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val count = when (selectedTab) {
                                0 -> playlists.size
                                1 -> favorites.size
                                else -> recentlyWatched.size
                            }
                            if (count > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets(0.dp),
                    actions = {
                        IconButton(onClick = { selectedTab = 0 }) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Playlists",
                                tint = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = { selectedTab = 1 }) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorites",
                                tint = if (selectedTab == 1) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = { selectedTab = 2 }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Recent",
                                tint = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        if (selectedTab == 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = "Add Playlist",
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            val isPitchBlack = MaterialTheme.colorScheme.surface == Color(0xFF000000)
                            MaterialTheme(
                                colorScheme = MaterialTheme.colorScheme.copy(
                                    surface = if (isPitchBlack) Color(0xF212121A) else MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                                )
                            ) {
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier
                                        .background(
                                            if (isPitchBlack) Color(0xF2101018) else MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                Brush.verticalGradient(
                                                    listOf(NeonCyan.copy(alpha = 0.45f), ElectricIndigo.copy(alpha = 0.25f), Color.White.copy(alpha = 0.08f))
                                                )
                                            ),
                                            RoundedCornerShape(20.dp)
                                        ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    stringResource(com.codesrahul.streamplus.R.string.add_playlist_url),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    "M3U, M3U8, or JSON URL",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            showMenu = false
                                            showAddDialog = true
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(NeonCyan.copy(alpha = 0.15f), CircleShape)
                                                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.AddLink, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    stringResource(com.codesrahul.streamplus.R.string.login_xtream),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    "Xtream Codes API v2 Login",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            showMenu = false
                                            showXtreamDialog = true
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(ElectricIndigo.copy(alpha = 0.2f), CircleShape)
                                                    .border(1.dp, ElectricIndigo.copy(alpha = 0.4f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.AccountCircle, null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    stringResource(com.codesrahul.streamplus.R.string.add_local_playlist),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    "Browse device storage",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            showMenu = false
                                            filePickerLauncher.launch("*/*")
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape)
                                                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.UploadFile, null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            },
                floatingActionButton = {
                    if (selectedTab == 0 && playlists.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.End) {
                            AnimatedVisibility(
                                visible = showFabMenu,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .glassmorphic(cornerRadius = 8.dp)
                                                .clickable { 
                                                    showFabMenu = false
                                                    filePickerLauncher.launch("*/*") 
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "File",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        SmallFloatingActionButton(
                                            onClick = { 
                                                showFabMenu = false
                                                filePickerLauncher.launch("*/*") 
                                            },
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Default.UploadFile, contentDescription = "Import File")
                                        }
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .glassmorphic(cornerRadius = 8.dp)
                                                .clickable { 
                                                    showFabMenu = false
                                                    showXtreamDialog = true 
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Xtream",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        SmallFloatingActionButton(
                                            onClick = { 
                                                showFabMenu = false
                                                showXtreamDialog = true 
                                            },
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Default.AccountCircle, contentDescription = "Login with Xtream Codes")
                                        }
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .glassmorphic(cornerRadius = 8.dp)
                                                .clickable { 
                                                    showFabMenu = false
                                                    showAddDialog = true 
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "URL",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        SmallFloatingActionButton(
                                            onClick = { 
                                                showFabMenu = false
                                                showAddDialog = true 
                                            },
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Default.AddLink, contentDescription = "Add Playlist URL")
                                        }
                                    }
                                }
                            }
                            
                            FloatingActionButton(
                                onClick = { showFabMenu = !showFabMenu },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.animateContentSize()
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    when (selectedTab) {
                        0 -> {
                            if (isLoading || playlistsState == null) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    userScrollEnabled = false
                                ) {
                                    items(6) {
                                        com.codesrahul.streamplus.ui.components.ShimmerPlaylistItem()
                                    }
                                }
                            } else if (playlists.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            modifier = Modifier.size(100.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.PlaylistAdd,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            text = "No Playlists Found",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Enter a M3U link or import a local file to start streaming your favourite channels.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(32.dp))
                                        Button(
                                            onClick = { showAddDialog = true },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                            contentPadding = PaddingValues(),
                                            modifier = Modifier.height(52.dp).fillMaxWidth(0.72f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.horizontalGradient(listOf(ElectricIndigo, NeonCyan)),
                                                        shape = RoundedCornerShape(16.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Add, null, tint = Color.White)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Add Playlist", fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                val sortedPlaylists = remember(playlists, settingsState.playlistSortOrder) {
                                    val baseSorted = when (settingsState.playlistSortOrder) {
                                        "Name"       -> playlists.sortedBy { it.name.lowercase() }
                                        "Category"   -> playlists.sortedBy { it.icon.lowercase() }
                                        "Added Date" -> playlists.sortedByDescending { it.id }
                                        else         -> playlists
                                    }
                                    baseSorted.sortedByDescending { it.isPinned }
                                }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = viewModel::onSearchQueryChange,
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text(stringResource(com.codesrahul.streamplus.R.string.search_playlists_placeholder)) },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                            trailingIcon = {
                                                if (searchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            )
                                        )
                                    }

                                    if (searchQuery.isNotEmpty()) {
                                        if (isSearching) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator()
                                                }
                                            }
                                        } else if (searchResults.isEmpty()) {
                                            item {
                                                Text(
                                                    "No channels found for \"$searchQuery\"",
                                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            item {
                                                Text(
                                                    "${searchResults.size} results",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                            items(searchResults, key = { "${it.name}|${it.group}|${it.playlistId}|${it.url}" }, contentType = { "channel_search" }) { channel ->
                                                SearchResultItem(channel = channel, onClick = {
                                                    val intent = android.content.Intent(context, PlayerActivity::class.java).apply {
                                                        putExtra(PlayerActivity.EXTRA_STREAM_URL, channel.url)
                                                        putExtra(PlayerActivity.EXTRA_STREAM_TITLE, channel.name)
                                                        putExtra(PlayerActivity.EXTRA_CHANNEL_LOGO, channel.logoUrl)
                                                        putExtra(PlayerActivity.EXTRA_USER_AGENT, channel.userAgent)
                                                        putExtra(PlayerActivity.EXTRA_PLAYLIST_ID, channel.playlistId)
                                                        putExtra(PlayerActivity.EXTRA_STREAM_GROUP, channel.group)
                                                        
                                                        channel.headers["Referer"]?.let { putExtra(PlayerActivity.EXTRA_REFERER, it) }
                                                        channel.headers["Cookie"]?.let  { putExtra(PlayerActivity.EXTRA_COOKIE, it) }
                                                        channel.headers["Origin"]?.let  { putExtra(PlayerActivity.EXTRA_ORIGIN, it) }
                                                        
                                                        putExtra(PlayerActivity.EXTRA_HEADERS, java.util.HashMap(channel.headers))
                                                        channel.licenseUrl?.let { putExtra(PlayerActivity.EXTRA_DRM_LICENSE_URL, it) }
                                                        channel.drmScheme?.let  { putExtra(PlayerActivity.EXTRA_DRM_SCHEME, it) }
                                                        if (channel.drmHeaders.isNotEmpty()) {
                                                            putExtra(PlayerActivity.EXTRA_DRM_HEADERS, java.util.HashMap(channel.drmHeaders))
                                                        }
                                                        putExtra(PlayerActivity.EXTRA_IS_VOD, channel.isVod)
                                                        putExtra(PlayerActivity.EXTRA_STREAM_TYPE, channel.streamType)
                                                    }
                                                    context.startActivity(intent)
                                                })
                                            }
                                        }
                                    } else {
                                        items(sortedPlaylists, key = { it.id }, contentType = { "playlist" }) { playlist ->
                                            val onPlaylistClickLambda = remember(playlist.id) { { onPlaylistClick(playlist.id) } }
                                            val onEditLambda = remember(playlist) { { showEditDialog = playlist } }
                                            val onPinToggleLambda = remember(playlist) { { viewModel.togglePin(playlist) } }

                                            PlaylistItem(
                                                playlist = playlist,
                                                settingsState = settingsState,
                                                onClick = onPlaylistClickLambda,
                                                onEdit = onEditLambda,
                                                onPinToggle = onPinToggleLambda,
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = animatedVisibilityScope
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (favorites.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            modifier = Modifier.size(100.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.FavoriteBorder,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            text = "No Favorites Yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Mark channels as favorite to quickly access them here.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                                val screenWidth = configuration.screenWidthDp
                                val columns = when {
                                    screenWidth >= 900  -> 5
                                    screenWidth >= 600  -> 4
                                    else -> 3
                                }
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(columns),
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(favorites, key = { "${it.name}|${it.group}|${it.playlistId}" }, contentType = { "channel_fav" }) { fav ->
                                        FavouriteChannelCard(
                                            favorite = fav,
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { onChannelClick(fav) },
                                            onRemove = { viewModel.toggleFavorite(fav, fav.playlistId) }
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (recentlyWatched.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            modifier = Modifier.size(100.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            text = "No Recent Playbacks",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Channels you play will appear here for quick access.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                                val screenWidth = configuration.screenWidthDp
                                val columns = when {
                                    screenWidth >= 900  -> 5
                                    screenWidth >= 600  -> 4
                                    else -> 3
                                }
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(columns),
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(recentlyWatched, key = { it.url + it.name }, contentType = { "channel_recent" }) { item ->
                                        RecentlyWatchedCard(
                                            entry = item,
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { onChannelClick(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }

    if (showAddDialog) {
        PlaylistDialog(
            title = "Add Playlist",
            confirmButtonText = "Save",
            onConfirm = { name, url, userAgent, icon, color, epgUrl ->
                val trimmed = url.trim()
                val finalUrl = when {
                    trimmed.startsWith("file://", ignoreCase = true) ||
                    trimmed.startsWith("content://", ignoreCase = true) ||
                    trimmed.startsWith("/") ||
                    trimmed.contains("://") -> trimmed
                    trimmed.contains("#EXTINF") || trimmed.contains("#EXTM3U") || trimmed.contains("#KODIPROP") ||
                    (trimmed.startsWith("[") && trimmed.endsWith("]")) || (trimmed.startsWith("{") && trimmed.endsWith("}")) -> {
                        // Raw playlist pasted directly! Save locally
                        try {
                            val playlistDir = java.io.File(context.filesDir, "playlists").apply { if (!exists()) mkdirs() }
                            val ext = if (trimmed.startsWith("[") || trimmed.startsWith("{")) "json" else "m3u"
                            val file = java.io.File(playlistDir, "${System.currentTimeMillis()}_custom.$ext")
                            file.writeText(trimmed)
                            android.net.Uri.fromFile(file).toString()
                        } catch (e: Exception) {
                            trimmed
                        }
                    }
                    trimmed.isNotEmpty() -> "http://$trimmed"
                    else -> trimmed
                }
                var finalEpgUrl = epgUrl.trim()
                if (finalEpgUrl.isNotEmpty() && !finalEpgUrl.contains("://") && !finalEpgUrl.startsWith("/")) {
                    finalEpgUrl = "http://$finalEpgUrl"
                }
                viewModel.addPlaylist(name, finalUrl, userAgent, icon, color, finalEpgUrl)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    showEditDialog?.let { playlist ->
        if (playlist.isXtream) {
            XtreamPlaylistDialog(
                title = "Edit Xtream Playlist",
                initialName = playlist.name,
                initialServerUrl = playlist.serverUrl ?: playlist.url,
                initialUsername = playlist.username ?: "",
                initialPassword = playlist.password ?: "",
                initialIcon = playlist.icon,
                initialColor = playlist.color,
                confirmButtonText = "Update",
                onConfirm = { name, server, user, pass, icon, color ->
                    var finalServer = server.trim()
                    if (finalServer.isNotEmpty() &&
                        !finalServer.startsWith("http://", ignoreCase = true) && 
                        !finalServer.startsWith("https://", ignoreCase = true)) {
                        finalServer = "http://$finalServer"
                    }
                    viewModel.updatePlaylist(
                        playlist.copy(
                            name = name,
                            url = finalServer,
                            serverUrl = finalServer,
                            username = user,
                            password = pass,
                            icon = icon,
                            color = color
                        )
                    )
                    showEditDialog = null
                },
                onDismiss = { showEditDialog = null },
                onDelete = {
                    playlistToDelete = playlist
                    showEditDialog = null
                }
            )
        } else {
            PlaylistDialog(
                title = "Edit Playlist",
                initialName = playlist.name,
                initialUrl = playlist.url,
                initialUserAgent = playlist.userAgent,
                initialIcon = playlist.icon,
                initialColor = playlist.color,
                initialEpgUrl = playlist.epgUrl ?: "",
                confirmButtonText = "Update",
                onConfirm = { name, url, userAgent, icon, color, epgUrl ->
                    val trimmed = url.trim()
                    val finalUrl = when {
                        trimmed.startsWith("file://", ignoreCase = true) ||
                        trimmed.startsWith("content://", ignoreCase = true) ||
                        trimmed.startsWith("/") ||
                        trimmed.contains("://") -> trimmed
                        trimmed.isNotEmpty() -> "http://$trimmed"
                        else -> trimmed
                    }
                    var finalEpgUrl = epgUrl.trim()
                    if (finalEpgUrl.isNotEmpty() && !finalEpgUrl.contains("://") && !finalEpgUrl.startsWith("/")) {
                        finalEpgUrl = "http://$finalEpgUrl"
                    }
                    viewModel.updatePlaylist(playlist.copy(name = name, url = finalUrl, userAgent = userAgent, icon = icon, color = color, epgUrl = finalEpgUrl))
                    showEditDialog = null
                },
                onDismiss = { showEditDialog = null },
                onDelete = {
                    playlistToDelete = playlist
                    showEditDialog = null
                }
            )
        }
    }
    
    if (showXtreamDialog) {
        XtreamPlaylistDialog(
            onConfirm = { name, server, user, pass, icon, color ->
                var finalServer = server.trim()
                if (finalServer.isNotEmpty() &&
                    !finalServer.startsWith("http://", ignoreCase = true) && 
                    !finalServer.startsWith("https://", ignoreCase = true)) {
                    finalServer = "http://$finalServer"
                }
                viewModel.addXtreamPlaylist(name, finalServer, user, pass, icon, color)
                showXtreamDialog = false
            },
            onDismiss = { showXtreamDialog = false }
        )
    }

    val isPitchBlack = MaterialTheme.colorScheme.surface == Color(0xFF000000)
    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            tonalElevation = if (isPitchBlack) 0.dp else AlertDialogDefaults.TonalElevation,
            title = { Text(stringResource(com.codesrahul.streamplus.R.string.delete_playlist_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(com.codesrahul.streamplus.R.string.delete_playlist_confirm_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(playlist)
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(com.codesrahul.streamplus.R.string.delete_btn), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text(stringResource(com.codesrahul.streamplus.R.string.cancel_btn))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlaylistItem(
    playlist: PlaylistEntity,
    settingsState: AppSettings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onPinToggle: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val hideUri = settingsState.hidePlaylistUri
    val resolvedIcon = if (playlist.icon == "default") settingsState.defaultPlaylistIcon.lowercase() else playlist.icon.lowercase()
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val cardBg = if (isLight) Color(0xF5FFFFFF) else Color(0xF0121624)

    val typeLabel = when {
        playlist.isXtream -> "Xtream"
        playlist.url.startsWith("file://") || playlist.url.startsWith("/storage/") || playlist.url.startsWith("content://") -> "Local"
        else -> null
    }
    val typeBadgeColor = when {
        playlist.isXtream -> Color(0xFF00E5FF)
        playlist.url.startsWith("file://") || playlist.url.startsWith("/storage/") || playlist.url.startsWith("content://") -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(cornerRadius = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f),
                    if (isLight) Color.Black.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.02f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sharedTransitionModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedElement(
                        rememberSharedContentState(key = "playlist_logo_${playlist.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            } else {
                Modifier
            }

            // Colored icon box with soft glow
            Box(
                modifier = Modifier
                    .then(sharedTransitionModifier)
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (playlist.color != -16777216) Color(playlist.color).copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                    .border(
                        0.5.dp,
                        if (playlist.color != -16777216) Color(playlist.color).copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        RoundedCornerShape(14.dp)
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
                    tint = if (playlist.color != -16777216) Color(playlist.color) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = playlist.name.take(200),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false).basicMarquee(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (typeLabel != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = typeBadgeColor.copy(alpha = 0.14f),
                            border = BorderStroke(0.5.dp, typeBadgeColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = typeBadgeColor,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (!hideUri) {
                    Spacer(modifier = Modifier.height(3.dp))
                    val displaySubtitle = when {
                        playlist.isXtream -> {
                            val host = playlist.serverUrl?.removePrefix("http://")?.removePrefix("https://")?.substringBefore("/") ?: "Xtream Service"
                            "Xtream • $host"
                        }
                        playlist.url.startsWith("file://") || playlist.url.startsWith("/storage/") || playlist.url.startsWith("content://") -> {
                            val filename = playlist.url.substringAfterLast("/")
                            "Local • $filename"
                        }
                        else -> {
                            val domain = playlist.url.removePrefix("http://").removePrefix("https://").substringBefore("/").substringBefore("?")
                            domain.ifBlank { playlist.url }
                        }
                    }
                    Text(
                        text = displaySubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                onClick = onPinToggle,
                shape = CircleShape,
                color = if (playlist.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                modifier = Modifier.size(36.dp).tvFocusable(cornerRadius = 18.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pin",
                        tint = if (playlist.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Surface(
                onClick = onEdit,
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier.size(36.dp).tvFocusable(cornerRadius = 18.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun extractPlaylistNameFromUrl(streamUrl: String): String? {
    return try {
        val uri = android.net.Uri.parse(streamUrl)
        val lastSegment = uri.lastPathSegment ?: return null
        val dotIndex = lastSegment.lastIndexOf('.')
        val base = if (dotIndex != -1) lastSegment.substring(0, dotIndex) else lastSegment
        val cleaned = base.replace(Regex("[_\\-]"), " ")
            .split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }.trim()
        cleaned.ifEmpty { null }
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDialog(
    title: String,
    initialName: String = "",
    initialUrl: String = "",
    initialUserAgent: String = "Auto",
    initialIcon: String = "default",
    initialColor: Int = -16777216,
    initialEpgUrl: String = "",
    confirmButtonText: String,
    onConfirm: (String, String, String, String, Int, String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }
    var icon by remember { mutableStateOf(initialIcon) }
    var color by remember { mutableStateOf(initialColor) }
    var epgUrl by remember { mutableStateOf(initialEpgUrl) }
    var isNameModified by remember { mutableStateOf(initialName.isNotEmpty()) }

    var userAgentDisplay by remember {
        mutableStateOf(
            when (initialUserAgent) {
                "Auto" -> "Auto (Inbuilt)"
                UserAgentManager.DEFAULT_UA -> "Default"
                else -> UserAgentManager.presets.find { it.value == initialUserAgent }?.name ?: "Custom"
            }
        )
    }
    var userAgentValue by remember { mutableStateOf(initialUserAgent.ifEmpty { UserAgentManager.DEFAULT_UA }) }
    var isUserAgentExpanded by remember { mutableStateOf(false) }
    val userAgentOptions = listOf("Auto (Inbuilt)", "Default") + UserAgentManager.presets.map { it.name } + "Custom"

    var showCustomUADialog by remember { mutableStateOf(false) }
    var customUAInput by remember { mutableStateOf("") }

    val icons = listOf("default", "tv", "movie", "sports", "music", "news", "game", "code")
    val colors = listOf(
        -16777216, 0xFF6200EE.toInt(), 0xFF007AFF.toInt(), 0xFF34C759.toInt(),
        0xFFFF9500.toInt(), 0xFFFF3B30.toInt(), 0xFF5E5CE6.toInt(), 0xFF5AC8FA.toInt()
    )

    val isPitchBlack = MaterialTheme.colorScheme.surface == Color(0xFF000000)
    if (showCustomUADialog) {
        AlertDialog(
            shape = RoundedCornerShape(20.dp),
            onDismissRequest = { showCustomUADialog = false },
            tonalElevation = if (isPitchBlack) 0.dp else AlertDialogDefaults.TonalElevation,
            title = { Text(stringResource(com.codesrahul.streamplus.R.string.custom_ua_title), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = customUAInput,
                    onValueChange = { customUAInput = it },
                    label = { Text(stringResource(com.codesrahul.streamplus.R.string.ua_string_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    userAgentValue = customUAInput.ifBlank { UserAgentManager.DEFAULT_UA }
                    userAgentDisplay = "Custom"
                    showCustomUADialog = false
                }) { Text(stringResource(com.codesrahul.streamplus.R.string.confirm_btn), fontWeight = FontWeight.Bold, color = NeonCyan) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomUADialog = false }) { Text(stringResource(com.codesrahul.streamplus.R.string.cancel_btn)) }
            }
        )
    }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val detectedProtocol = remember(url) {
        val trimmed = url.trim().lowercase()
        when {
            trimmed.isEmpty() -> null
            trimmed.contains(".m3u8") || trimmed.contains(".m3u") -> "M3U / HLS Playlist"
            trimmed.contains("/get.php") || trimmed.contains("player_api.php") -> "Xtream Codes Stream"
            trimmed.contains(".mpd") -> "MPEG-DASH Stream"
            trimmed.contains(".mp4") || trimmed.contains(".mkv") -> "Direct Media File"
            else -> "Custom IPTV Stream"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = if (isPitchBlack) Color(0xF50A0A12) else MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        NeonCyan.copy(alpha = 0.55f),
                        ElectricIndigo.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            ),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(ElectricIndigo.copy(alpha = 0.35f), NeonCyan.copy(alpha = 0.2f))
                                    )
                                )
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (onDelete != null) Icons.Default.Edit else Icons.Default.AddLink,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonCyan.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "M3U8 • M3U • JSON • Local Stream",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quick Clipboard Paste Pill
                val clipboardText = remember { clipboardManager.getText()?.text?.trim() ?: "" }
                if (clipboardText.isNotEmpty() &&
                    (clipboardText.startsWith("http://", ignoreCase = true) || clipboardText.startsWith("https://", ignoreCase = true)) &&
                    url != clipboardText
                ) {
                    Surface(
                        onClick = {
                            url = clipboardText
                            if (!isNameModified && clipboardText.isNotEmpty()) {
                                extractPlaylistNameFromUrl(clipboardText)?.let { name = it }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = NeonCyan.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Paste Detected Stream URL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                Text(clipboardText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        }
                    }
                }

                // Playlist Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; isNameModified = true },
                    label = { Text(stringResource(com.codesrahul.streamplus.R.string.playlist_name)) },
                    leadingIcon = {
                        Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = NeonCyan)
                    },
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            IconButton(onClick = { name = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // M3U / Media URL Field
                OutlinedTextField(
                    value = url,
                    onValueChange = { newUrl ->
                        url = newUrl
                        if (!isNameModified && newUrl.isNotEmpty()) {
                            extractPlaylistNameFromUrl(newUrl)?.let { name = it }
                        }
                    },
                    label = { Text(stringResource(com.codesrahul.streamplus.R.string.m3u_url_placeholder)) },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = ElectricIndigo)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val text = clipboardManager.getText()?.text
                                    if (!text.isNullOrBlank()) {
                                        url = text.trim()
                                        if (!isNameModified) {
                                            extractPlaylistNameFromUrl(text.trim())?.let { name = it }
                                        }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                            if (url.isNotEmpty()) {
                                IconButton(onClick = { url = "" }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                        focusedBorderColor = ElectricIndigo,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                // Smart Protocol Detection Badge
                if (detectedProtocol != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NeonCyan.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Auto Detected: $detectedProtocol",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // EPG XMLTV URL
                OutlinedTextField(
                    value = epgUrl,
                    onValueChange = { epgUrl = it },
                    label = { Text(stringResource(com.codesrahul.streamplus.R.string.epg_url_placeholder)) },
                    placeholder = { Text(stringResource(com.codesrahul.streamplus.R.string.epg_optional)) },
                    leadingIcon = {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (epgUrl.isNotEmpty()) {
                            IconButton(onClick = { epgUrl = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // User Agent Selector
                ExposedDropdownMenuBox(
                    expanded = isUserAgentExpanded,
                    onExpandedChange = { isUserAgentExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = userAgentDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(com.codesrahul.streamplus.R.string.user_agent_label)) },
                        leadingIcon = {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUserAgentExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    MaterialTheme(
                        colorScheme = MaterialTheme.colorScheme.copy(
                            surface = if (isPitchBlack) Color(0xF212121A) else MaterialTheme.colorScheme.surface.copy(alpha = 1.0f)
                        )
                    ) {
                        ExposedDropdownMenu(
                            expanded = isUserAgentExpanded,
                            onDismissRequest = { isUserAgentExpanded = false }
                        ) {
                            userAgentOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        when (option) {
                                            "Auto (Inbuilt)" -> { userAgentDisplay = option; userAgentValue = "Auto" }
                                            "Custom" -> { customUAInput = userAgentValue; showCustomUADialog = true }
                                            else -> {
                                                userAgentDisplay = option
                                                userAgentValue = UserAgentManager.getFullUserAgent(option)
                                            }
                                        }
                                        isUserAgentExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section Card: Icon & Color Customization
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(com.codesrahul.streamplus.R.string.icon_theme_sec),
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Personalize", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(icons) { iconKey ->
                                val isSelected = icon == iconKey
                                Surface(
                                    onClick = { icon = iconKey },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = if (isSelected) BorderStroke(1.5.dp, NeonCyan) else null,
                                    modifier = Modifier.size(46.dp).tvFocusable(cornerRadius = 14.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (iconKey) {
                                                "tv"     -> Icons.Default.Tv
                                                "movie"  -> Icons.Default.Movie
                                                "sports" -> Icons.Default.SportsSoccer
                                                "music"  -> Icons.Default.MusicNote
                                                "news"   -> Icons.Default.Public
                                                "game"   -> Icons.Default.Games
                                                "code"   -> Icons.Default.Code
                                                else     -> Icons.Default.List
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(colors) { colorInt ->
                                val isSelected = color == colorInt
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (colorInt == -16777216) MaterialTheme.colorScheme.onSurface else Color(colorInt))
                                        .tvFocusable(cornerRadius = 18.dp)
                                        .clickable { color = colorInt }
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) NeonCyan else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (colorInt == -16777216) MaterialTheme.colorScheme.surface else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(stringResource(com.codesrahul.streamplus.R.string.cancel_btn), fontWeight = FontWeight.SemiBold)
                    }

                    val canSave = url.isNotBlank()
                    Button(
                        onClick = {
                            val trimmedUrl = url.trim()
                            if (trimmedUrl.isNotBlank()) {
                                val finalName = name.trim().ifBlank {
                                    extractPlaylistNameFromUrl(trimmedUrl) ?: "My Playlist"
                                }
                                onConfirm(finalName, trimmedUrl, userAgentValue, icon, color, epgUrl.trim())
                            }
                        },
                        enabled = canSave,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier.weight(1.3f).height(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (canSave) {
                                        Brush.horizontalGradient(listOf(ElectricIndigo, NeonCyan))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.25f), Color.Gray.copy(alpha = 0.25f)))
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(confirmButtonText, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultItem(channel: M3UItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().tvFocusable(cornerRadius = 12.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Channel logo
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                if (channel.logoUrl.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    channel.name.take(200),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                if (channel.group.isNotBlank()) {
                    Text(channel.group, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentlyWatchedCard(entry: M3UItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val cardBg = if (isLight) Color(0xF5FFFFFF) else Color(0xF0121624)
    Card(
        modifier = modifier
            .aspectRatio(0.95f)
            .tvFocusable(cornerRadius = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f),
                    if (isLight) Color.Black.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.02f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                modifier = Modifier.size(54.dp)
            ) {
                if (entry.logoUrl.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = entry.logoUrl,
                        contentDescription = entry.name,
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                entry.name.take(200),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavouriteChannelCard(
    favorite: M3UItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val cardBg = if (isLight) Color(0xF5FFFFFF) else Color(0xF0121624)
    Card(
        modifier = modifier
            .aspectRatio(0.95f)
            .tvFocusable(cornerRadius = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f),
                    if (isLight) Color.Black.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.02f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                    modifier = Modifier.size(54.dp)
                ) {
                    if (favorite.logoUrl.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = favorite.logoUrl,
                            contentDescription = favorite.name,
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(26.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    favorite.name.take(200),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                onClick = onRemove,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Remove from favourites",
                        tint = Color(0xFFFF2D55),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun XtreamPlaylistDialog(
    title: String = "Login to API",
    initialName: String = "",
    initialServerUrl: String = "",
    initialUsername: String = "",
    initialPassword: String = "",
    initialIcon: String = "default",
    initialColor: Int = -1,
    confirmButtonText: String = "Login",
    onConfirm: (String, String, String, String, String, Int) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }
    var passwordVisible by remember { mutableStateOf(false) }

    val isPitchBlack = MaterialTheme.colorScheme.surface == Color(0xFF000000)
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = if (isPitchBlack) Color(0xF50A0A12) else MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        ElectricIndigo.copy(alpha = 0.55f),
                        NeonCyan.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            ),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(ElectricIndigo.copy(alpha = 0.4f), NeonCyan.copy(alpha = 0.2f))
                                    )
                                )
                                .border(1.dp, ElectricIndigo.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (onDelete != null) Icons.Default.Edit else Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = ElectricIndigo,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ElectricIndigo.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Xtream Codes API v2 • Live TV, VOD & Series",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricIndigo,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quick Clipboard Paste Pill for Server URL
                val clipboardText = remember { clipboardManager.getText()?.text?.trim() ?: "" }
                if (clipboardText.isNotEmpty() &&
                    (clipboardText.startsWith("http://", ignoreCase = true) || clipboardText.startsWith("https://", ignoreCase = true)) &&
                    serverUrl != clipboardText
                ) {
                    Surface(
                        onClick = { serverUrl = clipboardText },
                        shape = RoundedCornerShape(12.dp),
                        color = ElectricIndigo.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Paste Server URL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricIndigo)
                                Text(clipboardText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricIndigo)
                        }
                    }
                }

                // Provider / Account Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(com.codesrahul.streamplus.R.string.provider_name)) },
                    leadingIcon = {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = NeonCyan)
                    },
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            IconButton(onClick = { name = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Server URL & Port
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text(stringResource(com.codesrahul.streamplus.R.string.server_url_label)) },
                    placeholder = { Text("http://example.com:8080") },
                    leadingIcon = {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = ElectricIndigo)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val text = clipboardManager.getText()?.text
                                    if (!text.isNullOrBlank()) {
                                        serverUrl = text.trim()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                            }
                            if (serverUrl.isNotEmpty()) {
                                IconButton(onClick = { serverUrl = "" }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                        focusedBorderColor = ElectricIndigo,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(com.codesrahul.streamplus.R.string.username_label)) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (username.isNotEmpty()) {
                            IconButton(onClick = { username = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password with Visibility Toggle
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(com.codesrahul.streamplus.R.string.password_label)) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(stringResource(com.codesrahul.streamplus.R.string.cancel_btn), fontWeight = FontWeight.SemiBold)
                    }

                    val canSaveXtream = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
                    Button(
                        onClick = {
                            val trimmedServer = serverUrl.trim()
                            val trimmedUser = username.trim()
                            val trimmedPass = password.trim()
                            if (trimmedServer.isNotBlank() && trimmedUser.isNotBlank() && trimmedPass.isNotBlank()) {
                                val finalName = name.trim().ifBlank {
                                    val host = try {
                                        val uri = android.net.Uri.parse(
                                            if (trimmedServer.contains("://")) trimmedServer else "http://$trimmedServer"
                                        )
                                        uri.host
                                    } catch (e: Exception) { null }
                                    host?.takeIf { it.isNotBlank() } ?: "Xtream IPTV"
                                }
                                val finalColor = if (initialColor != -1) initialColor else {
                                    val colors = listOf(Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFD54F), Color(0xFFBA68C8))
                                    colors.random().toArgb()
                                }
                                onConfirm(finalName, trimmedServer, trimmedUser, trimmedPass, initialIcon, finalColor)
                            }
                        },
                        enabled = canSaveXtream,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier.weight(1.3f).height(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (canSaveXtream) {
                                        Brush.horizontalGradient(listOf(ElectricIndigo, NeonCyan))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.25f), Color.Gray.copy(alpha = 0.25f)))
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(confirmButtonText, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
