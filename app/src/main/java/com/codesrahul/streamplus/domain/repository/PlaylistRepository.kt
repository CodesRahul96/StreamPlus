package com.codesrahul.streamplus.domain.repository

import com.codesrahul.streamplus.domain.model.M3UItem
import java.io.InputStream

interface PlaylistRepository {
    suspend fun parsePlaylist(inputStream: InputStream): List<M3UItem>
    suspend fun validatePlaylist(items: List<M3UItem>): List<M3UItem>
    suspend fun savePlaylist(items: List<M3UItem>, filePath: String): Boolean
    
    // Local Playlist Management
    fun getAllPlaylists(): kotlinx.coroutines.flow.Flow<List<com.codesrahul.streamplus.data.local.PlaylistEntity>>
    fun getPlaylist(id: Int): kotlinx.coroutines.flow.Flow<com.codesrahul.streamplus.data.local.PlaylistEntity?>
    suspend fun insertPlaylist(playlist: com.codesrahul.streamplus.data.local.PlaylistEntity)
    suspend fun deletePlaylist(playlist: com.codesrahul.streamplus.data.local.PlaylistEntity)
    
    // Network
    suspend fun loadPlaylist(url: String, userAgent: String? = null): List<M3UItem>
}
