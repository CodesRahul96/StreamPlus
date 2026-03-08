package com.codesrahul.streamplus.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "favorites",
    primaryKeys = ["name", "groupTitle", "playlistId"]
)
@androidx.compose.runtime.Immutable
data class FavoriteEntity(
    val name: String,
    val groupTitle: String,
    val playlistId: Int,
    val url: String, // Latest known URL
    val logoUrl: String = "",
    val userAgent: String = "",
    val headers: Map<String, String> = emptyMap(),
    val licenseUrl: String? = null,
    val drmScheme: String? = null,
    val drmHeaders: Map<String, String> = emptyMap(),
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE name = :name AND groupTitle = :group AND playlistId = :playlistId)")
    suspend fun isFavorite(name: String, group: String, playlistId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE name = :name AND groupTitle = :group AND playlistId = :playlistId")
    suspend fun removeFavorite(name: String, group: String, playlistId: Int)

    @Query("DELETE FROM favorites WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: Int)
}
