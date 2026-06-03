package com.codesrahul.streamplus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val userAgent: String = "",
    val type: String = "URL", // URL or FILE
    val icon: String = "default",
    val color: Int = -16777216,
    val epgUrl: String? = null,
    val isPinned: Boolean = false,
    // Xtream Codes API Fields
    val isXtream: Boolean = false,
    val serverUrl: String? = null,
    val username: String? = null,
    val password: String? = null
)
