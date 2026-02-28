package com.codesrahul.streamplus.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.codesrahul.streamplus.data.local.FavoriteEntity
import com.codesrahul.streamplus.data.local.PlaylistEntity
import com.codesrahul.streamplus.data.local.HistoryEntity
import com.codesrahul.streamplus.data.settings.AppSettings
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.ui.components.PremiumBackground
import com.codesrahul.streamplus.ui.settings.SettingsViewModel
import com.codesrahul.streamplus.util.UserAgentManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlaylistScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onPlaylistClick: (Int) -> Unit,
    onChannelClick: (M3UItem) -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<PlaylistEntity?>(null) }
    val playlists by viewModel.playlists.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recentlyWatched by viewModel.recentlyWatched.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val context = LocalContext.current
    // Always hoist SettingsViewModel at top-level — never inside a conditional branch
    val settingsState by hiltViewModel<SettingsViewModel>().settings.collectAsState(initial = AppSettings())
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.importLocalPlaylist(context, it) }
    }

    PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Playlists", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (playlists.isNotEmpty()) {
                                Text("${playlists.size} sources", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add URL") },
                                onClick = {
                                    showMenu = false
                                    showAddDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.AddLink, null, modifier = Modifier.size(20.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Import M3U File") },
                                onClick = {
                                    showMenu = false
                                    filePickerLauncher.launch("*/*")
                                },
                                leadingIcon = { Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(20.dp)) }
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                if (playlists.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddDialog = true },
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text("New Playlist") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        ) { padding ->
            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier.padding(padding).fillMaxSize(),
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp).fillMaxWidth(0.7f)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Playlist", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val sortedPlaylists = remember(playlists, settingsState.playlistSortOrder) {
                    when (settingsState.playlistSortOrder) {
                        "Name"       -> playlists.sortedBy { it.name }
                        "Category"   -> playlists.sortedBy { it.icon }
                        "Added Date" -> playlists.sortedByDescending { it.id }
                        else         -> playlists
                    }
                }
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // --- Global Search Bar ---
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search all playlists…") },
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

                    // --- Search Results ---
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
                            items(searchResults, key = { it.url }) { channel ->
                                SearchResultItem(channel = channel, onClick = { onChannelClick(channel) })
                            }
                        }
                        return@LazyColumn
                    }

                    // --- Recently Watched Carousel ---
                    if (recentlyWatched.isNotEmpty()) {
                        item {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Recently Watched", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(end = 16.dp)
                                ) {
                                    items(recentlyWatched, key = { it.url }) { entry ->
                                        RecentlyWatchedCard(
                                            entry = entry,
                                            onClick = { onChannelClick(
                                                M3UItem(
                                                    name = entry.name,
                                                    url = entry.url,
                                                    logoUrl = entry.logoUrl ?: "",
                                                    group = entry.group ?: "",
                                                    userAgent = entry.userAgent ?: ""
                                                )
                                            )}
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- Favourites Carousel ---
                    if (favorites.isNotEmpty()) {
                        item {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Favourites",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        "${favorites.size} channels",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(end = 16.dp)
                                ) {
                                    items(favorites, key = { it.url }) { fav ->
                                        with(sharedTransitionScope) {
                                            FavouriteChannelCard(
                                                favorite = fav,
                                                modifier = Modifier.sharedElement(
                                                    rememberSharedContentState(key = "fav_${fav.url}"),
                                                    animatedVisibilityScope = animatedContentScope
                                                ),
                                                onClick = {
                                                    onChannelClick(
                                                        M3UItem(
                                                            name = fav.name,
                                                            url = fav.url,
                                                            logoUrl = fav.logoUrl,
                                                            group = fav.groupTitle
                                                        )
                                                    )
                                                },
                                                onRemove = { viewModel.toggleFavorite(
                                                    M3UItem(name = fav.name, url = fav.url, logoUrl = fav.logoUrl, group = fav.groupTitle),
                                                    fav.playlistId
                                                )}
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- Playlist list ---
                    items(sortedPlaylists, key = { it.id }) { playlist ->
                        with(sharedTransitionScope) {
                            PlaylistItem(
                                playlist = playlist,
                                settingsState = settingsState,
                                modifier = Modifier.sharedElement(
                                    rememberSharedContentState(key = "playlist_${playlist.id}"),
                                    animatedVisibilityScope = animatedContentScope
                                ),
                                onClick = { onPlaylistClick(playlist.id) },
                            onEdit = { showEditDialog = playlist }
                        )
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
            onConfirm = { name, url, userAgent, icon, color ->
                viewModel.addPlaylist(name, url, userAgent, icon, color)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    showEditDialog?.let { playlist ->
        PlaylistDialog(
            title = "Edit Playlist",
            initialName = playlist.name,
            initialUrl = playlist.url,
            initialUserAgent = playlist.userAgent,
            initialIcon = playlist.icon,
            initialColor = playlist.color,
            confirmButtonText = "Update",
            onConfirm = { name, url, userAgent, icon, color ->
                viewModel.updatePlaylist(playlist.copy(name = name, url = url, userAgent = userAgent, icon = icon, color = color))
                showEditDialog = null
            },
            onDismiss = { showEditDialog = null },
            onDelete = {
                viewModel.deletePlaylist(playlist)
                showEditDialog = null
            }
        )
    }
}

@Composable
fun PlaylistItem(
    playlist: PlaylistEntity,
    settingsState: AppSettings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val hideUri = settingsState.hidePlaylistUri
    val resolvedIcon = if (playlist.icon == "default") settingsState.defaultPlaylistIcon.lowercase() else playlist.icon.lowercase()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored icon box
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (playlist.color != -16777216) Color(playlist.color).copy(alpha = 0.15f)
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
                    tint = if (playlist.color != -16777216) Color(playlist.color) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!hideUri) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = playlist.url,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
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
    confirmButtonText: String,
    onConfirm: (String, String, String, String, Int) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }
    var icon by remember { mutableStateOf(initialIcon) }
    var color by remember { mutableStateOf(initialColor) }
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

    if (showCustomUADialog) {
        AlertDialog(
            onDismissRequest = { showCustomUADialog = false },
            title = { Text("Custom User Agent") },
            text = {
                OutlinedTextField(
                    value = customUAInput,
                    onValueChange = { customUAInput = it },
                    label = { Text("UA String") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    userAgentValue = customUAInput.ifBlank { UserAgentManager.DEFAULT_UA }
                    userAgentDisplay = "Custom"
                    showCustomUADialog = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomUADialog = false }) { Text("Cancel") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (onDelete != null) Icons.Default.Edit else Icons.Default.AddCircleOutline,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; isNameModified = true },
                    label = { Text("Playlist Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { newUrl ->
                        url = newUrl
                        if (!isNameModified && newUrl.isNotEmpty()) {
                            try {
                                val uri = android.net.Uri.parse(newUrl)
                                val lastSegment = uri.lastPathSegment
                                if (!lastSegment.isNullOrEmpty()) {
                                    var fileName = lastSegment
                                    val dotIndex = fileName.lastIndexOf('.')
                                    if (dotIndex != -1) fileName = fileName.substring(0, dotIndex)
                                    fileName = fileName.replace(Regex("[_\\-]"), " ")
                                        .split(" ").joinToString(" ") { word ->
                                            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                                        }.trim()
                                    if (fileName.isNotEmpty()) name = fileName
                                }
                            } catch (e: Exception) {}
                        }
                    },
                    label = { Text("M3U URL or JSON Path") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(20.dp)) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = isUserAgentExpanded,
                    onExpandedChange = { isUserAgentExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = userAgentDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("User Agent") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUserAgentExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isUserAgentExpanded,
                        onDismissRequest = { isUserAgentExpanded = false }
                    ) {
                        userAgentOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
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
                
                Spacer(modifier = Modifier.height(20.dp))
                Text("Icon & Theme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(icons) { iconKey ->
                        val isSelected = icon == iconKey
                        Surface(
                            onClick = { icon = iconKey },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(44.dp)
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
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
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
                                .clickable { color = colorInt }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && url.isNotBlank()) onConfirm(name, url, userAgentValue, icon, color) },
                enabled = name.isNotBlank() && url.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
            ) { Text(confirmButtonText, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}

@Composable
fun SearchResultItem(channel: M3UItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
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
                Text(channel.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (channel.group.isNotBlank()) {
                    Text(channel.group, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun RecentlyWatchedCard(entry: com.codesrahul.streamplus.data.local.HistoryEntity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(140.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(52.dp)) {
                if (!entry.logoUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = entry.logoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(entry.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun FavouriteChannelCard(
    favorite: FavoriteEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = modifier.width(130.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Box {
            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    modifier = Modifier.size(52.dp)
                ) {
                    if (favorite.logoUrl.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = favorite.logoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(26.dp))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    favorite.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            // Remove favourite button (top-end)
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp).padding(4.dp)
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Remove from favourites",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
