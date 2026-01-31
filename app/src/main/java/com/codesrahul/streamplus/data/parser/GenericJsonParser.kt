package com.codesrahul.streamplus.data.parser

import android.util.Log
import com.codesrahul.streamplus.domain.model.M3UItem
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.Reader
import java.io.StringReader

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenericJsonParser @Inject constructor() {
    private val TAG = "GenericJsonParser"

    fun parse(reader: Reader): List<M3UItem> {
        val list = mutableListOf<M3UItem>()
        try {
            val element = JsonParser().parse(reader)
            scanForItems(element, list)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing generic JSON", e)
        }
        return list
    }

    fun parse(jsonString: String): List<M3UItem> {
        return parse(StringReader(jsonString))
    }

    private fun scanForItems(element: com.google.gson.JsonElement, list: MutableList<M3UItem>, depth: Int = 0) {
        if (depth > 20) return // Increased depth for nested structures

        when {
            element.isJsonArray -> {
                element.asJsonArray.forEach { scanForItems(it, list, depth + 1) }
            }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                // Check if this object ITSELF is a channel
                val url = findPotentialUrl(obj)
                if (url != null) {
                    val item = parseSingleObject(obj, list.size)
                    if (item != null) list.add(item)
                } else {
                    // Otherwise keep digging
                    for (key in obj.keySet()) {
                        scanForItems(obj.get(key), list, depth + 1)
                    }
                }
            }
        }
    }

    private fun findPotentialUrl(obj: JsonObject): String? {
        val keys = listOf("url", "link", "stream", "uri", "source", "file", "m3u8_url", "mpd_url", "play_url", "m3uLink", "m3u_url", "video")
        for (key in keys) {
            val value = findString(obj, key)
            if (value != null && (value.startsWith("http") || value.startsWith("rtsp") || value.contains("://"))) {
                return value
            }
        }
        // Try all string fields if nothing obvious found
        for (key in obj.keySet()) {
            val element = obj.get(key)
            if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
                val value = element.asString
                if (value.startsWith("http") && (value.contains(".m3u8") || value.contains(".mpd") || value.contains(".ts") || value.contains("/live/"))) {
                    return value
                }
            }
        }
        return null
    }

    private fun parseSingleObject(obj: JsonObject, index: Int): M3UItem? {
        val url = findPotentialUrl(obj) ?: return null
        
        val name = findString(obj, "name", "title", "channel_name", "display_name", "caption", "station", "tv_name", "channel", "label", "tvg-name", "tvg_name", "ch_name") ?: "Channel $index"
        val logo = findString(obj, "logo", "icon", "image", "thumb", "thumbnail", "stream_icon", "channel_logo", "logo_url", "poster", "tvg-logo", "banner", "tvg_logo", "img", "cover", "picture", "imageUrl", "image_url") ?: ""
        val group = findString(obj, "group", "category", "genre", "category_name", "group_title", "group-title", "cat_name", "category_id", "folder", "playlist", "section", "group_name") ?: "Uncategorized"

        val drmLicense = findString(obj, "license_key", "drm_url", "license", "clearkey", "key", "license_url", "license_src", "drm_license", "kodi_prop_license_key", "drmLicense")
        var drmScheme = findString(obj, "drm_scheme", "drm_type", "license_type", "license_mode", "scheme", "drm_system", "drmScheme")
        
        var finalDrmLicense = drmLicense
        var drmHeaders = mutableMapOf<String, String>()
        if (finalDrmLicense == null && obj.has("drm") && obj.get("drm").isJsonObject) {
             val drmObj = obj.getAsJsonObject("drm")
             finalDrmLicense = findString(drmObj, "key", "license", "url", "license_url")
             if (drmScheme == null) {
                 drmScheme = findString(drmObj, "type", "scheme", "system")
             }
             parseHeadersInObject(drmObj)?.let { drmHeaders.putAll(it) }
        }

        if (drmScheme == null && finalDrmLicense != null) {
            when {
                finalDrmLicense.contains("clearkey", ignoreCase = true) -> drmScheme = "clearkey"
                finalDrmLicense.contains("widevine", ignoreCase = true) || finalDrmLicense.contains("wv", ignoreCase = true) -> drmScheme = "widevine"
                finalDrmLicense.contains("playready", ignoreCase = true) || finalDrmLicense.contains("pr", ignoreCase = true) -> drmScheme = "playready"
                finalDrmLicense.contains("keyid=", ignoreCase = true) && finalDrmLicense.contains("key=", ignoreCase = true) -> drmScheme = "clearkey"
                finalDrmLicense.matches(Regex("^[0-9a-fA-F]{32}:[0-9a-fA-F]{32}$")) -> drmScheme = "clearkey"
            }
        }
        
        if (finalDrmLicense != null && drmScheme == null && url.contains(".mpd", ignoreCase = true)) {
             drmScheme = "widevine"
        }

        val snifferResult = MetadataSniffer.sniffMetadata(url, name)
        return M3UItem(
            name = snifferResult.name ?: name ?: "Channel ${index + 1}",
            url = url,
            group = group ?: snifferResult.group ?: "Uncategorized",
            logoUrl = logo ?: snifferResult.logo ?: "",
            headers = parseHeaders(obj) ?: emptyMap(),
            drmHeaders = drmHeaders.toMap(),
            licenseUrl = finalDrmLicense ?: snifferResult.drmLicense,
            drmScheme = drmScheme ?: snifferResult.drmScheme,
            isPlaylist = url.lowercase().run { contains(".m3u") || contains(".json") || contains("m3u=1") }
        )
    }

    private fun parseHeaders(obj: JsonObject): Map<String, String>? {
        return parseHeadersInObject(obj)
    }

    private fun parseHeadersInObject(obj: JsonObject): Map<String, String>? {
        val headers = mutableMapOf<String, String>()
        val headersKey = obj.keySet().firstOrNull { 
            it.equals("headers", ignoreCase = true) || it.equals("http_headers", ignoreCase = true) || 
            it.equals("request_headers", ignoreCase = true) || it.equals("stream_headers", ignoreCase = true) ||
            it.equals("license_headers", ignoreCase = true) || it.equals("drm_headers", ignoreCase = true)
        }
        
        if (headersKey != null && obj.get(headersKey).isJsonObject) {
            val headersObj = obj.getAsJsonObject(headersKey)
            for (key in headersObj.keySet()) {
                val value = headersObj.get(key)
                if (!value.isJsonNull) {
                    headers[normalizeHeaderKey(key)] = value.asString
                }
            }
        }

        findString(obj, "user-agent", "user_agent", "ua", "http_user_agent")?.let { headers["User-Agent"] = it }
        findString(obj, "referer", "referrer", "http-referer", "http_referrer")?.let { headers["Referer"] = it }
        findString(obj, "cookie", "cookies", "http_cookie")?.let { headers["Cookie"] = it }
        findString(obj, "origin", "http-origin", "http_origin")?.let { headers["Origin"] = it }

        return if (headers.isNotEmpty()) headers else null
    }

    private fun normalizeHeaderKey(key: String): String {
        return when (key.lowercase().replace("_", "-")) {
            "cookie" -> "Cookie"
            "user-agent" -> "User-Agent"
            "referer", "referrer" -> "Referer"
            "origin" -> "Origin"
            "authorization" -> "Authorization"
            "content-type" -> "Content-Type"
            "accept" -> "Accept"
            else -> key
        }
    }

    private fun findString(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            if (obj.has(key) && !obj.get(key).isJsonNull) {
                val element = obj.get(key)
                if (element.isJsonPrimitive) return element.asString
            }
        }
        
        for (objKey in obj.keySet()) {
             val normalizedObjKey = objKey.replace("_", "").replace("-", "").lowercase()
             for (key in keys) {
                 val normalizedKey = key.replace("_", "").replace("-", "").lowercase()
                 if (normalizedKey == normalizedObjKey) {
                     val element = obj.get(objKey)
                     if (!element.isJsonNull && element.isJsonPrimitive) {
                         return element.asString
                     }
                 }
             }
        }
        return null
    }
}
