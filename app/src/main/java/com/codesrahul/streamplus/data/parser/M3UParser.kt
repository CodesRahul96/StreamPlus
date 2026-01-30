package com.codesrahul.streamplus.data.parser

import android.util.Log
import com.codesrahul.streamplus.domain.model.M3UItem
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3UParser @Inject constructor(
    private val jsonParser: GenericJsonParser,
    private val simpleListParser: SimpleListParser,
    private val kodiParser: KodiParser
) {
    private val TAG = "M3UParser"
    private val PROP_REGEX = Regex("([a-zA-Z0-9\\-_]+)=(?:\"([^\"]*)\"|([^, ]+))")

    /**
     * Universal entry point that detects format and routes to appropriate parser.
     */
    fun parseUniversal(inputStream: InputStream): List<M3UItem> {
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        val pushbackReader = java.io.PushbackReader(reader, 10)
        
        try {
            var firstChar = -1
            while (true) {
                val c = pushbackReader.read()
                if (c == -1) break
                if (!Character.isWhitespace(c)) {
                    firstChar = c
                    pushbackReader.unread(c)
                    break
                }
            }
            
            return when (firstChar.toChar()) {
                '{', '[' -> {
                    Log.d(TAG, "Detected JSON format")
                    jsonParser.parse(pushbackReader)
                }
                '#' -> {
                    // Peek more to see if it's Kodi or Star
                    val content = BufferedReader(pushbackReader).readText()
                    if (content.contains("#KODIPROP") || content.contains("EXTHTTP") || content.contains("/mp1/") || content.contains("/mp2/")) {
                        Log.d(TAG, "Detected Kodi/Star format")
                        kodiParser.parse(content)
                    } else {
                        Log.d(TAG, "Detected standard M3U format")
                        parseM3UInternal(BufferedReader(StringReader(content)))
                    }
                }
                else -> {
                    Log.d(TAG, "Unknown format, trying fallback")
                    val content = BufferedReader(pushbackReader).readText()
                    if (content.contains("http://") || content.contains("https://")) {
                        simpleListParser.parse(content)
                    } else {
                        emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Universal parse error", e)
            return emptyList()
        }
    }

    fun parse(inputStream: InputStream): List<M3UItem> {
        return parseUniversal(inputStream)
    }

    /**
     * Backward compatibility for simple strings
     */
    fun parse(content: String): List<M3UItem> {
        val trimmed = content.trim()
        return if (trimmed.startsWith("#")) {
             // Check if it's Kodi/Star style
             if (trimmed.contains("#KODIPROP") || trimmed.contains("EXTHTTP") || trimmed.contains("/mp1/") || trimmed.contains("/mp2/")) {
                 kodiParser.parse(trimmed)
             } else {
                 parseM3UInternal(BufferedReader(StringReader(trimmed)))
             }
        } else if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
             jsonParser.parse(trimmed)
        } else {
             simpleListParser.parse(trimmed)
        }
    }

    private fun parseM3UInternal(reader: BufferedReader): List<M3UItem> {
        // Star network often uses #EXTHTTP or #KODIPROP, route early if detected
        // Note: For streaming readers, we can't easily peek deep content. 
        // We'll use the robust M3U engine here instead.
        
        val channels = mutableListOf<M3UItem>()
        var line: String?

        var currentName: String = ""
        var currentLogo: String = ""
        var currentGroup: String = ""
        var currentHeaders = mutableMapOf<String, String>()
        var currentDrmScheme: String? = null
        var currentDrmLicense: String? = null
        val globalHeaders = mutableMapOf<String, String>()

        fun saveAndReset(url: String) {
            val finalHeaders = globalHeaders.toMutableMap().apply { putAll(currentHeaders) }
            channels.add(
                M3UItem(
                    name = if (currentName.isNotEmpty()) currentName else "Channel ${channels.size + 1}",
                    url = url,
                    logoUrl = currentLogo,
                    group = if (currentGroup.isNotEmpty()) currentGroup else "Uncategorized",
                    headers = finalHeaders,
                    licenseUrl = currentDrmLicense,
                    drmScheme = currentDrmScheme
                )
            )
            currentName = ""
            currentLogo = ""
            currentGroup = ""
            currentHeaders = mutableMapOf()
            currentDrmScheme = null
            currentDrmLicense = null
        }

        while (reader.readLine().also { line = it } != null) {
            var trimmedLine = line?.trim() ?: continue
            if (trimmedLine.isEmpty()) continue

            if (trimmedLine.startsWith("#EXTM3U")) {
                PROP_REGEX.findAll(trimmedLine).forEach { match ->
                    val key = match.groupValues[1].lowercase()
                    val value = if (match.groupValues[2].isNotEmpty()) match.groupValues[2] else match.groupValues[3]
                    when (key) {
                        "user-agent", "http-user-agent" -> globalHeaders["User-Agent"] = value
                        "referer", "http-referer" -> globalHeaders["Referer"] = value
                        "cookie", "cookies" -> globalHeaders["Cookie"] = value
                        "origin" -> globalHeaders["Origin"] = value
                    }
                }
                continue
            }

            if (trimmedLine.startsWith("#EXTINF:")) {
                val lastCommaIndex = trimmedLine.lastIndexOf(',')
                if (lastCommaIndex != -1) {
                    currentName = trimmedLine.substring(lastCommaIndex + 1).trim()
                }

                val propertiesPart = if (lastCommaIndex != -1) trimmedLine.substring(0, lastCommaIndex) else trimmedLine
                PROP_REGEX.findAll(propertiesPart).forEach { match ->
                    val key = match.groupValues[1].lowercase()
                    val value = if (match.groupValues[2].isNotEmpty()) match.groupValues[2] else match.groupValues[3]
                    when (key) {
                        "tvg-logo", "logo", "icon" -> currentLogo = value
                        "group-title", "group" -> currentGroup = value
                        "tvg-name", "name" -> if (value.isNotEmpty()) currentName = value
                        "user-agent" -> currentHeaders["User-Agent"] = value
                        "referer" -> currentHeaders["Referer"] = value
                        "cookie" -> currentHeaders["Cookie"] = value
                        "license-key", "license-url" -> currentDrmLicense = value
                        "drm-scheme" -> currentDrmScheme = value
                    }
                }
            } else if (trimmedLine.startsWith("#EXTGRP:")) {
                currentGroup = trimmedLine.substringAfter("#EXTGRP:").trim()
            } else if (trimmedLine.startsWith("#EXTHTTP:")) {
                try {
                    val after = trimmedLine.substringAfter("HTTP:").trim()
                    var jsonStr = after
                    var trailingUrl: String? = null
                    // If the JSON and URL are on the same line (common in some feeds), extract JSON object first
                    if (after.startsWith("{")) {
                        var brace = 0
                        var endIdx = -1
                        for (i in after.indices) {
                            val ch = after[i]
                            if (ch == '{') brace++
                            if (ch == '}') {
                                brace--
                                if (brace == 0) { endIdx = i; break }
                            }
                        }
                        if (endIdx != -1) {
                            jsonStr = after.substring(0, endIdx + 1)
                            trailingUrl = after.substring(endIdx + 1).trim().takeIf { it.isNotEmpty() }
                        }
                    }

                    val json = org.json.JSONObject(jsonStr)
                    val headers = json.optJSONObject("headers") ?: json
                    val keys = headers.keys()
                    while (keys.hasNext()) {
                        val key = keys.next() as String
                        if (key != "payload" && key != "headers") {
                            val normKey = when(key.lowercase()) {
                                "user-agent" -> "User-Agent"
                                "cookie" -> "Cookie"
                                "referer" -> "Referer"
                                else -> key
                            }
                            currentHeaders[normKey] = headers.optString(key)
                        }
                    }
                    val payload = json.optJSONObject("payload")
                    if (payload != null && payload.has("license_key")) {
                        currentDrmLicense = payload.getString("license_key")
                    }
                    // If a URL was present on the same line, treat it as the resource URL
                    if (trailingUrl != null && trailingUrl.contains("://")) {
                        saveAndReset(trailingUrl)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing EXTHTTP", e)
                }
            } else if (trimmedLine.startsWith("#KODIPROP:")) {            } else if (trimmedLine.startsWith("#KODIPROP:")) {
                val parts = trimmedLine.substringAfter("#KODIPROP:").split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    when (key) {
                        "inputstream.adaptive.license_type" -> currentDrmScheme = value
                        "inputstream.adaptive.license_key" -> currentDrmLicense = value.split("|")[0]
                        "inputstream.adaptive.stream_headers" -> {
                             value.split("&").forEach { pair ->
                                 val kv = pair.split("=", limit = 2)
                                 if (kv.size == 2) currentHeaders[kv[0]] = kv[1]
                             }
                        }
                    }
                }
            } else if (!trimmedLine.startsWith("#")) {
                if (trimmedLine.contains("://")) {
                    var finalUrl = trimmedLine
                    if (trimmedLine.contains("|")) {
                        val urlParts = trimmedLine.split("|", limit = 2)
                        finalUrl = urlParts[0].trim()
                        urlParts[1].split("&").forEach { pair ->
                            val kv = pair.split("=", limit = 2)
                            if (kv.size == 2) {
                                val normKey = when(kv[0].lowercase()) {
                                    "user-agent" -> "User-Agent"
                                    "referer" -> "Referer"
                                    "cookie" -> "Cookie"
                                    else -> kv[0]
                                }
                                currentHeaders[normKey] = kv[1]
                            }
                        }
                    }
                    saveAndReset(finalUrl)
                }
            }
        }
        return channels
    }
}
