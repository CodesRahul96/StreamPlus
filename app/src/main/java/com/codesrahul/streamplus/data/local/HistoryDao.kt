package com.codesrahul.streamplus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT lastPosition FROM history_table WHERE name = :name AND `group` = :group AND playlistId = :playlistId")
    suspend fun getPosition(name: String, group: String, playlistId: Int): Long?

    @Delete
    suspend fun deleteHistory(history: HistoryEntity)

    @Query("DELETE FROM history_table")
    suspend fun clearAll()

    @Query("DELETE FROM history_table WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: Int)
}
