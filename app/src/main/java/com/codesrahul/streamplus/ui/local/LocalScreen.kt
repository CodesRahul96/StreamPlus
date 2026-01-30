package com.codesrahul.streamplus.ui.local

import android.Manifest
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codesrahul.streamplus.ui.components.PermissionBox
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

data class VideoFolder(val name: String, val count: Int)

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

    LaunchedEffect(Unit) {
        val allFolders = getVideoFolders(context)
        val totalCount = allFolders.sumOf { it.count }
        folders = if (totalCount > 0) listOf(VideoFolder("All Videos", totalCount)) + allFolders else allFolders
    }

    if (selectedFolder != null) {
        LocalVideoList(folderName = selectedFolder!!, onBack = { selectedFolder = null })
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Local Videos") }) }
        ) { padding ->
            if (folders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No videos found")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.padding(padding).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folders, key = { it.name }) { folder ->
                        VideoFolderItem(folder) { selectedFolder = folder.name }
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
                    deleteVideo(context, videoToDelete!!)
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
                    renameVideo(context, videoToRename!!, newName)
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
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(filteredVideos, key = { it.id }) { video ->
                    var showMenu by remember { mutableStateOf(false) }
                    
                    ListItem(
                        headlineContent = { Text(video.title) },
                        supportingContent = { Text(video.path) },
                        leadingContent = { Icon(Icons.Filled.VideoLibrary, null) },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        onClick = {
                                            showMenu = false
                                            newName = video.title
                                            videoToRename = video
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            showMenu = false
                                            videoToDelete = video
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            val intent = android.content.Intent(context, com.codesrahul.streamplus.ui.player.PlayerActivity::class.java).apply {
                                putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_URL, video.path)
                                putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_TITLE, video.title)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

data class LocalVideo(val id: Long, val title: String, val path: String)

fun deleteVideo(context: Context, video: LocalVideo) {
    try {
        val uri = android.content.ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.id)
        context.contentResolver.delete(uri, null, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun renameVideo(context: Context, video: LocalVideo, newName: String) {
    try {
        val uri = android.content.ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.id)
        val values = android.content.ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, newName)
        }
        context.contentResolver.update(uri, values, null, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun VideoFolderItem(folder: VideoFolder, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = "${folder.count} Videos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getVideosInFolder(context: Context, folderName: String): List<LocalVideo> {
    val videos = mutableListOf<LocalVideo>()
    val selection = if (folderName == "All Videos") null else "${MediaStore.Video.Media.BUCKET_DISPLAY_NAME} = ?"
    val selectionArgs = if (folderName == "All Videos") null else arrayOf(folderName)

    try {
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DATA),
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(nameCol) ?: "Unknown"
                val path = cursor.getString(dataCol) ?: ""
                videos.add(LocalVideo(id, title, path))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return videos
}

fun getVideoFolders(context: Context): List<VideoFolder> {
    val folders = mutableMapOf<String, Int>()
    val projection = arrayOf(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
    
    try {
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val bucketName = cursor.getString(bucketCol) ?: "Unknown"
                folders[bucketName] = folders.getOrDefault(bucketName, 0) + 1
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return folders.map { VideoFolder(it.key, it.value) }
}
