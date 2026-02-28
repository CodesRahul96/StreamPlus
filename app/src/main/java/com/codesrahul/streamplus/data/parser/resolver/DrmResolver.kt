package com.codesrahul.streamplus.data.parser.resolver

import com.codesrahul.streamplus.domain.model.M3UItem

class DrmResolver : StreamResolver {

    override fun resolve(item: M3UItem): M3UItem {
        val originalScheme = item.drmScheme
        val originalLicense = item.licenseUrl
        val url = item.url.lowercase()
        
        var scheme = originalScheme
        
        // 1. Auto-detect scheme from URL/License if not set
        if (scheme.isNullOrBlank()) {
            scheme = when {
                // ClearKey Header format (KID:KEY) - hex or base64 without http
                item.licenseUrl?.contains(":") == true && !item.licenseUrl.startsWith("http") -> "clearkey"
                
                // Dash almost always uses Widevine
                url.contains(".mpd") -> "widevine"
                
                // HLS or others with a License URL
                item.licenseUrl?.isNotEmpty() == true -> {
                    val lUrl = item.licenseUrl.lowercase()
                    when {
                        lUrl.contains("widevine") || lUrl.contains("lic/wv") || lUrl.contains("proxy/lic") -> "widevine"
                        lUrl.contains("playready") -> "playready"
                        lUrl.contains("clearkey") -> "clearkey"
                        // If it's a standard URL, default to Widevine as it's the most common
                        lUrl.startsWith("http") -> "widevine"
                        else -> null
                    }
                }
                else -> null
            }
        }

        // 2. Map standard strings to internal names
        val normalizedScheme = when (scheme?.lowercase()) {
            "widevine", "com.widevine.alpha", "wv" -> "widevine"
            "clearkey", "org.w3.clearkey" -> "clearkey"
            "playready", "com.microsoft.playready", "pr" -> "playready"
            else -> scheme
        }

        if (normalizedScheme == originalScheme) return item

        return item.copy(
            drmScheme = normalizedScheme
        )
    }
}
