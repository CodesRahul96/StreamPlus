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

    private val _folders = MutableStateFlow<List<VideoFolder>>(emptyList())
    val folders = _folders.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder = _selectedFolder.asStateFlow()

    private val _videos = MutableStateFlow<List<LocalVideo>>(emptyList())
    val videos = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val settings = settingsRepository.settings

    init {
        viewModelScope.launch {
            combine(settings, _selectedFolder) { s, folder -> s to folder }
                .collect { (s, folder) ->
                    refreshFolders(s)
                    if (folder != null) {
                        loadVideos(folder, s)
                    }
                }
        }
    }

    fun refreshFolders() {
        viewModelScope.launch {
            val s = settings.first()
            refreshFolders(s)
        }
    }

    private suspend fun refreshFolders(s: com.codesrahul.streamplus.data.settings.AppSettings) {
        _isLoading.value = true
        val list = withContext(Dispatchers.IO) {
            getVideoFolders(context)
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

    private fun loadVideos(folderName: String, s: com.codesrahul.streamplus.data.settings.AppSettings) {
        viewModelScope.launch {
            _isLoading.value = true
            val list = withContext(Dispatchers.IO) {
                getVideosInFolder(context, folderName)
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
