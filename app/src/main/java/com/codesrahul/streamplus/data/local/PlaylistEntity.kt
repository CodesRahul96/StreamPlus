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
    val color: Int = -16777216 // Color.Black as int, or maybe a better default like Color(0xFF6750A4).toArgb()
)
