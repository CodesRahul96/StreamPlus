package com.codesrahul.streamplus.ui.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codesrahul.streamplus.data.local.FavoriteEntity
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
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

    companion object {
        // Track which playlists have been refreshed in this app session to avoid redundant network calls
        private val sessionRefreshedPlaylists = mutableSetOf<Int>()
    }

    val playlistId: String = savedStateHandle.get<String>("playlistId") ?: ""
    private val _playlist = MutableStateFlow<com.codesrahul.streamplus.data.local.PlaylistEntity?>(null)
    val playlist = _playlist.asStateFlow()

    private val _channels = MutableStateFlow<List<M3UItem>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isBackgroundLoading = MutableStateFlow(false)
    val isBackgroundLoading = _isBackgroundLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedGroup = MutableStateFlow("All")
    val selectedGroup = _selectedGroup.asStateFlow()

    // Xtream-specific: which top-level category tab is selected (Live/VOD/Series)
    private val _selectedStreamType = MutableStateFlow("live")
    val selectedStreamType = _selectedStreamType.asStateFlow()

    // Whether the current playlist is Xtream (drives tab visibility in the UI)
    private val _isXtreamPlaylist = MutableStateFlow(false)
    val isXtreamPlaylist = _isXtreamPlaylist.asStateFlow()

    // Favorite Identity set for fast O(1) lookup in the UI (Format: "name|group|playlistId")
    val favoriteIdentities = repository.getAllFavorites()
        .map { list -> list.map { "${it.name}|${it.groupTitle}|${it.playlistId}" }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private val _debouncedSearchQuery = _searchQuery
        .debounce(300)
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val filteredChannels = combine(_channels, _debouncedSearchQuery, _selectedGroup, _selectedStreamType, _isXtreamPlaylist) { channels, query, group, streamType, isXtream ->
        kotlinx.coroutines.withContext(Dispatchers.Default) {
            val afterTypeFiler = if (isXtream) channels.filter { it.streamType == streamType } else channels
            if (query.isBlank() && group == "All") {
                afterTypeFiler
            } else {
                afterTypeFiler.filter { ch ->
                    (group == "All" || ch.group.take(100).trim() == group) &&
                    (ch.name.contains(query, ignoreCase = true) || ch.url.contains(query, ignoreCase = true))
                }
            }
        }
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val groups = combine(_channels, _selectedStreamType, _isXtreamPlaylist) { channels, streamType, isXtream ->
        kotlinx.coroutines.withContext(Dispatchers.Default) {
            val relevantChannels = if (isXtream) channels.filter { it.streamType == streamType } else channels
            buildList {
                add("All")
                relevantChannels.map { it.group.take(100).trim() }.distinct().filter { it.isNotBlank() }.sorted().forEach { add(it) }
            }
        }
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf("All"))

    init {
        viewModelScope.launch {
            val id = playlistId.toIntOrNull()
            if (id != null) {
                repository.getPlaylist(id).collect { entity ->
                    if (entity != null) {
                        _isXtreamPlaylist.value = entity.isXtream == true
                        val prevUrl = _playlist.value?.url
                        _playlist.value = entity
                        if (prevUrl != entity.url) {
                            // If it's an Xtream playlist, we force a refresh ONCE per app session
                            // to ensure any connectivity improvements are applied to cached items.
                            val shouldForce = entity.isXtream == true && !sessionRefreshedPlaylists.contains(entity.id)
                            if (shouldForce) sessionRefreshedPlaylists.add(entity.id)
                            
                            loadChannels(entity.url, entity.userAgent, forceRefresh = shouldForce, playlistId = entity.id)
                            entity.epgUrl?.let { epgUrl ->
                                viewModelScope.launch {
                                    epgManager.loadEPG(epgUrl)
                                }
                            }
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
        viewModelScope.launch { playlist.value?.let { loadChannels(it.url, it.userAgent, forceRefresh = true, playlistId = it.id) } }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedGroup(group: String) { _selectedGroup.value = group; }
    fun setSelectedStreamType(type: String) { _selectedStreamType.value = type; _selectedGroup.value = "All" }

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
                        logoUrl = item.logoUrl,
                        userAgent = item.userAgent,
                        headers = item.headers,
                        licenseUrl = item.licenseUrl,
                        drmScheme = item.drmScheme,
                        drmHeaders = item.drmHeaders,
                        streamType = item.streamType
                    )
                )
            }
        }
    }

    private var loadChannelsJob: kotlinx.coroutines.Job? = null

    private fun loadChannels(url: String, userAgent: String?, forceRefresh: Boolean = false, playlistId: Int = -1) {
        loadChannelsJob?.cancel()
        loadChannelsJob = viewModelScope.launch {
            _isLoading.value = true
            _isBackgroundLoading.value = forceRefresh
            try {
                // If forceRefresh is requested, first quickly load from cache (forceRefresh = false)
                // so the user doesn't see a blank shimmer screen while waiting for the network
                if (forceRefresh) {
                    val cached = repository.loadPlaylist(url, userAgent, forceRefresh = false, playlistId)
                    if (cached.isNotEmpty()) {
                        _channels.value = cached
                        _isLoading.value = false // We have content, hide full shimmer!
                    }
                }
                
                val freshChannels = repository.loadPlaylist(url, userAgent, forceRefresh, playlistId)
                _channels.value = freshChannels
                if (playlistId != -1) {
                    repository.startBackgroundDiagnostics(playlistId, freshChannels)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.emit(e.message ?: "Failed to refresh playlist: Network error")
            } finally {
                _isLoading.value = false
                _isBackgroundLoading.value = false
            }
        }
    }

    fun getCurrentProgram(epgId: String?) = epgManager.getCurrentProgram(epgId)
}
