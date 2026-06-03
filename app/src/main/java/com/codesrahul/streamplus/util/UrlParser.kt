package com.codesrahul.streamplus.util

object UrlParser {
    private val PIPE_REGEX = "(?i)(\\?\\|)|(\\?%7C)|(\\|)|(%7C)".toRegex()

    fun parsePipedUrl(raw: String): Pair<String, Map<String, String>> {
        val headers = mutableMapOf<String, String>()
        var finalUrl = raw

        // Handle full M3U block pastes (user pasting multiple lines including the URL)
        if (raw.contains("#EXTINF") || raw.contains("#KODIPROP") || raw.contains("#EXTHTTP")) {
            val lines = raw.lines()
            for (line in lines) {
                val tLine = line.trim()
                if (tLine.startsWith("http")) {
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
                    try {
                        headers[kv[0]] = java.net.URLDecoder.decode(kv[1], "UTF-8")
                    } catch (e: Exception) {
                        headers[kv[0]] = kv[1]
                    }
                }
            }
        }

        return Pair(finalUrl, headers)
    }
}
