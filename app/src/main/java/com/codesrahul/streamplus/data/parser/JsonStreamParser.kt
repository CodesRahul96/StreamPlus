package com.codesrahul.streamplus.data.parser

import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.domain.model.StreamStatus
import com.google.gson.stream.JsonReader
import java.io.InputStream
import java.io.InputStreamReader

class JsonStreamParser : StreamParser {

    override suspend fun parse(input: InputStream, sourceUrl: String): List<M3UItem> {
        val items = mutableListOf<M3UItem>()
        val reader = JsonReader(InputStreamReader(input, "UTF-8"))
        
        try {
            reader.use { r ->
                if (r.peek() == com.google.gson.stream.JsonToken.BEGIN_ARRAY) {
                    r.beginArray()
                    while (r.hasNext()) {
                        parseSingleItem(r)?.let { items.add(it) }
                    }
                    r.endArray()
                } else if (r.peek() == com.google.gson.stream.JsonToken.BEGIN_OBJECT) {
                    r.beginObject()
                    while (r.hasNext()) {
                        val name = r.nextName()
                        if (name == "channels" && r.peek() == com.google.gson.stream.JsonToken.BEGIN_ARRAY) {
                            r.beginArray()
                            while (r.hasNext()) {
                                parseSingleItem(r)?.let { items.add(it) }
                            }
                            r.endArray()
                        } else {
                            r.skipValue()
                        }
                    }
                    r.endObject()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    private fun parseSingleItem(reader: JsonReader): M3UItem? {
        var name = "Unknown Channel"
        var url = ""
        var group = "Uncategorized"
        var logo = ""
        val headers = mutableMapOf<String, String>()
        var drmScheme: String? = null
        var drmLicense: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            when (key.lowercase()) {
                "name" -> name = reader.nextString()
                "url" -> url = reader.nextString()
                "group" -> group = reader.nextString()
                "logo" -> logo = reader.nextString()
                "user-agent" -> headers["User-Agent"] = reader.nextString()
                "referer" -> headers["Referer"] = reader.nextString()
                "drm_scheme" -> drmScheme = reader.nextString()
                "drm_license" -> drmLicense = reader.nextString()
                "headers" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        headers[reader.nextName()] = reader.nextString()
                    }
                    reader.endObject()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        if (url.isEmpty()) return null

        return M3UItem(
            name = name,
            url = url,
            group = group,
            logoUrl = logo,
            status = StreamStatus.UNKNOWN,
            headers = headers,
            drmHeaders = headers.toMap(),
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
