package com.codesrahul.streamplus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY isPinned DESC, id ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY isPinned DESC, id ASC")
    suspend fun getAllPlaylistsList(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylist(id: Int): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistSync(id: Int): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET epgUrl = :epgUrl WHERE id = :id")
    suspend fun updateEpgUrl(id: Int, epgUrl: String)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)
}
