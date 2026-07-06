package com.codesrahul.streamplus.ui.local

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.codesrahul.streamplus.ui.components.PermissionBox
import com.codesrahul.streamplus.ui.components.PremiumBackground
import com.codesrahul.streamplus.ui.util.glassmorphic
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.codesrahul.streamplus.ui.settings.SettingsViewModel
import androidx.compose.ui.res.stringResource
import com.codesrahul.streamplus.R
import java.util.concurrent.TimeUnit

enum class MediaType { VIDEO, AUDIO }

data class LocalFolder(
    val name: String,
    val count: Int,
    val firstMediaUri: Uri?,
    val totalSize: Long = 0,
    val mediaType: MediaType = MediaType.VIDEO
)

data class LocalMedia(
    val id: Long,
    val title: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val uri: Uri,
    val mediaType: MediaType = MediaType.VIDEO,
    val artist: String? = null,
    val album: String? = null
)

enum class SortType {
    NAME, DATE, SIZE, COUNT
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocalScreen() {
    val permissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    )

    val viewModel: LocalViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsState(initial = com.codesrahul.streamplus.data.settings.AppSettings())
    val selectedFolder by viewModel.selectedFolder.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    PermissionBox(
        permissionState = permissionState,
        description = stringResource(R.string.local_permission_desc)
    ) {
        val currentType by viewModel.mediaType.collectAsState()
        
        Column {
            TabRow(
                selectedTabIndex = if (currentType == MediaType.VIDEO) 0 else 1,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = currentType == MediaType.VIDEO,
                    onClick = { viewModel.setMediaType(MediaType.VIDEO) },
                    text = { Text(stringResource(R.string.tab_video), fontWeight = if (currentType == MediaType.VIDEO) FontWeight.Bold else FontWeight.Medium) }
                )
                Tab(
                    selected = currentType == MediaType.AUDIO,
                    onClick = { viewModel.setMediaType(MediaType.AUDIO) },
                    text = { Text(stringResource(R.string.tab_music), fontWeight = if (currentType == MediaType.AUDIO) FontWeight.Bold else FontWeight.Medium) }
                )
            }

            if (selectedFolder != null) {
                LocalMediaList(
                    viewModel = viewModel, 
                    settings = settings,
                    onBack = { viewModel.selectFolder(null) },
                    onShowSettings = { showSettings = true }
                )
            } else {
                LocalMediaGrid(
                    viewModel = viewModel, 
                    settings = settings,
                    onShowSettings = { showSettings = true }
                )
            }
        }

