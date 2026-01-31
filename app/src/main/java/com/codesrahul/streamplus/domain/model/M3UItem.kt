package com.codesrahul.streamplus.domain.model

data class M3UItem(
    val name: String,
    val url: String,
    val group: String = "",
    val logoUrl: String = "",
    var status: StreamStatus = StreamStatus.UNKNOWN,
    val headers: Map<String, String> = emptyMap(),
    val drmHeaders: Map<String, String> = emptyMap(),
    val licenseUrl: String? = null,
    val drmScheme: String? = null,
    val isPlaylist: Boolean = false
)

enum class StreamStatus {
    UNKNOWN,
    ACTIVE,
    DEAD,
    TIMEOUT
}
