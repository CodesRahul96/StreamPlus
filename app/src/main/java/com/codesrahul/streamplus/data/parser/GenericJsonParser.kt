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
            findAndProcessArrays(element, list)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing generic JSON", e)
        }
        return list
    }

    fun parse(jsonString: String): List<M3UItem> {
        return parse(StringReader(jsonString))
    }

    private fun findAndProcessArrays(element: com.google.gson.JsonElement, list: MutableList<M3UItem>, depth: Int = 0) {
        if (depth > 5) return

        when {
            element.isJsonArray -> {
                val array = element.asJsonArray
                val looksLikeChannels = array.any { 
                    it.isJsonObject && (
                        hasAnyKey(it.asJsonObject, "url", "link", "stream", "uri", "source", "file", "m3u8_url", "mpd_url", "play_url", "m3uLink", "m3u_url")
                    )
                }

                if (looksLikeChannels) {
                    processArray(array, list)
                } else {
                    array.forEach { findAndProcessArrays(it, list, depth + 1) }
                }
            }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                if (hasAnyKey(obj, "url", "link", "stream", "uri", "source", "file", "m3u8_url", "m3uLink", "m3u_url")) {
                    val tv = parseSingleObject(obj, list.size)
                    if (tv != null) list.add(tv)
                } else {
                    for (key in obj.keySet()) {
                        findAndProcessArrays(obj.get(key), list, depth + 1)
                    }
                }
            }
        }
    }
    
    private fun hasAnyKey(obj: JsonObject, vararg keys: String): Boolean {
        return keys.any { findString(obj, it) != null }
    }

    private fun processArray(array: JsonArray, list: MutableList<M3UItem>) {
        for (item in array) {
            if (item.isJsonObject) {
                val obj = item.asJsonObject
                val tv = parseSingleObject(obj, list.size)
                if (tv != null) {
                    list.add(tv)
                }
            }
        }
    }

    private fun parseSingleObject(obj: JsonObject, index: Int): M3UItem? {
        val name = findString(obj, "name", "title", "channel_name", "display_name", "caption", "station", "tv_name", "channel", "label", "tvg-name", "tvg_name", "ch_name") ?: "Channel $index"
        val url = findString(obj, "url", "stream_url", "uri", "file", "src", "link", "stream", "play_url", "m3u8_url", "mpd_url", "video_url", "address", "location", "media_url", "hls_url", "dash_url", "rtsp_url", "source", "content_url", "m3uLink", "m3u_url")
        if (url.isNullOrBlank()) return null

        val logo = findString(obj, "logo", "icon", "image", "thumb", "thumbnail", "stream_icon", "channel_logo", "logo_url", "poster", "tvg-logo", "banner", "tvg_logo", "img", "cover", "picture", "imageUrl", "image_url") ?: ""
        val group = findString(obj, "group", "category", "genre", "category_name", "group_title", "group-title", "cat_name", "category_id", "folder", "playlist", "section", "group_name") ?: "Uncategorized"

        val drmLicense = findString(obj, "license_key", "drm_url", "license", "clearkey", "key", "license_url", "license_src", "drm_license", "kodi_prop_license_key")
        var drmScheme = findString(obj, "drm_scheme", "drm_type", "license_type", "license_mode", "scheme", "drm_system")
        
        var finalDrmLicense = drmLicense
        if (finalDrmLicense == null && obj.has("drm") && obj.get("drm").isJsonObject) {
             val drmObj = obj.getAsJsonObject("drm")
             finalDrmLicense = findString(drmObj, "key", "license", "url", "license_url")
             if (drmScheme == null) {
                 drmScheme = findString(drmObj, "type", "scheme", "system")
             }
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

        return M3UItem(
            name = name,
            url = url,
            group = group,
            logoUrl = logo,
            headers = parseHeaders(obj) ?: emptyMap(),
            licenseUrl = finalDrmLicense,
            drmScheme = drmScheme
        )
    }

    private fun parseHeaders(obj: JsonObject): Map<String, String>? {
        val headers = mutableMapOf<String, String>()
        val headersKey = obj.keySet().firstOrNull { 
            it.equals("headers", ignoreCase = true) || it.equals("http_headers", ignoreCase = true) || 
            it.equals("request_headers", ignoreCase = true) || it.equals("stream_headers", ignoreCase = true)
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
                return if (element.isJsonPrimitive) element.asString else element.toString()
            }
        }
        
        for (objKey in obj.keySet()) {
             val normalizedObjKey = objKey.replace("_", "").replace("-", "").lowercase()
             for (key in keys) {
                 val normalizedKey = key.replace("_", "").replace("-", "").lowercase()
                 if (normalizedKey == normalizedObjKey) {
                     val element = obj.get(objKey)
                     if (!element.isJsonNull) {
                         return if (element.isJsonPrimitive) element.asString else element.toString()
                     }
                 }
             }
        }
        return null
    }
}
