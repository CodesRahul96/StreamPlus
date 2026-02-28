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
import javax.inject.Inject

@HiltViewModel
class LocalViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _folders = MutableStateFlow<List<VideoFolder>>(emptyList())
    val folders = _folders.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder = _selectedFolder.asStateFlow()

    private val _videos = MutableStateFlow<List<LocalVideo>>(emptyList())
    val videos = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.DATE)
    val sortType = _sortType.asStateFlow()

    fun refreshFolders() {
        viewModelScope.launch {
            _isLoading.value = true
            val list = withContext(Dispatchers.IO) {
                getVideoFolders(context)
            }
            _folders.value = when (_sortType.value) {
                SortType.NAME -> list.sortedBy { it.name }
                SortType.DATE -> list 
                SortType.SIZE -> list.sortedByDescending { it.totalSize }
                SortType.COUNT -> list.sortedByDescending { it.count }
            }
            _isLoading.value = false
        }
    }

    fun selectFolder(folderName: String?) {
        _selectedFolder.value = folderName
        if (folderName != null) {
            loadVideos(folderName)
        }
    }

    private fun loadVideos(folderName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val list = withContext(Dispatchers.IO) {
                getVideosInFolder(context, folderName)
            }
            _videos.value = list
            _isLoading.value = false
        }
    }

    fun setSortType(type: SortType) {
        _sortType.value = type
        refreshFolders()
    }
}
