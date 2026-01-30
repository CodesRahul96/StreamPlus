package com.codesrahul.streamplus.ui.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: String = savedStateHandle["playlistId"] ?: ""
    private val _playlist = MutableStateFlow<com.codesrahul.streamplus.data.local.PlaylistEntity?>(null)
    val playlist = _playlist.asStateFlow()

    private val _channels = MutableStateFlow<List<M3UItem>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            val id = playlistId.toIntOrNull()
            if (id != null) {
                repository.getPlaylist(id).collect { entity ->
                    if (entity != null) {
                        _playlist.value = entity
                        loadChannels(entity.url, entity.userAgent)
                    }
                }
            } else if (playlistId.startsWith("http")) {
                // It's a direct URL (possibly base64 encoded or raw if handled by nav)
                val decodedUrl = try {
                    java.net.URLDecoder.decode(playlistId, "UTF-8")
                } catch (e: Exception) {
                    playlistId
                }
                
                val name = savedStateHandle.get<String>("name") ?: "Sub Playlist"
                val ua = savedStateHandle.get<String>("userAgent") ?: "Auto"
                
                val tempEntity = com.codesrahul.streamplus.data.local.PlaylistEntity(
                    id = -1,
                    name = name,
                    url = decodedUrl,
                    userAgent = ua
                )
                _playlist.value = tempEntity
                loadChannels(decodedUrl, ua)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            playlist.value?.let { entity ->
                loadChannels(entity.url, entity.userAgent)
            }
        }
    }

    private fun loadChannels(url: String, userAgent: String?) {
        viewModelScope.launch {
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
}
