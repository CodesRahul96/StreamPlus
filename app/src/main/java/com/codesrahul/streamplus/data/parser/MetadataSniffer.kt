package com.codesrahul.streamplus.data.parser

import com.codesrahul.streamplus.domain.model.M3UItem

object MetadataSniffer {

    fun sniffMetadata(url: String, currentName: String? = null): Metadata {
        var name = currentName
        var logo: String? = null
        var group: String? = null
        var drmScheme: String? = null
        var drmLicense: String? = null

        val lowerUrl = url.lowercase()

        // 1. Extract Name from URL patterns
        if (name.isNullOrBlank() || name.startsWith("Channel")) {
            name = when {
                url.contains("/mp1/") -> url.substringAfter("/mp1/").substringBefore("/").clean()
                url.contains("/mp2/") -> url.substringAfter("/mp2/").substringBefore("/").clean()
                url.contains("/live/") -> {
                    val segment = url.substringAfter("/live/").substringAfter("/")
                    (if (segment.contains("/")) segment.substringBefore("/") else segment.substringBefore(".")).clean()
                }
                url.contains("/bpk-tv/") -> url.substringAfter("/bpk-tv/").substringBefore("/").clean()
                url.contains("/ZEE5_Live_Channels/") -> url.substringAfter("/ZEE5_Live_Channels/").substringBefore("/").clean()
                url.contains("/cmaf/live/") -> {
                    val segments = url.substringAfter("/cmaf/live/").split("/")
                    if (segments.size >= 2) segments[1].clean() else null
                }
                url.contains("sony", ignoreCase = true) && url.contains(".mpd") -> "Sony Stream"
                url.contains("zee5", ignoreCase = true) && url.contains(".mpd") -> "Zee5 Stream"
                url.contains("star", ignoreCase = true) && url.contains(".mpd") -> "Star Stream"
                url.contains("/channels/") -> url.substringAfter("/channels/").substringBefore("/").substringBefore(".").clean()
                url.contains("/hls/") -> url.substringAfter("/hls/").substringBefore("/").substringBefore(".").clean()
                else -> null
            }
        }

        // 2. Sniff DRM
        if (lowerUrl.contains("clearkey") || lowerUrl.contains("ck=")) {
            drmScheme = "clearkey"
        } else if (lowerUrl.contains("widevine") || lowerUrl.contains("wv=") || lowerUrl.contains("sony") || lowerUrl.contains("zee5") || lowerUrl.contains("star") || lowerUrl.contains("hotstar") || lowerUrl.contains("voot") || lowerUrl.contains("jiocinema")) {
            // Premium Indian OTT patterns
            if (lowerUrl.contains(".mpd") || lowerUrl.contains("license") || lowerUrl.contains("manifest") || lowerUrl.contains("/drm/") || lowerUrl.contains("key")) {
                 drmScheme = "widevine"
            }
        }
        
        // Final fallback for MPD
        if (drmScheme == null && lowerUrl.contains(".mpd")) {
            drmScheme = "widevine"
        }

        // 3. Sniff Group from URL
        if (group == null) {
            group = when {
                lowerUrl.contains("news") -> "News"
                lowerUrl.contains("movie") -> "Movies"
                lowerUrl.contains("sport") -> "Sports"
                lowerUrl.contains("music") -> "Music"
                lowerUrl.contains("kids") -> "Kids"
                lowerUrl.contains("infotainment") -> "Infotainment"
                else -> null
            }
        }

        return Metadata(name, logo, group, drmScheme, drmLicense)
    }

    private fun String.clean(): String {
        return this.replace("_", " ")
            .replace("-", " ")
            .replace("%20", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.titlecase() } }
            .trim()
    }

    data class Metadata(
        val name: String?,
        val logo: String?,
        val group: String?,
        val drmScheme: String?,
        val drmLicense: String?
    )
}
