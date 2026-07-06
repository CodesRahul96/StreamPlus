package com.codesrahul.streamplus.ui.local

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import com.codesrahul.streamplus.data.settings.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class LocalViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _folders = MutableStateFlow<List<LocalFolder>>(emptyList())
    val folders = _folders.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder = _selectedFolder.asStateFlow()

    private val _videos = MutableStateFlow<List<LocalMedia>>(emptyList())
    val videos = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _mediaType = MutableStateFlow(MediaType.VIDEO)
    val mediaType = _mediaType.asStateFlow()

    val settings = settingsRepository.settings

    init {
        viewModelScope.launch {
            combine(settings, _selectedFolder, _mediaType) { s, folder, type -> Triple(s, folder, type) }
                .collect { (s, folder, type) ->
                    refreshFolders(s, type)
                    if (folder != null) {
                        loadVideos(folder, s, type)
                    }
                }
        }
    }

    fun refreshFolders() {
        viewModelScope.launch {
            val s = settings.first()
            refreshFolders(s, _mediaType.value)
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            val s = settingsRepository.settings.first()
            val type = _mediaType.value
            refreshFolders(s, type)
            val currentFolder = _selectedFolder.value
            if (currentFolder != null) {
                loadVideos(currentFolder, s, type)
            }
        }
    }

    fun setMediaType(type: MediaType) {
        _selectedFolder.value = null
        _mediaType.value = type
    }

    private suspend fun refreshFolders(s: com.codesrahul.streamplus.data.settings.AppSettings, type: MediaType) {
        _isLoading.value = true
        val list = withContext(Dispatchers.IO) {
            if (type == MediaType.VIDEO) getLocalFolders(context) else getAudioFolders(context)
        }
        
        val sortedList = when (s.folderSortField) {
            "Name" -> list.sortedBy { it.name }
            "Count" -> list.sortedBy { it.count }
            "Size" -> list.sortedBy { it.totalSize }
            "Date" -> list // MediaStore already returns default order
            else -> list
        }
        
        _folders.value = if (s.folderSortAsc) sortedList else sortedList.reversed()
        _isLoading.value = false
    }

    fun selectFolder(folderName: String?) {
        _selectedFolder.value = folderName
    }

    private fun loadVideos(folderName: String, s: com.codesrahul.streamplus.data.settings.AppSettings, type: MediaType) {
        viewModelScope.launch {
            _isLoading.value = true
            val list = withContext(Dispatchers.IO) {
                if (type == MediaType.VIDEO) getVideosInFolder(context, folderName)
                else getAudiosInFolder(context, folderName)
            }
            
            val sortedList = when (s.videoSortField) {
                "Name" -> list.sortedBy { it.title }
                "Duration" -> list.sortedBy { it.duration }
                "Size" -> list.sortedBy { it.size }
                "Date" -> list.sortedBy { it.dateAdded }
                else -> list
            }
            
            _videos.value = if (s.videoSortAsc) sortedList else sortedList.reversed()
            _isLoading.value = false
        }
    }

    // Removed setSortType as it's now handled by repository settings
}
