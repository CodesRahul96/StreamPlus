package com.codesrahul.streamplus.ui.playlist

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codesrahul.streamplus.data.local.FavoriteEntity
import com.codesrahul.streamplus.data.local.PlaylistEntity
import com.codesrahul.streamplus.data.repository.HistoryRepository
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val playlists = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites = repository.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyWatched = historyRepository.getAllHistory()
        .map { entries -> entries.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<M3UItem>>(emptyList())
    val searchResults: StateFlow<List<M3UItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Auto-search with 500ms debounce whenever the query changes
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                    } else {
                        _isSearching.value = true
                        _searchResults.value = repository.searchAllPlaylists(query)
                        _isSearching.value = false
                    }
                }
        }
        viewModelScope.launch {
            playlists.collect { _isLoading.value = false }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun addPlaylist(name: String, url: String, userAgent: String, icon: String = "default", color: Int = -16777216, epgUrl: String? = null) {
        viewModelScope.launch {
            repository.insertPlaylist(
                PlaylistEntity(name = name, url = url, userAgent = userAgent, icon = icon, color = color, epgUrl = epgUrl)
            )
        }
    }

    fun updatePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch { repository.insertPlaylist(playlist) }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch { repository.deletePlaylist(playlist) }
    }

    fun toggleFavorite(item: M3UItem, playlistId: Int) {
        viewModelScope.launch {
            if (repository.isFavorite(item.name, item.group, playlistId)) {
                repository.removeFavorite(item.name, item.group, playlistId)
            } else {
                repository.addFavorite(
                    FavoriteEntity(
                        name = item.name,
                        groupTitle = item.group,
                        playlistId = playlistId,
                        url = item.url,
                        logoUrl = item.logoUrl
                    )
                )
            }
        }
    }

    fun importLocalPlaylist(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val fileName = getFileName(context, uri) ?: "Local Playlist"
                val playlistDir = File(context.filesDir, "playlists")
                if (!playlistDir.exists()) playlistDir.mkdirs()
                val safeName = fileName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                val localFile = File(playlistDir, "${System.currentTimeMillis()}_$safeName")
                contentResolver.openInputStream(uri)?.use { input ->
                    localFile.outputStream().use { output -> input.copyTo(output) }
                }
                repository.insertPlaylist(
                    PlaylistEntity(name = fileName, url = Uri.fromFile(localFile).toString(), userAgent = "Default")
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
                    if (index != -1) result = cursor.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }
}
