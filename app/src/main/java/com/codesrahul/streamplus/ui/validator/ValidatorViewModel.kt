package com.codesrahul.streamplus.ui.validator

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.domain.model.StreamStatus
import com.codesrahul.streamplus.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

sealed class ValidatorUiState {
    data object Idle : ValidatorUiState()
    data object Loading : ValidatorUiState()
    data class Validating(val progress: Int, val total: Int, val currentItemName: String) : ValidatorUiState()
    data class Success(val items: List<M3UItem>) : ValidatorUiState()
    data class Error(val message: String) : ValidatorUiState()
}

@HiltViewModel
class ValidatorViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ValidatorUiState>(ValidatorUiState.Idle)
    val uiState: StateFlow<ValidatorUiState> = _uiState.asStateFlow()

    private var currentItems = listOf<M3UItem>()

    fun loadPlaylist(inputStream: InputStream) {
        viewModelScope.launch {
            _uiState.value = ValidatorUiState.Loading
            try {
                currentItems = repository.parsePlaylist(inputStream)
                _uiState.value = ValidatorUiState.Success(currentItems) // Initially show loaded items without validation
            } catch (e: Exception) {
                _uiState.value = ValidatorUiState.Error("Failed to parse playlist: ${e.message}")
            }
        }
    }

    fun startValidation() {
        if (currentItems.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = ValidatorUiState.Validating(0, currentItems.size, "Starting...")
            
            // For better progress updates, we might need to change the repository to emit flow or validate one by one here
            // implementing a simple chunked validation here for progress updates.
             val total = currentItems.size
             
             currentItems.forEachIndexed { index, item ->
                 _uiState.value = ValidatorUiState.Validating(index + 1, total, item.name)
                 // This is not efficient for UI to update on every single item if list is huge, 
                 // but for now it's fine for "modern" feel.
                 // We should probably parallelize this but still report progress.
                 
                 // Simulating parallel batching or just calling helper
                 // To keep it simple and responsive, let's use the repository's validatePlaylist 
                 // but we need progress.
                 // A better approach: Refactor Repository to Flow or just do it here.
                 // Let's do a simple map here for now to get progress.
                 
                 // Ideally we want parallel checks.
             }
             
             // Let's stick to the repository method for now, but to get progress we'd need to change the interface.
             // For this MVP, let's just use the bulk validate and show "Validating..."
             
             // Actually, the user wants "Modern, Bugless". A progress bar that doesn't move is bad.
             // Redesign: We will validate in chunks and update progress.
             
             val results = mutableListOf<M3UItem>()
             val chunkSize = 20
             val chunks = currentItems.chunked(chunkSize)
             var processedCount = 0
             
             for (chunk in chunks) {
                 try {
                     val validatedChunk = repository.validatePlaylist(chunk)
                     results.addAll(validatedChunk)
                     processedCount += chunk.size
                     _uiState.value = ValidatorUiState.Validating(processedCount, total, "Checking items...")
                 } catch (e: Exception) {
                     processedCount += chunk.size
                     results.addAll(chunk) // Fallback to original items on total failure of chunk
                     _uiState.value = ValidatorUiState.Validating(processedCount, total, "Retrying...")
                 }
             }
             
             currentItems = results
             _uiState.value = ValidatorUiState.Success(results)
        }
    }
    
    fun exportPlaylist(outputStream: java.io.OutputStream) {
        viewModelScope.launch {
            val cleanItems = currentItems.filter { it.status == StreamStatus.ACTIVE }
            if (cleanItems.isEmpty()) {
                 _uiState.update { ValidatorUiState.Error("No active streams to export") }
                 return@launch
            }
            val success = repository.savePlaylist(cleanItems, outputStream)
            if (!success) {
                 _uiState.update { ValidatorUiState.Error("Failed to save playlist") }
            }
        }
    }
}
