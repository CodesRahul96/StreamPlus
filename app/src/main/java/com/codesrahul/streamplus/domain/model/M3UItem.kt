package com.codesrahul.streamplus.domain.model

@androidx.compose.runtime.Immutable
data class M3UItem(
    val name: String,
    val url: String,
    val group: String = "",
    val logoUrl: String = "",
    val playlistId: Int = -1,
    val status: StreamStatus = StreamStatus.UNKNOWN,
    val headers: Map<String, String> = emptyMap(),
    val drmHeaders: Map<String, String> = emptyMap(),
    val licenseUrl: String? = null,
    val drmScheme: String? = null,
    val userAgent: String = "",
    val isPlaylist: Boolean = false,
    val epgId: String? = null,
    val isVod: Boolean = false
)

enum class StreamStatus {
    UNKNOWN,
    ACTIVE,
    DEAD,
    TIMEOUT
}
