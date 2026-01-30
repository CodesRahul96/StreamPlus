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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    items(folders) { folder ->
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

    LaunchedEffect(folderName) {
        videos = getVideosInFolder(context, folderName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(videos) { video ->
                ListItem(
                    headlineContent = { Text(video.title) },
                    supportingContent = { Text(video.path) },
                    leadingContent = { Icon(Icons.Filled.VideoLibrary, null) },
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

data class LocalVideo(val title: String, val path: String)

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
                Icons.Default.Folder,
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
    val projection = arrayOf(
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DATA
    )
    val selection = if (folderName == "All Videos") null else "${MediaStore.Video.Media.BUCKET_DISPLAY_NAME} = ?"
    val selectionArgs = if (folderName == "All Videos") null else arrayOf(folderName)

    try {
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            while (cursor.moveToNext()) {
                val title = cursor.getString(nameCol) ?: "Unknown"
                val path = cursor.getString(dataCol) ?: ""
                videos.add(LocalVideo(title, path))
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
