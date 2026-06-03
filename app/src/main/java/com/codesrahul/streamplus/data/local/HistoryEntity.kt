package com.codesrahul.streamplus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "history_table",
    primaryKeys = ["name", "group", "playlistId"]
)
@androidx.compose.runtime.Immutable
data class HistoryEntity(
    val name: String,
    val group: String,
    val playlistId: Int,
    val url: String, // Latest known URL
    val logoUrl: String?,
    val userAgent: String?,
    val headers: Map<String, String> = emptyMap(),
    val cookie: String?,
    val referer: String?,
    val origin: String?,
    val licenseUrl: String?,
    val drmScheme: String?,
    val drmHeaders: Map<String, String> = emptyMap(),
    val streamType: String = "",
    val lastPosition: Long,
    val duration: Long,
    val timestamp: Long
)
