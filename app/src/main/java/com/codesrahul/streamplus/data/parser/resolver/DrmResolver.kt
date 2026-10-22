package com.codesrahul.streamplus.data.parser.resolver

import com.codesrahul.streamplus.domain.model.M3UItem

class DrmResolver : StreamResolver {

    override fun resolve(item: M3UItem): M3UItem {
        val originalScheme = item.drmScheme
        var scheme = originalScheme
        
        val hasClearKeyHeader = item.headers.keys.any { it.equals("clearkey", ignoreCase = true) } ||
                item.drmHeaders.keys.any { it.equals("clearkey", ignoreCase = true) || it.equals("X-ClearKey-Data", ignoreCase = true) }
        val licenseUrlLower = item.licenseUrl?.lowercase() ?: ""
        val isClearKeyUrl = licenseUrlLower.contains("clearkey")

        // 1. ClearKey takes priority if key or clearkey URL is explicitly present
        if (hasClearKeyHeader || isClearKeyUrl) {
            scheme = "clearkey"
        } else if (scheme.isNullOrBlank()) {
            scheme = when {
                item.licenseUrl?.contains(":") == true && !item.licenseUrl.startsWith("http") -> "clearkey"

                item.licenseUrl?.isNotEmpty() == true -> {
                    when {
                        licenseUrlLower.contains("widevine") || licenseUrlLower.contains("lic/wv") || licenseUrlLower.contains("proxy/lic") -> "widevine"
                        licenseUrlLower.contains("playready") -> "playready"
                        licenseUrlLower.contains("clearkey") -> "clearkey"
                        licenseUrlLower.startsWith("http") -> "widevine"
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
