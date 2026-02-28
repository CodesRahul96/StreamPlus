package com.codesrahul.streamplus.ui.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codesrahul.streamplus.data.local.FavoriteEntity
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val epgManager: com.codesrahul.streamplus.data.epg.EPGManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: String = savedStateHandle.get<String>("playlistId") ?: ""
    private val _playlist = MutableStateFlow<com.codesrahul.streamplus.data.local.PlaylistEntity?>(null)
    val playlist = _playlist.asStateFlow()

    private val _channels = MutableStateFlow<List<M3UItem>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedGroup = MutableStateFlow("All")
    val selectedGroup = _selectedGroup.asStateFlow()

    // Favorite Identity set for fast O(1) lookup in the UI (Format: "name|group|playlistId")
    val favoriteIdentities = repository.getAllFavorites()
        .map { list -> list.map { "${it.name}|${it.groupTitle}|${it.playlistId}" }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val filteredChannels = combine(_channels, _searchQuery, _selectedGroup) { channels, query, group ->
        if (query.isBlank() && group == "All") {
            channels
        } else {
            kotlinx.coroutines.withContext(Dispatchers.Default) {
                channels.filter { ch ->
                    (group == "All" || ch.group == group) &&
                    (ch.name.contains(query, ignoreCase = true) || ch.url.contains(query, ignoreCase = true))
                }
            }
        }
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups = _channels.map { channels ->
        kotlinx.coroutines.withContext(Dispatchers.Default) {
            buildList {
                add("All")
                channels.map { it.group }.distinct().filter { it.isNotBlank() }.sorted().forEach { add(it) }
            }
        }
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    init {
        viewModelScope.launch {
            val id = playlistId.toIntOrNull()
            if (id != null) {
                repository.getPlaylist(id).collect { entity ->
                    if (entity != null) {
                        val prevUrl = _playlist.value?.url
                        _playlist.value = entity
                        if (prevUrl != entity.url) {
                            loadChannels(entity.url, entity.userAgent)
                            entity.epgUrl?.let { epgManager.loadEPG(it) }
                        }
                    }
                }
            } else if (playlistId.startsWith("http")) {
                val decodedUrl = try { java.net.URLDecoder.decode(playlistId, "UTF-8") } catch (e: Exception) { playlistId }
                val name = savedStateHandle.get<String>("name") ?: "Sub Playlist"
                val ua = savedStateHandle.get<String>("userAgent") ?: "Auto"
                val tempEntity = com.codesrahul.streamplus.data.local.PlaylistEntity(id = -1, name = name, url = decodedUrl, userAgent = ua)
                _playlist.value = tempEntity
                loadChannels(decodedUrl, ua)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { playlist.value?.let { loadChannels(it.url, it.userAgent) } }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun setSelectedGroup(group: String) { _selectedGroup.value = group }

    fun toggleFavorite(item: M3UItem) {
        val currentPlaylistId = _playlist.value?.id ?: -1
        viewModelScope.launch {
            if (repository.isFavorite(item.name, item.group, currentPlaylistId)) {
                repository.removeFavorite(item.name, item.group, currentPlaylistId)
            } else {
                repository.addFavorite(
                    FavoriteEntity(
                        name = item.name,
                        groupTitle = item.group,
                        playlistId = currentPlaylistId,
                        url = item.url,
                        logoUrl = item.logoUrl
                    )
                )
            }
        }
    }

    private var loadChannelsJob: kotlinx.coroutines.Job? = null

    private fun loadChannels(url: String, userAgent: String?) {
        loadChannelsJob?.cancel()
        loadChannelsJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                _channels.value = repository.loadPlaylist(url, userAgent)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCurrentProgram(epgId: String?) = epgManager.getCurrentProgram(epgId)
}
