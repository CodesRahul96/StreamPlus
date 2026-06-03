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

            items.add(M3UItem(
                name = finalName,
                url = finalUrl,
                group = if (currentGroup.isBlank()) "Uncategorized" else currentGroup,
                logoUrl = currentLogo,
                status = StreamStatus.UNKNOWN,
                headers = mergedHeaders,
                drmHeaders = currentDrmHeaders, 
                licenseUrl = currentDrmLicense,
                drmScheme = detectedScheme,
                userAgent = mergedHeaders["User-Agent"] ?: currentUserAgent,
                isPlaylist = false,
                epgId = currentEpgId,
                isVod = currentDuration > 0 || isVodGroup(currentGroup)
            ))
            Log.d("M3UParser", "Created item: $finalName, UA=${mergedHeaders["User-Agent"]}, DRM Scheme=${detectedScheme}")
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
                    Log.d("M3UParser", "Parsing EXTVLCOPT: $opt")
                    parseOption(opt, currentHeaders)
                    Log.d("M3UParser", "Current headers after EXTVLCOPT: $currentHeaders")
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
                    parseKodiProp(prop, currentHeaders, currentDrmHeaders) { scheme, license ->
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
                    if (trimmed.contains(",http", ignoreCase = true)) {
                        val commaIdx = trimmed.indexOf(",http", ignoreCase = true)
                        val namePart = trimmed.substring(0, commaIdx).trim()
                        val urlPart = trimmed.substring(commaIdx + 1).trim()
                        if (urlPart.isNotEmpty()) {
                            currentName = namePart
                            saveItem(urlPart)
                        }
                    } else {
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
        
        return items
    }

    override fun canParse(url: String, prefix: String): Boolean {
        return url.contains(".m3u", true) || 
               prefix.contains("#EXTM3U") || 
               prefix.contains("http://") || 
               prefix.contains("https://")
    }

    private fun parseProps(input: String, onProp: (String, String) -> Unit) {
        PROP_REGEX.findAll(input).forEach { match ->
            val key = match.groupValues[1].ifEmpty { match.groupValues[3] }
            val value = match.groupValues[2].ifEmpty { match.groupValues[4] }
            onProp(key, value)
        }
    }

    private fun parseOption(opt: String, headers: MutableMap<String, String>) {
        val parts = opt.split("=", limit = 2)
        if (parts.size == 2) {
            val rawKey = parts[0].trim()
            val key = rawKey.lowercase()
            var value = parts[1].trim()
            
            // Try to URL decode the value if it looks encoded (contains % and likely hex)
            if (value.contains("%")) {
                try {
                    value = java.net.URLDecoder.decode(value, "UTF-8")
                } catch (e: Exception) {
                    // Keep raw value if decode fails
                }
            }
            
            // Generic Header Parsing: "http-User-Agent", "Header-Name", etc.
            when {
                // Known mappings
                key == "http-user-agent" || key == "user-agent" -> {
                    headers["User-Agent"] = value
                    Log.d("M3UParser", ">>> FOUND User-Agent in EXTVLCOPT: $value")
                }
                key == "http-referrer" || key == "referrer" -> headers["Referer"] = value
                key == "http-origin" || key == "origin" -> headers["Origin"] = value
                // Generic http- prefix removal
                key.startsWith("http-") -> {
                    // Convert "http-x-custom-header" -> "X-Custom-Header"
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
                        value.lowercase().contains("widevine") -> "widevine"
                        value.lowercase().contains("playready") -> "playready"
                        value.lowercase().contains("clearkey") -> "clearkey"
                        else -> value
                    }
                    onDrm(scheme, null)
                }
                "license_key" -> onDrm(null, value)
                "manifest_headers", 
                "stream_headers" -> {
                    // key=value&key2=value2
                    value.split("&").forEach { 
                        val kv = it.split("=", limit = 2)
                        if (kv.size == 2) {
                            try {
                                headers[kv[0]] = java.net.URLDecoder.decode(kv[1], "UTF-8")
                            } catch (e: Exception) {
                                headers[kv[0]] = kv[1] // Fallback to raw value
                            }
                        }
                    }
                }
                "license_headers" -> {
                    // key=value&key2=value2
                    value.split("&").forEach { 
                        val kv = it.split("=", limit = 2)
                        if (kv.size == 2) {
                            try {
                                drmHeaders[kv[0]] = java.net.URLDecoder.decode(kv[1], "UTF-8")
                            } catch (e: Exception) {
                                drmHeaders[kv[0]] = kv[1] // Fallback to raw value
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
        if (s.contains("widevine") || s.contains("com.widevine.alpha")) return "widevine"
        if (s.contains("playready") || s.contains("com.microsoft.playready")) return "playready"
        if (s.contains("clearkey")) return "clearkey"
        
        // Infer from license URL only (not from stream URL — .mpd streams are not always DRM-protected)
        val l = explicitLicense?.lowercase() ?: ""
        if (l.contains("widevine") || l.endsWith("/wv") || l.contains("/widevine")) return "widevine"
        if (l.contains("playready")) return "playready"
        
        // Only infer widevine if a license URL is also present (i.e., actual DRM is being used)
        // Do NOT auto-set widevine just because it's a .mpd file — those can be unprotected DASH
        return null
    }

    private fun isVodGroup(group: String): Boolean {
        val g = group.lowercase()
        return g.contains("movie") || g.contains("vod") || g.contains("cinema") || g.contains("series") || g.contains("films")
    }
}