        if (showSettings) {
            GallerySettingsBottomSheet(
                settings = settings,
                viewModel = settingsViewModel,
                onDismiss = { showSettings = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMediaGrid(
    viewModel: LocalViewModel,
    settings: com.codesrahul.streamplus.data.settings.AppSettings,
    onShowSettings: () -> Unit
) {
    val context = LocalContext.current
    val folders by viewModel.folders.collectAsState()
    
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedFolders = remember { mutableStateListOf<String>() }



    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.refreshAll()
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    PremiumBackground {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        windowInsets = WindowInsets(0.dp),
                        title = {
                            Column {
                                    Text(
                                        if (isSelectionMode) stringResource(R.string.selection_mode_count, selectedFolders.size) else if (viewModel.mediaType.value == MediaType.VIDEO) stringResource(R.string.gallery_title) else stringResource(R.string.music_library_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!isSelectionMode && folders.isNotEmpty()) {
                                        val countText = if (viewModel.mediaType.value == MediaType.VIDEO) stringResource(R.string.video_folders_count, folders.size) else stringResource(R.string.music_folders_count, folders.size)
                                        Text(
                                            countText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
                        actions = {
                            if (isSelectionMode) {
                                IconButton(onClick = {
                                    selectedFolders.clear()
                                    selectedFolders.addAll(folders.map { it.name })
                                }) {
                                    Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.select_all_btn))
                                }
                                IconButton(onClick = {
                                    val uris = selectedFolders.flatMap { folder ->
                                        if (viewModel.mediaType.value == MediaType.VIDEO) {
                                            getVideosInFolder(context, folder).map { it.uri }
                                        } else {
                                            getAudiosInFolder(context, folder).map { it.uri }
                                        }
                                    }
                                    val result = deleteVideos(context, uris)
                                    if (result is FileOpResult.RequiresPermission) {
                                        launcher.launch(IntentSenderRequest.Builder(result.intentSender).build())
                                    }
                                    isSelectionMode = false
                                    selectedFolders.clear()
                                    viewModel.refreshFolders()
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_menu_item), tint = MaterialTheme.colorScheme.error)
                                }
                                IconButton(onClick = {
                                    isSelectionMode = false
                                    selectedFolders.clear()
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel_btn))
                                }
                            } else {
                                IconButton(onClick = { viewModel.refreshFolders() }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh_btn))
                                }
                                IconButton(onClick = onShowSettings) {
                                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_btn))
                                }
                            }
                        }
                    )
                }
            ) { padding ->
                if (folders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val emptyIcon = if (viewModel.mediaType.value == MediaType.VIDEO) Icons.Default.VideoLibrary else Icons.Default.LibraryMusic
                            val emptyText = if (viewModel.mediaType.value == MediaType.VIDEO) stringResource(R.string.empty_videos_msg) else stringResource(R.string.empty_music_msg)
                            Icon(
                                emptyIcon,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(emptyText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = if (settings.galleryLayout == "Grid") GridCells.Adaptive(160.dp) else GridCells.Fixed(1),
                        modifier = Modifier
                            .padding(padding)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(folders, key = { it.name }, contentType = { "folder" }) { folder ->
                            val onItemClick = remember(folder, isSelectionMode, viewModel) {
                                {
                                    if (isSelectionMode) {
                                        if (selectedFolders.contains(folder.name)) {
                                            selectedFolders.remove(folder.name)
                                            if (selectedFolders.isEmpty()) {
                                                isSelectionMode = false
                                            }
                                        } else {
                                            selectedFolders.add(folder.name)
                                        }
                                    } else {
                                        viewModel.selectFolder(folder.name)
                                    }
                                    Unit
                                }
                            }
                            val onItemLongClick = remember(folder, isSelectionMode) {
                                {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedFolders.add(folder.name)
                                    }
                                    Unit
                                }
                            }
                            
                            LocalFolderItem(
                                folder = folder,
                                isSelected = selectedFolders.contains(folder.name),
                                isSelectionMode = isSelectionMode,
                                settings = settings,
                                onClick = onItemClick,
                                onLongClick = onItemLongClick
                            )
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMediaList(
    viewModel: LocalViewModel,
    settings: com.codesrahul.streamplus.data.settings.AppSettings,
    onBack: () -> Unit,
    onShowSettings: () -> Unit
) {
    val context = LocalContext.current
    val videos by viewModel.videos.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val folderName = selectedFolder ?: ""
    
    var videoToDelete by remember { mutableStateOf<LocalMedia?>(null) }
    var videoToRename by remember { mutableStateOf<LocalMedia?>(null) }
    var newName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val filteredVideos = remember(videos, searchQuery) {
        if (searchQuery.isEmpty()) videos 
        else videos.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }
    
    val currentType by viewModel.mediaType.collectAsState()
    val typeLabel = if (currentType == MediaType.VIDEO) stringResource(R.string.tab_video).lowercase() else stringResource(R.string.tab_music).lowercase()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.refreshAll()
        }
    }

    val isPitchBlack = MaterialTheme.colorScheme.surface == Color(0xFF000000)
    if (videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            tonalElevation = if (isPitchBlack) 0.dp else AlertDialogDefaults.TonalElevation,
            title = { Text(if (currentType == MediaType.VIDEO) stringResource(R.string.delete_video_title) else stringResource(R.string.delete_song_title)) },
            text = { Text(stringResource(R.string.delete_media_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    videoToDelete?.let { video ->
                        val result = deleteVideo(context, video)
                        if (result is FileOpResult.RequiresPermission) {
                            launcher.launch(IntentSenderRequest.Builder(result.intentSender).build())
                        }
                    }
                    videoToDelete = null
                    viewModel.refreshAll()
                }) { Text(stringResource(R.string.delete_menu_item), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) { Text(stringResource(R.string.cancel_btn)) }
            }
        )
    }

    if (videoToRename != null) {
        AlertDialog(
            onDismissRequest = { videoToRename = null },
            tonalElevation = if (isPitchBlack) 0.dp else AlertDialogDefaults.TonalElevation,
            title = { Text(if (currentType == MediaType.VIDEO) stringResource(R.string.rename_video_title) else stringResource(R.string.rename_song_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.new_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    videoToRename?.let { video ->
                        val result = renameVideo(context, video, newName)
                        if (result is FileOpResult.RequiresPermission) {
                            launcher.launch(IntentSenderRequest.Builder(result.intentSender).build())
                        }
                    }
                    videoToRename = null
                    viewModel.refreshAll()
                }) { Text(stringResource(R.string.rename_menu_item)) }
            },
            dismissButton = {
                TextButton(onClick = { videoToRename = null }) { Text(stringResource(R.string.cancel_btn)) }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    PremiumBackground {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                if (isSearchActive) {
                    TopAppBar(
                        windowInsets = WindowInsets(0.dp),
                        title = {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.search_videos_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)) },
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                        ),
                        scrollBehavior = scrollBehavior,
                        navigationIcon = {
                            IconButton(onClick = { 
                                isSearchActive = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close_search_desc))
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        windowInsets = WindowInsets(0.dp),
                        title = { 
                            Column {
                                Text(folderName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                if (videos.isNotEmpty()) {
                                    Text("${filteredVideos.size} $typeLabel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                        ),
                        scrollBehavior = scrollBehavior,
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_btn))
                            }
                        },
                        actions = {
                            IconButton(onClick = onShowSettings) {
                                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_btn))
                            }
                            IconButton(onClick = { viewModel.refreshFolders() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh_btn))
                            }
                        }
                    )
                }
            }
    ) { padding ->
        if (filteredVideos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    val emptyMsg = if (searchQuery.isEmpty()) {
                        if (currentType == MediaType.VIDEO) stringResource(R.string.empty_msg_no_videos) else stringResource(R.string.empty_msg_no_songs)
                    } else {
                        stringResource(R.string.empty_msg_no_results, searchQuery)
                    }
                    Text(emptyMsg, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = if (settings.galleryLayout == "Grid") GridCells.Adaptive(160.dp) else GridCells.Fixed(1),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredVideos, key = { it.id }, contentType = { "video" }) { video ->
                    val onItemClick = remember(video, context) {
                        {
                            val targetClass = if (video.mediaType == MediaType.AUDIO) {
                                com.codesrahul.streamplus.ui.player.MusicPlayerActivity::class.java
                            } else {
                                com.codesrahul.streamplus.ui.player.PlayerActivity::class.java
                            }
                            val intent = Intent(context, targetClass).apply {
                                putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_URL, video.uri.toString())
                                putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_TITLE, video.title)
                                putExtra("EXTRA_IS_AUDIO", video.mediaType == MediaType.AUDIO)
                                putExtra("EXTRA_MEDIA_TYPE", video.mediaType.name)
                                
                                val playlistUris = filteredVideos.map { it.uri.toString() }.toTypedArray()
                                val playlistTitles = filteredVideos.map { it.title }.toTypedArray()
                                val playlistArtists = filteredVideos.map { it.artist ?: "Unknown Artist" }.toTypedArray()
                                val playlistAlbums = filteredVideos.map { it.album ?: "Unknown Album" }.toTypedArray()
                                val clickedIndex = filteredVideos.indexOf(video)
                                
                                putExtra("EXTRA_PLAYLIST_URIS", playlistUris)
                                putExtra("EXTRA_PLAYLIST_TITLES", playlistTitles)
                                putExtra("EXTRA_PLAYLIST_ARTISTS", playlistArtists)
                                putExtra("EXTRA_PLAYLIST_ALBUMS", playlistAlbums)
                                putExtra("EXTRA_PLAYLIST_INDEX", clickedIndex)
                                
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        }
                    }
                    val onRenameClick = remember(video) {
                        {
                            newName = video.title
                            videoToRename = video
                        }
                    }
                    val onDeleteClick = remember(video) {
                        {
                            videoToDelete = video
                        }
                    }

                    VideoPosterItem(
                        video = video,
                        settings = settings,
                        onClick = onItemClick,
                        onRename = onRenameClick,
                        onDelete = onDeleteClick
                    )
                }
            }
        }
    }
}
}

