package com.codesrahul.streamplus.util

object UrlParser {
    private val PIPE_REGEX = "(?i)(\\?\\|)|(\\?%7C)|(\\|)|(%7C)".toRegex()

    private val CLEARKEY_HEX_REGEX = "([0-9a-fA-F]{32}):([0-9a-fA-F]{32})".toRegex()

    fun parsePipedUrl(raw: String): Pair<String, Map<String, String>> {
        val headers = mutableMapOf<String, String>()
        var finalUrl = raw.trim()

        // Handle full M3U block pastes (user pasting multiple lines including the URL)
        if (raw.contains("#EXTINF") || raw.contains("#KODIPROP") || raw.contains("#EXTHTTP")) {
            val lines = raw.lines()
            for (line in lines) {
                val tLine = line.trim()
                if (tLine.startsWith("http://", ignoreCase = true) || 
                    tLine.startsWith("https://", ignoreCase = true) ||
                    tLine.startsWith("rtsp://", ignoreCase = true) ||
                    tLine.startsWith("rtmp://", ignoreCase = true)) {
                    finalUrl = tLine
                } else if (tLine.startsWith("#EXTHTTP:")) {
                    try {
                        val jsonStr = tLine.removePrefix("#EXTHTTP:")
                        val json = org.json.JSONObject(jsonStr)
                        json.keys().forEach { key ->
                            headers[key] = json.getString(key)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                } else if (tLine.startsWith("#EXTVLCOPT:http-user-agent=")) {
                    headers["User-Agent"] = tLine.removePrefix("#EXTVLCOPT:http-user-agent=")
                } else if (tLine.startsWith("#KODIPROP:inputstream.adaptive.license_type=")) {
                    headers["DRM-Scheme"] = tLine.removePrefix("#KODIPROP:inputstream.adaptive.license_type=")
                } else if (tLine.startsWith("#KODIPROP:inputstream.adaptive.license_key=")) {
                    headers["DRM-LicenseUrl"] = tLine.removePrefix("#KODIPROP:inputstream.adaptive.license_key=")
                } else if (tLine.startsWith("#EXTVLCOPT:http-referrer=")) {
                    headers["Referer"] = tLine.removePrefix("#EXTVLCOPT:http-referrer=")
                }
            }
        }

        // Handle Piped URLs (URL|User-Agent=...&Referer=...)
        val match = PIPE_REGEX.find(finalUrl)
        if (match != null) {
            val urlPart = finalUrl.substring(0, match.range.first).trim()
            val params = finalUrl.substring(match.range.last + 1)
            finalUrl = urlPart
            params.split("&").forEach {
                val kv = it.split("=", limit = 2)
                if (kv.size == 2) {
                    val key = kv[0].trim()
                    val value = try {
                        java.net.URLDecoder.decode(kv[1], "UTF-8")
                    } catch (e: Exception) {
                        kv[1]
                    }
                    when {
                        key.equals("User-Agent", ignoreCase = true) -> headers["User-Agent"] = value
                        key.equals("Referer", ignoreCase = true) -> headers["Referer"] = value
                        key.equals("Origin", ignoreCase = true) -> headers["Origin"] = value
                        key.equals("Cookie", ignoreCase = true) -> headers["Cookie"] = value
                        key.equals("license_key", ignoreCase = true) || key.equals("DRM-LicenseUrl", ignoreCase = true) -> headers["DRM-LicenseUrl"] = value
                        key.equals("license_type", ignoreCase = true) || key.equals("DRM-Scheme", ignoreCase = true) -> headers["DRM-Scheme"] = value
                        else -> headers[key] = value
                    }
                }
            }
        }

        // Auto-detect ClearKey hex pair in URL or LicenseUrl
        val combined = "$finalUrl ${headers["DRM-LicenseUrl"] ?: ""}"
        val ckMatch = CLEARKEY_HEX_REGEX.find(combined)
        if (ckMatch != null) {
            headers["DRM-Scheme"] = "clearkey"
            if (!headers.containsKey("DRM-LicenseUrl")) {
                headers["DRM-LicenseUrl"] = "https://clearkey.direct/${ckMatch.groupValues[1]}:${ckMatch.groupValues[2]}"
            }
        }

        return Pair(finalUrl, headers)
    }
}
