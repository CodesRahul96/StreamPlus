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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.codesrahul.streamplus.ui.settings.SettingsViewModel
import java.util.concurrent.TimeUnit

data class VideoFolder(
    val name: String,
    val count: Int,
    val firstVideoUri: Uri?,
    val totalSize: Long = 0
)

data class LocalVideo(
    val id: Long,
    val title: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val uri: Uri
)

enum class SortType {
    NAME, DATE, SIZE, COUNT
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocalScreen() {
    val permissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    val viewModel: LocalViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsState(initial = com.codesrahul.streamplus.data.settings.AppSettings())
    val selectedFolder by viewModel.selectedFolder.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    PermissionBox(
        permissionState = permissionState,
        description = "Permission needed to load local videos"
    ) {
        if (selectedFolder != null) {
            LocalVideoList(
                viewModel = viewModel, 
                settingsViewModel = settingsViewModel,
                settings = settings,
                onBack = { viewModel.selectFolder(null) },
                onShowSettings = { showSettings = true }
            )
        } else {
            LocalVideoGrid(
                viewModel = viewModel, 
                settingsViewModel = settingsViewModel,
                settings = settings,
                onShowSettings = { showSettings = true }
            )
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
fun LocalVideoGrid(
    viewModel: LocalViewModel,
    settingsViewModel: SettingsViewModel,
    settings: com.codesrahul.streamplus.data.settings.AppSettings,
    onShowSettings: () -> Unit
) {
    val context = LocalContext.current
    val folders by viewModel.folders.collectAsState()
    
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedFolders = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        viewModel.refreshFolders()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.refreshFolders()
        }
    }

    PremiumBackground {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    if (isSelectionMode) "${selectedFolders.size} Selected" else "Gallery",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!isSelectionMode && folders.isNotEmpty()) {
                                    Text(
                                        "${folders.size} folders",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        actions = {
                            if (isSelectionMode) {
                                IconButton(onClick = {
                                    selectedFolders.clear()
                                    selectedFolders.addAll(folders.map { it.name })
                                }) {
                                    Icon(Icons.Filled.SelectAll, contentDescription = "Select All")
                                }
                                IconButton(onClick = {
                                    val uris = selectedFolders.flatMap { folder ->
                                        getVideosInFolder(context, folder).map { it.uri }
                                    }
                                    val result = deleteVideos(context, uris)
                                    if (result is FileOpResult.RequiresPermission) {
                                        launcher.launch(IntentSenderRequest.Builder(result.intentSender).build())
                                    }
                                    isSelectionMode = false
                                    selectedFolders.clear()
                                    viewModel.refreshFolders()
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                                IconButton(onClick = {
                                    isSelectionMode = false
                                    selectedFolders.clear()
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Cancel")
                                }
                            } else {
                                IconButton(onClick = onShowSettings) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                                }
                            }
                        }
                    )
                }
            ) { padding ->
                if (folders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No videos found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        items(folders, key = { it.name }) { folder ->
                            VideoFolderItem(
                                folder = folder,
                                isSelected = selectedFolders.contains(folder.name),
                                isSelectionMode = isSelectionMode,
                                settings = settings,
                                onClick = {
                                    if (isSelectionMode) {
                                        if (selectedFolders.contains(folder.name)) {
                                            selectedFolders.remove(folder.name)
                                            if (selectedFolders.isEmpty()) isSelectionMode = false
                                        } else {
                                            selectedFolders.add(folder.name)
                                        }
                                    } else {
                                        viewModel.selectFolder(folder.name)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedFolders.add(folder.name)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalVideoList(
    viewModel: LocalViewModel,
    settingsViewModel: SettingsViewModel,
    settings: com.codesrahul.streamplus.data.settings.AppSettings,
    onBack: () -> Unit,
    onShowSettings: () -> Unit
) {
    val context = LocalContext.current
    val videos by viewModel.videos.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val folderName = selectedFolder ?: ""
    val isLoading by viewModel.isLoading.collectAsState()
    
    var videoToDelete by remember { mutableStateOf<LocalVideo?>(null) }
    var videoToRename by remember { mutableStateOf<LocalVideo?>(null) }
    var newName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val filteredVideos = remember(videos, searchQuery) {
        if (searchQuery.isEmpty()) videos 
        else videos.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.selectFolder(folderName)
        }
    }

    if (videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Delete Video") },
            text = { Text("Are you sure you want to delete \"${videoToDelete?.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    videoToDelete?.let { video ->
                        val result = deleteVideo(context, video)
                        if (result is FileOpResult.RequiresPermission) {
                            launcher.launch(IntentSenderRequest.Builder(result.intentSender).build())
                        }
                    }
                    videoToDelete = null
                    viewModel.selectFolder(folderName)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (videoToRename != null) {
        AlertDialog(
            onDismissRequest = { videoToRename = null },
            title = { Text("Rename Video") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
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
                    viewModel.selectFolder(folderName)
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { videoToRename = null }) { Text("Cancel") }
            }
        )
    }

    PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (isSearchActive) {
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search videos...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)) },
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
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        navigationIcon = {
                            IconButton(onClick = { 
                                isSearchActive = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close Search")
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = { 
                            Column {
                                Text(folderName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                if (videos.isNotEmpty()) {
                                    Text("${filteredVideos.size} videos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = onShowSettings) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings")
                            }
                            IconButton(onClick = { viewModel.refreshFolders() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
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
                    Text(if (searchQuery.isEmpty()) "No videos found" else "No results for \"$searchQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                items(filteredVideos, key = { it.id }) { video ->
                    VideoPosterItem(
                        video = video,
                        settings = settings,
                        onClick = {
                            val intent = Intent(context, com.codesrahul.streamplus.ui.player.PlayerActivity::class.java).apply {
                                putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_URL, video.uri.toString())
                                putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_TITLE, video.title)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        },
                        onRename = {
                            newName = video.title
                            videoToRename = video
                        },
                        onDelete = {
                            videoToDelete = video
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
fun VideoPosterItem(
    video: LocalVideo,
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
                    .aspectRatio(0.72f), // Movie poster style
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.VideoFile, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(0.4f))
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
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; onRename() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; onDelete() }
                            )
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
            if (settings.showVideoSize) {
                Text(
                    text = formatSize(video.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    } else {
        // List Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
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
                    Icon(Icons.Default.VideoFile, null, modifier = Modifier.align(Alignment.Center).size(24.dp), tint = MaterialTheme.colorScheme.primary.copy(0.4f))
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
                    if (settings.showVideoSize) {
                        Text(
                            text = formatSize(video.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (settings.showVideoSize && settings.showVideoResolution) {
                        Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (settings.showVideoResolution) {
                        Text(
                            text = "1080p", // Mock resolution
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, null)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRename() })
                DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoFolderItem(
    folder: VideoFolder,
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
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                ) {
                    if (folder.firstVideoUri != null) {
                        AsyncImage(
                            model = folder.firstVideoUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
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
                            Icon(Icons.Filled.Folder, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
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
                                if (settings.showFolderPath) append("/sdcard/Videos/${folder.name}") // Mock path
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (folder.firstVideoUri != null) {
                    AsyncImage(model = folder.firstVideoUri, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(folder.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                if (settings.showFolderPath) {
                    Text("/sdcard/Videos/${folder.name}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GallerySettingsBottomSheet(
    settings: com.codesrahul.streamplus.data.settings.AppSettings,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1217), // Deep dark background
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(0.2f)) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Gallery Settings", 
                style = MaterialTheme.typography.headlineSmall, 
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Layout View
            SettingsSectionHeader("Layout View")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GalleryOptionButton(
                    label = "List", 
                    isSelected = settings.galleryLayout == "List", 
                    icon = Icons.Default.List,
                    modifier = Modifier.weight(1f)
                ) { viewModel.setGalleryLayout("List") }
                GalleryOptionButton(
                    label = "Grid", 
                    isSelected = settings.galleryLayout == "Grid", 
                    icon = Icons.Default.GridView,
                    modifier = Modifier.weight(1f)
                ) { viewModel.setGalleryLayout("Grid") }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Folder Sort
            SettingsSectionHeader("Folder Sort")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GalleryChip("Name", settings.folderSortField == "Name") { viewModel.setFolderSortField("Name") }
                    GalleryChip("Count", settings.folderSortField == "Count") { viewModel.setFolderSortField("Count") }
                }
                IconButton(
                    onClick = { viewModel.setFolderSortAsc(!settings.folderSortAsc) },
                    modifier = Modifier.background(Color.White.copy(0.05f), CircleShape).size(36.dp)
                ) {
                    Icon(
                         imageVector = if (settings.folderSortAsc) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                         contentDescription = null,
                         tint = Color(0xFF00E5FF),
                         modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Video Sort
            SettingsSectionHeader("Video Sort")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GalleryChip("Name", settings.videoSortField == "Name", Modifier.weight(1f)) { viewModel.setVideoSortField("Name") }
                    GalleryChip("Duration", settings.videoSortField == "Duration", Modifier.weight(1f)) { viewModel.setVideoSortField("Duration") }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GalleryChip("Size", settings.videoSortField == "Size") { viewModel.setVideoSortField("Size") }
                        GalleryChip("Date", settings.videoSortField == "Date") { viewModel.setVideoSortField("Date") }
                    }
                    IconButton(
                        onClick = { viewModel.setVideoSortAsc(!settings.videoSortAsc) },
                        modifier = Modifier.background(Color.White.copy(0.05f), CircleShape).size(36.dp)
                    ) {
                        Icon(
                             imageVector = if (settings.videoSortAsc) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                             contentDescription = null,
                             tint = Color(0xFF00E5FF),
                             modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Folder Display
            SettingsSectionHeader("Folder Display")
            GallerySwitchItem("Show Path", settings.showFolderPath) { viewModel.setShowFolderPath(it) }
            GallerySwitchItem("Show Count", settings.showFolderCount) { viewModel.setShowFolderCount(it) }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Video Display
            SettingsSectionHeader("Video Display")
            GallerySwitchItem("Thumbnail", settings.showVideoThumbnail) { viewModel.setShowVideoThumbnail(it) }
            GallerySwitchItem("Show Resolution", settings.showVideoResolution) { viewModel.setShowVideoResolution(it) }
            GallerySwitchItem("Show Duration", settings.showVideoDuration) { viewModel.setShowVideoDuration(it) }
            GallerySwitchItem("Show File Size", settings.showVideoSize) { viewModel.setShowVideoSize(it) }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        title, 
        style = MaterialTheme.typography.labelLarge, 
        color = Color.White.copy(0.6f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun GalleryOptionButton(label: String, isSelected: Boolean, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF00E5FF).copy(0.15f) else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color.White.copy(0.1f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (isSelected) Color(0xFF00E5FF) else Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = if (isSelected) Color(0xFF00E5FF) else Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
fun GalleryChip(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(0.05f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(label, color = if (isSelected) Color.Black else Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GallerySwitchItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00E5FF),
                checkedTrackColor = Color(0xFF00E5FF).copy(0.3f),
                uncheckedThumbColor = Color.White.copy(0.6f),
                uncheckedTrackColor = Color.White.copy(0.1f)
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

fun deleteVideo(context: Context, video: LocalVideo): FileOpResult {
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

fun renameVideo(context: Context, video: LocalVideo, newName: String): FileOpResult {
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, newName)
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

fun getVideosInFolder(context: Context, folderName: String): List<LocalVideo> {
    val videos = mutableListOf<LocalVideo>()
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
                
                videos.add(LocalVideo(id, title, path, duration, size, date, uri))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return videos
}

fun getVideoFolders(context: Context): List<VideoFolder> {
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
        VideoFolder(name, ids.size, firstUri, sizeMap[name] ?: 0L)
    }.toMutableList()
    
    val totalCount = folders.sumOf { it.count }
    if (totalCount > 0) {
        val firstUri = folders.firstOrNull { it.firstVideoUri != null }?.firstVideoUri
        folders.add(0, VideoFolder("All Videos", totalCount, firstUri, folders.sumOf { it.totalSize }))
    }
    
    return folders
}

sealed class FileOpResult {
    object Success : FileOpResult()
    data class RequiresPermission(val intentSender: IntentSender) : FileOpResult()
    data class Error(val message: String?) : FileOpResult()
}
