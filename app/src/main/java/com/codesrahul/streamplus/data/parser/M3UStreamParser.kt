package com.codesrahul.streamplus.data.parser

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.domain.model.StreamStatus
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class M3UStreamParser : StreamParser {

    private val PROP_REGEX = "([\\w-]+)=\"([^\"]*)\"|([\\w-]+)=([^\\s,]+)".toRegex()
    private val PIPE_REGEX = "(?i)(\\?\\|)|(\\?%7C)|(\\|)|(%7C)".toRegex()
    
    /** Populated after parsing — contains the EPG URL from the #EXTM3U header if present. */
    var lastDetectedEpgUrl: String? = null
        private set

    override suspend fun parse(input: InputStream, sourceUrl: String): List<M3UItem> {
        val items = mutableListOf<M3UItem>()
        val reader = input.bufferedReader()
        lastDetectedEpgUrl = null  // Reset for this parse run
        
        var currentName = ""
        var currentLogo = ""
        var currentGroup = ""
        var currentDrmHeaders = mutableMapOf<String, String>()
        var currentHeaders = mutableMapOf<String, String>()
        var currentDrmScheme: String? = null
        var currentDrmLicense: String? = null
        var currentUserAgent = ""
        var currentCookie = ""
        var currentReferer = ""
        var currentOrigin = ""
        var currentEpgId: String? = null
        var currentDuration: Int = -1 // -1 means live, > 0 means VOD
        
        val saveItem = { url: String ->
            val (finalUrl, pipeHeaders) = parsePipedUrl(url)
            
            val mergedHeaders = if (currentUserAgent.isBlank() && currentCookie.isBlank() && currentReferer.isBlank() && 
                currentOrigin.isBlank() && currentHeaders.isEmpty() && pipeHeaders.isEmpty()) {
                emptyMap<String, String>()
            } else {
                val map = mutableMapOf<String, String>()
                if (currentUserAgent.isNotBlank()) map["User-Agent"] = currentUserAgent
                if (currentCookie.isNotBlank()) map["Cookie"] = currentCookie
                if (currentReferer.isNotBlank()) map["Referer"] = currentReferer
                if (currentOrigin.isNotBlank()) map["Origin"] = currentOrigin
                map.putAll(currentHeaders)
                map.putAll(pipeHeaders)
                map
            }

            val finalName = if (currentName.isBlank()) extractNameFromUrl(finalUrl) else currentName
            val detectedScheme = detectDrmScheme(currentDrmScheme, currentDrmLicense)

            val sanitizedName = finalName.take(300).trim()
            val sanitizedGroup = (if (currentGroup.isBlank()) "Uncategorized" else currentGroup).take(100).trim()

            items.add(M3UItem(
                name = sanitizedName,
                url = finalUrl,
                group = sanitizedGroup,
                logoUrl = currentLogo,
                status = StreamStatus.UNKNOWN,
                headers = mergedHeaders,
                drmHeaders = currentDrmHeaders, 
                licenseUrl = currentDrmLicense,
                drmScheme = detectedScheme,
                userAgent = mergedHeaders["User-Agent"] ?: currentUserAgent,
                isPlaylist = false,
                epgId = currentEpgId,
                isVod = run {
                    val urlLower = finalUrl.lowercase()
                    val isLive = urlLower.contains(".m3u8") ||
                            urlLower.contains("/live/") ||
                            urlLower.contains("play/live.php") ||
                            urlLower.substringBefore("?").endsWith(".ts")
                    if (isLive) false else (currentDuration >= 300 || isVodGroup(sanitizedGroup))
                }
            ))
        }

        reader.useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEach
                
                // Parse EPG URL from the #EXTM3U header line (first line only)
                if (trimmed.startsWith("#EXTM3U")) {
                    parseProps(trimmed.substringAfter("#EXTM3U")) { k, v ->
                        when (k.lowercase()) {
                            "url-tvg", "x-tvg-url" -> {
                                if (!v.isBlank()) lastDetectedEpgUrl = v
                                Log.d("M3UParser", "Detected EPG URL from header: $v")
                            }
                        }
                    }
                    return@forEach
                }
                
                if (trimmed.startsWith("#EXTINF:")) {
                    // Parse EXTINF
                    val info = trimmed.substringAfter("#EXTINF:")
                    val paramsEnd = info.lastIndexOf(',')
                    
                    // Parse duration
                    try {
                        currentDuration = info.substringBefore(" ").toInt()
                    } catch (e: Exception) {}

                    if (paramsEnd != -1) {
                        currentName = info.substring(paramsEnd + 1).trim()
                        val props = info.substring(0, paramsEnd)
                        parseProps(props) { k, v -> 
                            when(k.lowercase()) {
                                "tvg-id" -> currentEpgId = v
                                "tvg-name" -> {
                                    currentEpgId = v
                                    if (currentName.isBlank()) currentName = v
                                }
                                "tvg-logo", "logo" -> currentLogo = v
                                "group-title", "group" -> currentGroup = v
                                "user-agent", "http-user-agent" -> currentHeaders["User-Agent"] = v
                                "referer", "http-referrer" -> currentHeaders["Referer"] = v
                                "origin", "http-origin" -> currentHeaders["Origin"] = v
                                "drm-scheme", "license-type", "inputstream.adaptive.license_type", "inputstream.adaptive.license_scheme" -> currentDrmScheme = v
                                "license-key", "inputstream.adaptive.license_key" -> currentDrmLicense = v
                            }
                        }
                    } else {
                        currentName = info.trim()
                    }
                } else if (trimmed.startsWith("#EXTGRP:")) {
                    currentGroup = trimmed.substringAfter("#EXTGRP:").trim()
                } else if (trimmed.startsWith("#EXTVLCOPT:") || trimmed.startsWith("# EXTVLCOPT:")) {
                    val opt = trimmed.substringAfter("VLCOPT:")
                    parseOption(opt, currentHeaders, currentDrmHeaders) { scheme, license ->
                        if (scheme != null) currentDrmScheme = scheme
                        if (license != null) currentDrmLicense = license
                    }
                } else if (trimmed.startsWith("#EXT-X-KEY:") || trimmed.startsWith("# EXT-X-KEY:")) {
                     val info = trimmed.substringAfter("KEY:")
                     parseProps(info) { k, v ->
                         if (k == "URI") currentDrmLicense = v.trim('"')
                         if (k == "KEYFORMAT") {
                             val format = v.trim('"').lowercase()
                             if (format.contains("widevine")) currentDrmScheme = "widevine"
                             else if (format.contains("playready")) currentDrmScheme = "playready"
                             else if (format.contains("identity")) currentDrmScheme = "clearkey"
                         }
                     }
                } else if (trimmed.startsWith("#KODIPROP:") || trimmed.startsWith("# KODIPROP:")) {
                    val prop = trimmed.substringAfter("PROP:")
                    parseKodiProp(prop, currentHeaders, currentDrmHeaders, currentDrmScheme) { scheme, license ->
                        if (scheme != null) currentDrmScheme = scheme
                        if (license != null) currentDrmLicense = license
                    }
                } else if (trimmed.startsWith("#EXTHTTP", true)) {
                    try {
                        val json = if (trimmed.contains("{")) {
                            trimmed.substring(trimmed.indexOf("{")).trim()
                        } else {
                            trimmed.substringAfter("HTTP:").trim()
                        }
                        val jsonDb = JSONObject(json)
                        val iter = jsonDb.keys()
                        while (iter.hasNext()) {
                            val key = iter.next()
                            val value = jsonDb.optString(key)
                            when {
                                key.equals("user-agent", true) -> currentUserAgent = value
                                key.equals("cookie", true) -> currentCookie = value
                                key.equals("referrer", true) || key.equals("referer", true) -> currentReferer = value
                                key.equals("origin", true) -> currentOrigin = value
                                else -> currentHeaders[key] = value
                            }
                        }
                    } catch (e: Exception) { Log.e("M3UParser", "Error parsing EXTHTTP", e) }
                } else if (trimmed.startsWith("#EXT-X-SESSION-DATA:") || trimmed.startsWith("# EXT-X-SESSION-DATA:")) {
                    val info = trimmed.substringAfter("DATA:")
                    parseProps(info) { k, v ->
                        if (k == "DATA-ID" && v.contains("cookie", true)) {
                            // Some providers use session data for cookies
                        }
                        if (k == "VALUE") {
                            // Potential header value
                        }
                    }
                } else if (trimmed.startsWith("#EXT-X-STREAM-INF:")) {
                    // Treat variants as valid items if the user imports them directly
                    val info = trimmed.substringAfter(":")
                    if (currentName.isBlank()) {
                        // Extract BANDWIDTH or RESOLUTION for a better name
                        val bw = info.substringAfter("BANDWIDTH=").substringBefore(",")
                        val res = info.substringAfter("RESOLUTION=").substringBefore(",")
                        if (res != info) currentName = "Quality: $res"
                        else if (bw != info) currentName = "Bandwidth: $bw"
                    }
                } else if (!trimmed.startsWith("#")) {
                    // Ignore HTML/XML/JS tags or script content that might wrap or prepend playlists
                    val lower = trimmed.lowercase()
                    if (lower.startsWith("<!doctype") || lower.startsWith("<html") || lower.startsWith("</html") ||
                        lower.startsWith("<head") || lower.startsWith("</head") || lower.startsWith("<body") ||
                        lower.startsWith("</body") || lower.startsWith("<script") || lower.startsWith("</script") ||
                        lower.startsWith("window.location") || lower.startsWith("<") || lower.startsWith("var ")
                    ) {
                        return@forEach
                    }

                    if (trimmed.contains(",http", ignoreCase = true)) {
                        val commaIdx = trimmed.indexOf(",http", ignoreCase = true)
                        val namePart = trimmed.substring(0, commaIdx).trim()
                        val urlPart = trimmed.substring(commaIdx + 1).trim()
                        if (urlPart.isNotEmpty()) {
                            currentName = namePart
                            saveItem(urlPart)
                        }
                    } else if (trimmed.contains("://") || trimmed.endsWith(".m3u8", true) || trimmed.endsWith(".ts", true) || trimmed.endsWith(".mpd", true)) {
                        saveItem(trimmed)
                    }
                    // Reset per-item state
                    currentName = ""
                    currentLogo = ""
                    currentDrmScheme = null
                    currentDrmLicense = null
                    currentDrmHeaders = mutableMapOf()
                    currentHeaders = mutableMapOf()
                    currentUserAgent = ""
                    currentCookie = ""
                    currentReferer = ""
                    currentOrigin = ""
                    currentEpgId = null
                    currentDuration = -1
                }
            }
        }
        
        Log.d("M3UParser", "Parsed ${items.size} channels from $sourceUrl")
        return items
    }

    override fun canParse(url: String, prefix: String): Boolean {
        return url.contains(".m3u", true) || 
               url.contains(".m3u8", true) ||
               prefix.contains("#EXTM3U") || 
               prefix.contains("#EXTINF") || 
               prefix.contains("#KODIPROP") || 
               prefix.contains("#EXTVLCOPT") || 
               prefix.contains("#EXTHTTP") || 
               prefix.contains("http://") || 
               prefix.contains("https://")
    }

    private fun parseProps(input: String, onProp: (String, String) -> Unit) {
        var i = 0
        val len = input.length
        while (i < len) {
            // Skip whitespace
            while (i < len && input[i].isWhitespace()) {
                i++
            }
            if (i >= len) break
            
            // Find key name
            val keyStart = i
            while (i < len && input[i] != '=' && !input[i].isWhitespace()) {
                i++
            }
            val keyEnd = i
            if (keyStart == keyEnd) {
                i++
                continue
            }
            val key = input.substring(keyStart, keyEnd)
            
            // Skip whitespace to '='
            while (i < len && input[i].isWhitespace()) {
                i++
            }
            
            if (i < len && input[i] == '=') {
                i++ // Skip '='
                // Skip whitespace after '='
                while (i < len && input[i].isWhitespace()) {
                    i++
                }
                if (i >= len) break
                
                if (input[i] == '"') {
                    i++ // Skip '"'
                    val valStart = i
                    while (i < len && input[i] != '"') {
                        if (input[i] == '\\' && i + 1 < len && input[i + 1] == '"') {
                            i += 2
                        } else {
                            i++
                        }
                    }
                    val valEnd = i
                    val value = input.substring(valStart, valEnd).replace("\\\"", "\"")
                    if (i < len) i++ // Skip closing '"'
                    onProp(key, value)
                } else {
                    val valStart = i
                    while (i < len && !input[i].isWhitespace() && input[i] != ',') {
                        i++
                    }
                    val valEnd = i
                    val value = input.substring(valStart, valEnd)
                    onProp(key, value)
                }
            } else {
                onProp(key, "")
            }
        }
    }

    private fun parseOption(
        opt: String,
        headers: MutableMap<String, String>,
        drmHeaders: MutableMap<String, String>,
        onDrm: (String?, String?) -> Unit
    ) {
        val parts = opt.split("=", limit = 2)
        if (parts.size == 2) {
            val rawKey = parts[0].trim()
            val key = rawKey.lowercase()
            var value = parts[1].trim()
            
            if (value.contains("%")) {
                try {
                    value = java.net.URLDecoder.decode(value, "UTF-8")
                } catch (e: Exception) {
                    // Keep raw value if decode fails
                }
            }
            
            when {
                key == "clearkey" || key == "http-clearkey" -> {
                    headers["clearkey"] = value
                    drmHeaders["clearkey"] = value
                    drmHeaders["X-ClearKey-Data"] = value
                    onDrm("clearkey", value)
                }
                key == "http-user-agent" || key == "user-agent" -> {
                    headers["User-Agent"] = value
                }
                key == "http-referrer" || key == "referrer" -> headers["Referer"] = value
                key == "http-origin" || key == "origin" -> headers["Origin"] = value
                key == "http-cookie" || key == "cookie" -> headers["Cookie"] = value
                key.startsWith("http-") -> {
                    headers[rawKey.substring(5)] = value
                }
                else -> headers[rawKey] = value
            }
        }
    }

    private fun parseKodiProp(
        prop: String,
        headers: MutableMap<String, String>,
        drmHeaders: MutableMap<String, String>,
        currentDrmScheme: String?,
        onDrm: (String?, String?) -> Unit
    ) {
        val parts = prop.split("=", limit = 2)
        if (parts.size == 2) {
            val key = parts[0].trim().lowercase().removePrefix("inputstream.adaptive.")
            val rawValue = parts[1].trim().removeSurrounding("\"")
            val value = if (rawValue.contains("%")) {
                try { java.net.URLDecoder.decode(rawValue, "UTF-8") } catch (e: Exception) { rawValue }
            } else rawValue

            when(key) {
                "license_type", "license_scheme" -> {
                    val scheme = when {
                        value.lowercase().contains("clearkey") -> "clearkey"
                        value.lowercase().contains("widevine") -> "widevine"
                        value.lowercase().contains("playready") -> "playready"
                        else -> value
                    }
                    onDrm(scheme, null)
                }
                "license_key" -> {
                    val isUrl = value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)
                    val urlLower = value.lowercase()
                    val inferredScheme = when {
                        urlLower.contains("clearkey") || currentDrmScheme?.equals("clearkey", true) == true -> "clearkey"
                        urlLower.contains("widevine") || urlLower.contains("lic/wv") || urlLower.contains("proxy/lic") -> "widevine"
                        urlLower.contains("playready") -> "playready"
                        else -> currentDrmScheme
                    }

                    if (isUrl && value.contains("|")) {
                        val licUrl = value.substringBefore("|").trim()
                        val licHeaders = value.substringAfter("|").trim()
                        licHeaders.split("&").forEach { param ->
                            val kv = param.split("=", limit = 2)
                            if (kv.size == 2) {
                                try {
                                    drmHeaders[kv[0].trim()] = java.net.URLDecoder.decode(kv[1].trim(), "UTF-8")
                                } catch (e: Exception) {
                                    drmHeaders[kv[0].trim()] = kv[1].trim()
                                }
                            }
                        }
                        onDrm(inferredScheme ?: "widevine", licUrl)
                    } else if (isUrl) {
                        onDrm(inferredScheme ?: "widevine", value)
                    } else if (value.startsWith("{") && value.contains("\"keys\"")) {
                        drmHeaders["clearkey"] = value
                        drmHeaders["X-ClearKey-Data"] = value
                        onDrm("clearkey", value)
                    } else {
                        drmHeaders["clearkey"] = value
                        drmHeaders["X-ClearKey-Data"] = value
                        onDrm("clearkey", value)
                    }
                }
                "manifest_type" -> {
                    headers["X-Manifest-Type"] = value
                }
                "server_certificate" -> {
                    drmHeaders["X-Drm-Certificate"] = value
                }
                "manifest_headers", 
                "stream_headers" -> {
                    value.split("&").forEach { 
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
                "license_headers" -> {
                    value.split("&").forEach { 
                        val kv = it.split("=", limit = 2)
                        if (kv.size == 2) {
                            try {
                                drmHeaders[kv[0]] = java.net.URLDecoder.decode(kv[1], "UTF-8")
                            } catch (e: Exception) {
                                drmHeaders[kv[0]] = kv[1]
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parsePipedUrl(raw: String): Pair<String, Map<String, String>> {
        return com.codesrahul.streamplus.util.UrlParser.parsePipedUrl(raw)
    }

    private fun extractNameFromUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.lastPathSegment?.substringBefore(".") ?: "Unknown"
        } catch (e: Exception) { "Unknown" }
    }
    
    private fun detectDrmScheme(explicitScheme: String?, explicitLicense: String?): String? {
        val s = explicitScheme?.lowercase() ?: ""
        val l = explicitLicense?.lowercase() ?: ""
        
        if (s.contains("clearkey") || s.contains("org.w3.clearkey") || l.contains("clearkey")) return "clearkey"
        if (s.contains("widevine") || s.contains("com.widevine.alpha")) return "widevine"
        if (s.contains("playready") || s.contains("com.microsoft.playready")) return "playready"
        
        val licenseIsUrl = l.startsWith("http://") || l.startsWith("https://")
        if (licenseIsUrl) {
            if (l.contains("widevine") || l.endsWith("/wv") || l.contains("/widevine") || l.contains("proxy/lic")) return "widevine"
            if (l.contains("playready")) return "playready"
            return "widevine"
        }
        
        if (l.contains(":") && !l.startsWith("http")) return "clearkey"
        
        return null
    }

    private fun isVodGroup(group: String): Boolean {
        val g = group.lowercase()
        return g.contains("movie") || g.contains("vod") || g.contains("cinema") || g.contains("series") || g.contains("films")
    }
}
