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

    override suspend fun parse(input: InputStream, sourceUrl: String): List<M3UItem> {
        val items = mutableListOf<M3UItem>()
        val reader = input.bufferedReader()
        
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
            val detectedScheme = detectDrmScheme(finalUrl, currentDrmScheme, currentDrmLicense)

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
                                "tvg-id", "tvg-name" -> currentEpgId = v
                                "tvg-logo", "logo" -> currentLogo = v
                                "group-title", "group" -> currentGroup = v
                                "tvg-name" -> if (currentName.isBlank()) currentName = v
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
                    parseKodiProp(prop, currentHeaders) { scheme, license ->
                        if (scheme != null) currentDrmScheme = scheme
                        if (license != null) currentDrmLicense = license
                    }
                } else if (trimmed.startsWith("#EXTHTTP:") || trimmed.startsWith("# EXTHTTP:")) {
                    try {
                        val json = trimmed.substringAfter("HTTP:")
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
                    saveItem(trimmed)
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
                } else if (trimmed.startsWith("http", true) && !trimmed.contains("#")) {
                     // RAW URL Support: If it starts with http and we missed it above (though the !startsWith("#") should catch it)
                     // This block is technically redundant due to the else if (!trimmed.startsWith("#")) above, 
                     // but explicitly capturing it here for clarity if we change the logic above.
                     // Actually, the above block DOES catch it. 
                     // The logic "else if (!trimmed.startsWith("#"))" catches EVERYTHING that is not a comment.
                     // So raw URLs are already handled! We just need to ensure currentName has a fallback.
                }
            }
        }
        
        return items
    }

    override fun canParse(url: String, content: String): Boolean {
        return url.contains(".m3u", true) || 
               content.contains("#EXTM3U") || 
               content.contains("http://") || 
               content.contains("https://")
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

    private fun parseKodiProp(prop: String, headers: MutableMap<String, String>, onDrm: (String?, String?) -> Unit) {
        val parts = prop.split("=", limit = 2)
        if (parts.size == 2) {
            val key = parts[0].trim().lowercase()
            val value = parts[1].trim()
            when(key) {
                "inputstream.adaptive.license_type", "inputstream.adaptive.license_scheme" -> onDrm(value, null)
                "inputstream.adaptive.license_key" -> onDrm(null, value)
                "inputstream.adaptive.manifest_headers", 
                "inputstream.adaptive.license_headers", 
                "inputstream.adaptive.stream_headers" -> {
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
    
    private fun detectDrmScheme(url: String, explicitScheme: String?, explicitLicense: String?): String? {
        if (!explicitScheme.isNullOrBlank()) {
             if (explicitScheme == "com.widevine.alpha") return "widevine"
             if (explicitScheme == "com.microsoft.playready") return "playready"
             return explicitScheme
        }
        if (url.contains(".mpd") || url.contains(".mpd?")) return "widevine"
        // Let DrmResolver handle the default based on license URL content
        return null
    }

    private fun isVodGroup(group: String): Boolean {
        val g = group.lowercase()
        return g.contains("movie") || g.contains("vod") || g.contains("cinema") || g.contains("series") || g.contains("films")
    }
}
