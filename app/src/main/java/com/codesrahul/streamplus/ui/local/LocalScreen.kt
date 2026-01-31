package com.codesrahul.streamplus.ui.local

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codesrahul.streamplus.ui.components.PermissionBox
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    PermissionBox(
        permissionState = permissionState,
        description = "Permission needed to load local videos"
    ) {
        LocalVideoGrid()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalVideoGrid() {
    val context = LocalContext.current
    var folders by remember { mutableStateOf<List<VideoFolder>>(emptyList()) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedFolders = remember { mutableStateListOf<String>() }
    var sortType by remember { mutableStateOf(SortType.DATE) }
    var showSortMenu by remember { mutableStateOf(false) }

    fun refreshFolders() {
        folders = getVideoFolders(context).let { list ->
            when (sortType) {
                SortType.NAME -> list.sortedBy { it.name }
                SortType.DATE -> list // date sorting not directly in folder, could query latest
                SortType.SIZE -> list.sortedByDescending { it.totalSize }
                SortType.COUNT -> list.sortedByDescending { it.count }
            }
        }
    }

    LaunchedEffect(sortType) {
        refreshFolders()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refreshFolders()
        }
    }

    if (selectedFolder != null) {
        LocalVideoList(folderName = selectedFolder!!, onBack = { selectedFolder = null })
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isSelectionMode) "${selectedFolders.size} Selected" else "Local Videos") },
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
                                refreshFolders()
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
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Filled.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                DropdownMenuItem(text = { Text("Name") }, onClick = { sortType = SortType.NAME; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Count") }, onClick = { sortType = SortType.COUNT; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Size") }, onClick = { sortType = SortType.SIZE; showSortMenu = false })
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (folders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No videos found", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(folders, key = { it.name }) { folder ->
                        VideoFolderItem(
                            folder = folder,
                            isSelected = selectedFolders.contains(folder.name),
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    if (selectedFolders.contains(folder.name)) {
                                        selectedFolders.remove(folder.name)
                                        if (selectedFolders.isEmpty()) isSelectionMode = false
                                    } else {
                                        selectedFolders.add(folder.name)
                                    }
                                } else {
                                    selectedFolder = folder.name
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
fun LocalVideoList(folderName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var videos by remember { mutableStateOf<List<LocalVideo>>(emptyList()) }
    var videoToDelete by remember { mutableStateOf<LocalVideo?>(null) }
    var videoToRename by remember { mutableStateOf<LocalVideo?>(null) }
    var newName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val filteredVideos = remember(videos, searchQuery) {
        if (searchQuery.isEmpty()) videos 
        else videos.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    fun refresh() {
        videos = getVideosInFolder(context, folderName)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refresh()
        }
    }

    LaunchedEffect(folderName) {
        refresh()
    }

    if (videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Delete Video") },
            text = { Text("Are you sure you want to delete '${videoToDelete?.title}'?") },
            confirmButton = {
                TextButton(onClick = {
                    val result = deleteVideo(context, videoToDelete!!)
                    if (result is FileOpResult.RequiresPermission) {
                        launcher.launch(IntentSenderRequest.Builder(result.intentSender).build())
                    }
                    videoToDelete = null
                    refresh()
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
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val result = renameVideo(context, videoToRename!!, newName)
                    if (result is FileOpResult.RequiresPermission) {
                        launcher.launch(IntentSenderRequest.Builder(result.intentSender).build())
                    }
                    videoToRename = null
                    refresh()
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { videoToRename = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search videos...") },
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                            singleLine = true
                        )
                    },
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
                    title = { Text(folderName) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { refresh() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        }
    ) { padding ->
        if (filteredVideos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(if (searchQuery.isEmpty()) "No videos found" else "No results for '$searchQuery'")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredVideos, key = { it.id }) { video ->
                    VideoPosterItem(
                        video = video,
                        onClick = {
                            val intent = android.content.Intent(context, com.codesrahul.streamplus.ui.player.PlayerActivity::class.java).apply {
                                putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_URL, video.path)
                                putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_TITLE, video.title)
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

@Composable
fun VideoPosterItem(
    video: LocalVideo,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f) // Movie poster aspect ratio
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = video.uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Duration Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(video.duration),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // More Menu Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            ) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRename() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            fontWeight = FontWeight.SemiBold,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatSize(video.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$bytes B"
    }
}

sealed class FileOpResult {
    object Success : FileOpResult()
    data class RequiresPermission(val intentSender: IntentSender) : FileOpResult()
    data class Error(val message: String?) : FileOpResult()
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoFolderItem(
    folder: VideoFolder,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
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
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Folder, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = folder.count.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    textAlign = TextAlign.Start
                )
                if (folder.totalSize > 0) {
                    Text(
                        text = formatSize(folder.totalSize),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
