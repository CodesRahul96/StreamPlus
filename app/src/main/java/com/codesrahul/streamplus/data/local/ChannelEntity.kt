package com.codesrahul.streamplus.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.codesrahul.streamplus.domain.model.M3UItem

@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"]), Index(value = ["url"])]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val name: String,
    val url: String,
    val group: String = "",
    val logoUrl: String = "",
    val userAgent: String = "",
    val headers: Map<String, String> = emptyMap(),
    val licenseUrl: String? = null,
    val drmScheme: String? = null,
    val drmHeaders: Map<String, String> = emptyMap(),
    val epgId: String? = null,
    val isVod: Boolean = false,
    val streamType: String = "", // "live", "vod", "series", or "" for non-Xtream
    val status: String = "UNKNOWN"
)

fun ChannelEntity.toDomain() = M3UItem(
    id = id,
    name = name,
    url = url,
    group = group,
    logoUrl = logoUrl,
    playlistId = playlistId,
    status = try { com.codesrahul.streamplus.domain.model.StreamStatus.valueOf(status) } catch (e: Exception) { com.codesrahul.streamplus.domain.model.StreamStatus.UNKNOWN },
    userAgent = userAgent,
    headers = headers,
    licenseUrl = licenseUrl,
    drmScheme = drmScheme,
    drmHeaders = drmHeaders,
    epgId = epgId,
    isVod = isVod,
    streamType = streamType
)

fun M3UItem.toEntity(playlistId: Int) = ChannelEntity(
    playlistId = playlistId,
    name = name,
    url = url,
    group = group,
    logoUrl = logoUrl,
    status = status.name,
    userAgent = userAgent,
    headers = headers,
    licenseUrl = licenseUrl,
    drmScheme = drmScheme,
    drmHeaders = drmHeaders,
    epgId = epgId,
    isVod = isVod,
    streamType = streamType
)
