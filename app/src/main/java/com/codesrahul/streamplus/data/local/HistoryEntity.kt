package com.codesrahul.streamplus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_table")
data class HistoryEntity(
    @PrimaryKey val url: String, // distinct by URL
    val name: String,
    val logoUrl: String?,
    val group: String?,
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