@Composable
fun VideoPosterItem(
    video: LocalMedia,
    settings: com.codesrahul.streamplus.data.settings.AppSettings,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isGridLayout = settings.galleryLayout == "Grid"

    if (isGridLayout) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (video.mediaType == MediaType.VIDEO) 0.72f else 1f) // Square for music
                    .glassmorphic(cornerRadius = 18.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (settings.showVideoThumbnail) {
                        AsyncImage(
                            model = video.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val placeholderIcon = if (video.mediaType == MediaType.VIDEO) Icons.Default.VideoFile else Icons.Default.MusicNote
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Icon(placeholderIcon, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(0.4f))
                        }
                    }

                    // Fancy duration overlay
                    if (settings.showVideoDuration) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = formatDuration(video.duration),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Glassy more menu
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Surface(
                            onClick = { showMenu = true },
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.MoreVert, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        MaterialTheme(
                            colorScheme = MaterialTheme.colorScheme.copy(
                                surface = MaterialTheme.colorScheme.surface.copy(alpha = 1.0f)
                            )
                        ) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.rename_menu_item)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) },
                                    onClick = { showMenu = false; onRename() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_menu_item), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                                    onClick = { showMenu = false; onDelete() }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                if (video.mediaType == MediaType.AUDIO) {
                    Text(
                        text = video.artist ?: stringResource(R.string.unknown_artist),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (settings.showVideoSize) {
                        Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
                
                if (settings.showVideoSize) {
                    Text(
                        text = formatSize(video.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    } else {
        // List Layout
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .glassmorphic(cornerRadius = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp, 60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (settings.showVideoThumbnail) {
                        AsyncImage(
                            model = video.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val placeholderIcon = if (video.mediaType == MediaType.VIDEO) Icons.Default.VideoFile else Icons.Default.MusicNote
                        Icon(placeholderIcon, null, modifier = Modifier.align(Alignment.Center).size(24.dp), tint = MaterialTheme.colorScheme.primary.copy(0.4f))
                    }
                    
                    if (settings.showVideoDuration) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = formatDuration(video.duration),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (video.mediaType == MediaType.AUDIO) {
                            Text(
                                text = video.artist ?: stringResource(R.string.unknown_artist),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                        
                        if (settings.showVideoSize) {
                            Text(
                                text = formatSize(video.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (settings.showVideoSize && settings.showVideoResolution && video.mediaType == MediaType.VIDEO) {
                            Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                        
                        if (settings.showVideoResolution && video.mediaType == MediaType.VIDEO) {
                            Text(
                                text = "HD", // Generic HD for now
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null)
                }
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        surface = MaterialTheme.colorScheme.surface.copy(alpha = 1.0f)
                    )
                ) {
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.rename_menu_item)) }, onClick = { showMenu = false; onRename() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete_menu_item), color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalFolderItem(
    folder: LocalFolder,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    settings: com.codesrahul.streamplus.data.settings.AppSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isGridLayout = settings.galleryLayout == "Grid"

    if (isGridLayout) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .glassmorphic(cornerRadius = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                 else Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                ) {
                    if (folder.firstMediaUri != null) {
                        AsyncImage(
                            model = folder.firstMediaUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val folderIcon = if (folder.mediaType == MediaType.VIDEO) Icons.Filled.Folder else Icons.Filled.LibraryMusic
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(folderIcon, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        }
                    }

                    // Item count badge
                    if (settings.showFolderCount) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = folder.count.toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (settings.showFolderCount || settings.showFolderPath) {
                         Text(
                            text = buildString {
                                if (settings.showFolderCount) append("${folder.count} items ")
                                if (settings.showFolderCount && settings.showFolderPath) append("• ")
                                if (settings.showFolderPath) {
                                    val pathLabel = if (folder.mediaType == MediaType.VIDEO) "Videos" else "Music"
                                    append("/sdcard/$pathLabel/${folder.name}") 
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    } else {
        // List Layout
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .glassmorphic(cornerRadius = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                 else Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (folder.firstMediaUri != null) {
                        AsyncImage(model = folder.firstMediaUri, contentDescription = null, contentScale = ContentScale.Crop)
                    } else {
                        val folderIcon = if (folder.mediaType == MediaType.VIDEO) Icons.Default.Folder else Icons.Default.LibraryMusic
                        Icon(folderIcon, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(folder.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    if (settings.showFolderPath) {
                        val pathLabel = if (folder.mediaType == MediaType.VIDEO) "Videos" else "Music"
                        Text("/sdcard/$pathLabel/${folder.name}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (settings.showFolderCount) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ) {
                        Text(
                            folder.count.toString(), 
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GallerySettingsBottomSheet(
    settings: com.codesrahul.streamplus.data.settings.AppSettings,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isPitchBlack = MaterialTheme.colorScheme.surface == Color(0xFF000000)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = if (isPitchBlack) 0.dp else 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                stringResource(R.string.gallery_settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Layout View
            SettingsSectionHeader(stringResource(R.string.gallery_sec_layout))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GalleryOptionButton(
                    label = stringResource(R.string.gallery_layout_list),
                    isSelected = settings.galleryLayout == "List",
                    icon = Icons.AutoMirrored.Filled.List,
                    modifier = Modifier.weight(1f)
                ) { viewModel.setGalleryLayout("List") }
                GalleryOptionButton(
                    label = stringResource(R.string.gallery_layout_grid),
                    isSelected = settings.galleryLayout == "Grid",
                    icon = Icons.Default.GridView,
                    modifier = Modifier.weight(1f)
                ) { viewModel.setGalleryLayout("Grid") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Folder Sort
            SettingsSectionHeader(stringResource(R.string.gallery_sec_folder_sort))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GalleryChip(stringResource(R.string.gallery_sort_name), settings.folderSortField == "Name") { viewModel.setFolderSortField("Name") }
                    GalleryChip(stringResource(R.string.gallery_sort_count), settings.folderSortField == "Count") { viewModel.setFolderSortField("Count") }
                }
                IconButton(
                    onClick = { viewModel.setFolderSortAsc(!settings.folderSortAsc) },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (settings.folderSortAsc) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Video Sort
            SettingsSectionHeader(stringResource(R.string.gallery_sec_video_sort))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GalleryChip(stringResource(R.string.gallery_sort_name), settings.videoSortField == "Name", Modifier.weight(1f)) { viewModel.setVideoSortField("Name") }
                    GalleryChip(stringResource(R.string.gallery_sort_duration), settings.videoSortField == "Duration", Modifier.weight(1f)) { viewModel.setVideoSortField("Duration") }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GalleryChip(stringResource(R.string.gallery_sort_size), settings.videoSortField == "Size") { viewModel.setVideoSortField("Size") }
                        GalleryChip(stringResource(R.string.gallery_sort_date), settings.videoSortField == "Date") { viewModel.setVideoSortField("Date") }
                    }
                    IconButton(
                        onClick = { viewModel.setVideoSortAsc(!settings.videoSortAsc) },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (settings.videoSortAsc) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Folder Display
            SettingsSectionHeader(stringResource(R.string.gallery_sec_folder_display))
            GallerySwitchItem(stringResource(R.string.gallery_show_path), settings.showFolderPath) { viewModel.setShowFolderPath(it) }
            GallerySwitchItem(stringResource(R.string.gallery_show_count), settings.showFolderCount) { viewModel.setShowFolderCount(it) }

            Spacer(modifier = Modifier.height(24.dp))

            // Video Display
            SettingsSectionHeader(stringResource(R.string.gallery_sec_video_display))
            GallerySwitchItem(stringResource(R.string.gallery_show_thumbnail), settings.showVideoThumbnail) { viewModel.setShowVideoThumbnail(it) }
            GallerySwitchItem(stringResource(R.string.gallery_show_resolution), settings.showVideoResolution) { viewModel.setShowVideoResolution(it) }
            GallerySwitchItem(stringResource(R.string.gallery_show_duration), settings.showVideoDuration) { viewModel.setShowVideoDuration(it) }
            GallerySwitchItem(stringResource(R.string.gallery_show_size), settings.showVideoSize) { viewModel.setShowVideoSize(it) }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun GalleryOptionButton(label: String, isSelected: Boolean, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
fun GalleryChip(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(label, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GallerySwitchItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

fun formatDuration(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%02d:%02d", minutes, seconds)
}

fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}

fun getAudiosInFolder(context: Context, folderName: String): List<LocalMedia> {
    val audios = mutableListOf<LocalMedia>()
    val selection = if (folderName == "All Music") null else "${MediaStore.Audio.Media.BUCKET_DISPLAY_NAME} = ?"
    val selectionArgs = if (folderName == "All Music") null else arrayOf(folderName)

    try {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM
        )
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(nameCol) ?: "Unknown"
                val path = cursor.getString(dataCol) ?: ""
                val duration = cursor.getLong(durationCol)
                val size = cursor.getLong(sizeCol)
                val date = cursor.getLong(dateCol)
                val artist = cursor.getString(artistCol)
                val album = cursor.getString(albumCol)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                
                audios.add(LocalMedia(id, title, path, duration, size, date, uri, MediaType.AUDIO, artist, album))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return audios
}

fun getAudioFolders(context: Context): List<LocalFolder> {
    val folderMap = mutableMapOf<String, MutableList<Long>>()
    val sizeMap = mutableMapOf<String, Long>()
    val projection = arrayOf(
        MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.SIZE
    )
    
    try {
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null, null
        )?.use { cursor ->
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            
            while (cursor.moveToNext()) {
                val bucketName = cursor.getString(bucketCol) ?: "Internal Storage"
                val id = cursor.getLong(idCol)
                val size = cursor.getLong(sizeCol)
                
                folderMap.getOrPut(bucketName) { mutableListOf() }.add(id)
                sizeMap[bucketName] = sizeMap.getOrDefault(bucketName, 0L) + size
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    val folders = folderMap.map { (name, ids) ->
        val firstUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ids.first())
        LocalFolder(name, ids.size, firstUri, sizeMap[name] ?: 0L, MediaType.AUDIO)
    }.toMutableList()
    
    val totalCount = folders.sumOf { it.count }
    if (totalCount > 0) {
        val firstUri = folders.firstOrNull { it.firstMediaUri != null }?.firstMediaUri
        folders.add(0, LocalFolder("All Music", totalCount, firstUri, folders.sumOf { it.totalSize }, MediaType.AUDIO))
    }
    
    return folders
}

fun deleteVideo(context: Context, video: LocalMedia): FileOpResult {
    return deleteVideos(context, listOf(video.uri))
}

fun deleteVideos(context: Context, uris: List<Uri>): FileOpResult {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
            FileOpResult.RequiresPermission(pendingIntent.intentSender)
        } else {
            var deletedTotal = 0
            uris.forEach { uri ->
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) deletedTotal++
            }
            if (deletedTotal > 0) FileOpResult.Success else FileOpResult.Error("Failed to delete")
        }
    } catch (e: Exception) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
            FileOpResult.RequiresPermission(e.userAction.actionIntent.intentSender)
        } else {
            FileOpResult.Error(e.message)
        }
    }
}

fun renameVideo(context: Context, video: LocalMedia, newName: String): FileOpResult {
    val values = ContentValues().apply {
        val colName = if (video.mediaType == MediaType.VIDEO) MediaStore.Video.Media.DISPLAY_NAME else MediaStore.Audio.Media.DISPLAY_NAME
        put(colName, newName)
    }
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createWriteRequest(context.contentResolver, listOf(video.uri))
            FileOpResult.RequiresPermission(pendingIntent.intentSender)
        } else {
            val rows = context.contentResolver.update(video.uri, values, null, null)
            if (rows > 0) FileOpResult.Success else FileOpResult.Error("Failed to rename")
        }
    } catch (e: Exception) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
            FileOpResult.RequiresPermission(e.userAction.actionIntent.intentSender)
        } else {
            FileOpResult.Error(e.message)
        }
    }
}

fun getVideosInFolder(context: Context, folderName: String): List<LocalMedia> {
    val videos = mutableListOf<LocalMedia>()
    val selection = if (folderName == "All Videos") null else "${MediaStore.Video.Media.BUCKET_DISPLAY_NAME} = ?"
    val selectionArgs = if (folderName == "All Videos") null else arrayOf(folderName)

    try {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(nameCol) ?: "Unknown"
                val path = cursor.getString(dataCol) ?: ""
                val duration = cursor.getLong(durationCol)
                val size = cursor.getLong(sizeCol)
                val date = cursor.getLong(dateCol)
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                
                videos.add(LocalMedia(id, title, path, duration, size, date, uri, MediaType.VIDEO))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return videos
}

fun getLocalFolders(context: Context): List<LocalFolder> {
    val folderMap = mutableMapOf<String, MutableList<Long>>()
    val sizeMap = mutableMapOf<String, Long>()
    val projection = arrayOf(
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.SIZE
    )
    
    try {
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null, null
        )?.use { cursor ->
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            
            while (cursor.moveToNext()) {
                val bucketName = cursor.getString(bucketCol) ?: "Internal Storage"
                val id = cursor.getLong(idCol)
                val size = cursor.getLong(sizeCol)
                
                folderMap.getOrPut(bucketName) { mutableListOf() }.add(id)
                sizeMap[bucketName] = sizeMap.getOrDefault(bucketName, 0L) + size
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    val folders = folderMap.map { (name, ids) ->
        val firstUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, ids.first())
        LocalFolder(name, ids.size, firstUri, sizeMap[name] ?: 0L, MediaType.VIDEO)
    }.toMutableList()
    
    val totalCount = folders.sumOf { it.count }
    if (totalCount > 0) {
        val firstUri = folders.firstOrNull { it.firstMediaUri != null }?.firstMediaUri
        folders.add(0, LocalFolder("All Videos", totalCount, firstUri, folders.sumOf { it.totalSize }, MediaType.VIDEO))
    }
    
    return folders
}

sealed class FileOpResult {
    object Success : FileOpResult()
    data class RequiresPermission(val intentSender: IntentSender) : FileOpResult()
    data class Error(val message: String?) : FileOpResult()
}
