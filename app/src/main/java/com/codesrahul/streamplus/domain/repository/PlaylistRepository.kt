package com.codesrahul.streamplus.domain.repository

import com.codesrahul.streamplus.data.local.FavoriteEntity
import com.codesrahul.streamplus.data.local.PlaylistEntity
import com.codesrahul.streamplus.domain.model.M3UItem
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface PlaylistRepository {
    suspend fun parsePlaylist(inputStream: InputStream): List<M3UItem>
    suspend fun validatePlaylist(items: List<M3UItem>): List<M3UItem>
    suspend fun savePlaylist(items: List<M3UItem>, outputStream: java.io.OutputStream): Boolean

    // Local Playlist Management
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>
    fun getPlaylist(id: Int): Flow<PlaylistEntity?>
    suspend fun insertPlaylist(playlist: PlaylistEntity)
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    // Network
    suspend fun loadPlaylist(url: String, userAgent: String? = null, forceRefresh: Boolean = false): List<M3UItem>

    // Favorites
    fun getAllFavorites(): Flow<List<FavoriteEntity>>
    suspend fun addFavorite(item: FavoriteEntity)
    suspend fun removeFavorite(name: String, group: String, playlistId: Int)
    suspend fun isFavorite(name: String, group: String, playlistId: Int): Boolean

    // Global Search
    suspend fun searchAllPlaylists(query: String): List<M3UItem>
}
