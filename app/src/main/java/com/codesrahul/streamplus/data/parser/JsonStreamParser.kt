package com.codesrahul.streamplus.data.parser

import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.domain.model.StreamStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

class JsonStreamParser : StreamParser {

    override suspend fun parse(input: InputStream, sourceUrl: String): List<M3UItem> {
        val items = mutableListOf<M3UItem>()
        try {
            val content = input.bufferedReader().use { it.readText() }
            val trimmed = content.trim()
            if (trimmed.startsWith("[")) {
                val jsonArray = JSONArray(trimmed)
                scanJsonForStreams(jsonArray, items)
            } else if (trimmed.startsWith("{")) {
                val jsonObject = JSONObject(trimmed)
                scanJsonForStreams(jsonObject, items)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    private fun scanJsonForStreams(jsonElement: Any, items: MutableList<M3UItem>, parentGroup: String = "Uncategorized") {
        when (jsonElement) {
            is JSONArray -> {
                for (i in 0 until jsonElement.length()) {
                    val item = jsonElement.opt(i)
                    if (item != null) {
                        if (item is JSONObject && isStreamObject(item)) {
                            parseStreamObject(item, parentGroup)?.let { items.add(it) }
                        } else {
                            scanJsonForStreams(item, items, parentGroup)
                        }
                    }
                }
            }
            is JSONObject -> {
                if (isStreamObject(jsonElement)) {
                    parseStreamObject(jsonElement, parentGroup)?.let { items.add(it) }
                } else {
                    val groupName = listOf("name", "title", "category", "group")
                        .mapNotNull { k -> jsonElement.optString(k).takeIf { it.isNotBlank() } }
                        .firstOrNull() ?: parentGroup

                    val keys = jsonElement.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = jsonElement.opt(key)
                        if (value != null) {
                            scanJsonForStreams(value, items, groupName)
                        }
                    }
                }
            }
        }
    }

    private fun isStreamObject(obj: JSONObject): Boolean {
        // Must contain at least one URL-like key
        return obj.has("url") || obj.has("link") || obj.has("stream") || 
               obj.has("stream_url") || obj.has("source") || obj.has("file") || 
               obj.has("src") || obj.has("uri") || obj.has("embed") || 
               obj.has("target") || obj.has("playback_url") || obj.has("hls") || obj.has("mpd")
    }

    private fun parseStreamObject(obj: JSONObject, fallbackGroup: String = "Uncategorized"): M3UItem? {
        val url = listOf("url", "link", "stream", "stream_url", "source", "file", "src", "uri", "embed", "target", "playback_url", "hls", "mpd")
            .mapNotNull { key -> obj.optString(key).takeIf { it.isNotBlank() } }
            .firstOrNull() ?: return null

        val name = listOf("name", "title", "channel_name", "channel_title", "stream_name", "label", "caption", "display_name")
            .mapNotNull { key -> obj.optString(key).takeIf { it.isNotBlank() } }
            .firstOrNull() ?: try {
                android.net.Uri.parse(url).lastPathSegment?.substringBefore(".") ?: "Unknown Channel"
            } catch (e: Exception) { "Unknown Channel" }

        val logo = listOf("logo", "logo_url", "image", "icon", "icon_url", "thumbnail", "thumb")
            .mapNotNull { key -> obj.optString(key).takeIf { it.isNotBlank() } }
            .firstOrNull() ?: ""

        val group = listOf("group", "grouptitle", "group-title", "category", "category_name", "genre")
            .mapNotNull { key -> obj.optString(key).takeIf { it.isNotBlank() } }
            .firstOrNull() ?: fallbackGroup

        val headers = mutableMapOf<String, String>()
        listOf("user-agent", "useragent", "ua").forEach { key ->
            val v = obj.optString(key)
            if (v.isNotBlank()) headers["User-Agent"] = v
        }
        listOf("referer", "referrer").forEach { key ->
            val v = obj.optString(key)
            if (v.isNotBlank()) headers["Referer"] = v
        }
        val cookie = obj.optString("cookie")
        if (cookie.isNotBlank()) headers["Cookie"] = cookie

        // Nested headers object support
        val headersKeys = listOf("headers", "http_headers", "request_headers")
        for (hk in headersKeys) {
            val nestedHeaders = obj.optJSONObject(hk)
            if (nestedHeaders != null) {
                val keys = nestedHeaders.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = nestedHeaders.optString(k)
                    if (v.isNotBlank()) {
                        headers[k] = v
                    }
                }
            }
        }

        val drmScheme = listOf("drm_scheme", "drmscheme", "drm")
            .mapNotNull { key -> obj.optString(key).takeIf { it.isNotBlank() } }
            .firstOrNull()

        val drmLicense = listOf("drm_license", "drmlicense", "license", "license_url")
            .mapNotNull { key -> obj.optString(key).takeIf { it.isNotBlank() } }
            .firstOrNull()

        val drmHeaders = mutableMapOf<String, String>()
        val drmHeadersKeys = listOf("drm_headers", "drmheaders")
        for (dhk in drmHeadersKeys) {
            val nestedDrm = obj.optJSONObject(dhk)
            if (nestedDrm != null) {
                val keys = nestedDrm.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = nestedDrm.optString(k)
                    if (v.isNotBlank()) {
                        drmHeaders[k] = v
                    }
                }
            }
        }
        if (drmHeaders.isEmpty()) {
            drmHeaders.putAll(headers)
        }

        return M3UItem(
            name = name.take(300).trim(),
            url = url,
            group = group.take(100).trim(),
            logoUrl = logo,
            status = StreamStatus.UNKNOWN,
            headers = headers,
            drmHeaders = drmHeaders,
            licenseUrl = drmLicense,
            drmScheme = drmScheme,
            userAgent = headers["User-Agent"] ?: "",
            isPlaylist = false
        )
    }

    override fun canParse(url: String, prefix: String): Boolean {
        val trimmed = prefix.trim()
        return url.endsWith(".json", true) || trimmed.startsWith("{") || trimmed.startsWith("[")
    }
}
