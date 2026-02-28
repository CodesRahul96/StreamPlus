package com.codesrahul.streamplus.ui.playlist

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codesrahul.streamplus.data.local.PlaylistEntity
import com.codesrahul.streamplus.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    val playlists = repository.getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addPlaylist(name: String, url: String, userAgent: String, icon: String = "default", color: Int = -16777216) {
        viewModelScope.launch {
            repository.insertPlaylist(
                PlaylistEntity(
                    name = name,
                    url = url,
                    userAgent = userAgent,
                    icon = icon,
                    color = color
                )
            )
        }
    }

    fun updatePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.insertPlaylist(playlist)
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    fun importLocalPlaylist(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val fileName = getFileName(context, uri) ?: "Local Playlist"
                
                // Create internal directory for playlists
                val playlistDir = File(context.filesDir, "playlists")
                if (!playlistDir.exists()) playlistDir.mkdirs()
                
                // Generate a unique safe filename
                val safeName = fileName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                val localFile = File(playlistDir, "${System.currentTimeMillis()}_$safeName")
                
                // Copy the file
                contentResolver.openInputStream(uri)?.use { input ->
                    localFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Save the local file's URI
                repository.insertPlaylist(
                    PlaylistEntity(
                        name = fileName,
                        url = Uri.fromFile(localFile).toString(),
                        userAgent = "Default"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
