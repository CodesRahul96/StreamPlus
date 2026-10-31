package com.codesrahul.streamplus.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codesrahul.streamplus.data.remote.model.XtreamSeriesInfo
import com.codesrahul.streamplus.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val _playlist = MutableStateFlow<com.codesrahul.streamplus.data.local.PlaylistEntity?>(null)
    val playlist = _playlist.asStateFlow()

    private val _seriesInfo = MutableStateFlow<XtreamSeriesInfo?>(null)
    val seriesInfo = _seriesInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadSeriesInfo(playlistId: Int, seriesId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Fetch playlist first
                val p = repository.getPlaylist(playlistId).firstOrNull()
                _playlist.value = p

                val info = repository.getSeriesInfo(playlistId, seriesId)
                if (info != null) {
                    _seriesInfo.value = info
                } else {
                    _error.value = "Failed to load series details"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getStreamBaseUrl(p: com.codesrahul.streamplus.data.local.PlaylistEntity, serverInfoUrl: String?): String {
        val rawUrl = p.serverUrl?.trim() ?: ""
        
        // 1. If server suggested a URL in auth, that's often the "proper" one
        if (!serverInfoUrl.isNullOrBlank() && serverInfoUrl.startsWith("http")) {
            return serverInfoUrl.removeSuffix("/")
        }

        // 2. Fallback to sanitized user-provided URL
        val noQuery = rawUrl.split("?")[0]
        var stripped = noQuery.removeSuffix("/")
            .replace(Regex("/(player_api\\.php|get\\.php|panel_api\\.php|xmltv\\.php|get|playlist|live|movie|series)/?.*$", RegexOption.IGNORE_CASE), "")
            .removeSuffix("/")
        
        stripped = stripped.replace(Regex("/[^/]+/[^/]+/[0-9]+/?$", RegexOption.IGNORE_CASE), "")
            .removeSuffix("/")
            
        return try {
            val parsed = java.net.URL(stripped)
            if (parsed.path.isNullOrEmpty() || parsed.path == "/") {
                val portStr = if (parsed.port != -1) ":${parsed.port}" else ""
                "${parsed.protocol}://${parsed.host}$portStr"
            } else {
                stripped
            }
        } catch (e: Exception) {
            stripped
        }
    }
}
