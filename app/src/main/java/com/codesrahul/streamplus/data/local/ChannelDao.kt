package com.codesrahul.streamplus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId")
    suspend fun getChannelsByPlaylist(playlistId: Int): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsByPlaylist(playlistId: Int)

    @Query("SELECT * FROM channels WHERE name = :name AND `group` = :group AND playlistId = :playlistId LIMIT 1")
    suspend fun getChannelByIdentity(name: String, group: String, playlistId: Int): ChannelEntity?

    @Query("""
        SELECT * FROM channels 
        WHERE name LIKE '%' || :query || '%' 
        OR `group` LIKE '%' || :query || '%' 
        LIMIT 100
    """)
    suspend fun searchChannels(query: String): List<ChannelEntity>

    @Query("DELETE FROM channels")
    suspend fun clearAll()
}
