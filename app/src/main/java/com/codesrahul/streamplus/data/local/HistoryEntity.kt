package com.codesrahul.streamplus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "history_table",
    primaryKeys = ["name", "group", "playlistId"]
)
data class HistoryEntity(
    val name: String,
    val group: String,
    val playlistId: Int,
    val url: String, // Latest known URL
    val logoUrl: String?,
    val userAgent: String?,
    val cookie: String?,
    val referer: String?,
    val origin: String?,
    val licenseUrl: String?,
    val drmScheme: String?,
    val lastPosition: Long,
    val duration: Long,
    val timestamp: Long
)
