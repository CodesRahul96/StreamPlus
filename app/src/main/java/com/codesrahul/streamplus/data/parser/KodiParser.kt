package com.codesrahul.streamplus.data.parser

import android.util.Log
import com.codesrahul.streamplus.domain.model.M3UItem
import java.io.BufferedReader
import java.io.StringReader

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KodiParser @Inject constructor() {
    private val TAG = "KodiParser"
    private val PROP_REGEX = Regex("([a-zA-Z0-9\\-_]+)=(?:\"([^\"]*)\"|([^, ]+))")

    fun parse(content: String): List<M3UItem> {
        val channels = mutableListOf<M3UItem>()
        val reader = BufferedReader(StringReader(content))
        var line: String?

        var currentName: String = ""
        var currentLogo: String = ""
        var currentGroup: String = ""
        var currentHeaders = mutableMapOf<String, String>()
        var currentDrmScheme: String? = null
        var currentDrmLicense: String? = null
        var currentUrl: String? = null

        fun saveAndReset() {
            if (currentUrl != null) {
                channels.add(
                    M3UItem(
                        name = if (currentName.isNotEmpty()) currentName else "Channel ${channels.size + 1}",
                        url = currentUrl!!,
                        logoUrl = currentLogo,
                        group = if (currentGroup.isNotEmpty()) currentGroup else "Uncategorized",
                        headers = currentHeaders.toMap(),
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
                currentUrl = null
            }
        }

        while (reader.readLine().also { line = it } != null) {
            try {
                var trimmedLine = line?.trim() ?: continue
                if (trimmedLine.isEmpty()) continue

                if (trimmedLine.startsWith("# ")) {
                    trimmedLine = "#" + trimmedLine.substring(1).trimStart()
                }

                if (trimmedLine.startsWith("#EXTM3U")) continue

                else if (trimmedLine.startsWith("#EXTINF:")) {
                    saveAndReset()
                    val lastCommaIndex = trimmedLine.lastIndexOf(',')
                    if (lastCommaIndex != -1) {
                        currentName = trimmedLine.substring(lastCommaIndex + 1).trim()
                    }

                    val propertiesPart = if (lastCommaIndex != -1) trimmedLine.substring(0, lastCommaIndex) else trimmedLine
                    val matches = PROP_REGEX.findAll(propertiesPart)
                    for (match in matches) {
                        val key = match.groupValues[1].lowercase() 
                        val value = if (match.groupValues[2].isNotEmpty()) match.groupValues[2] else match.groupValues[3]
                        
                        when (key) {
                            "tvg-logo", "tvg_logo", "logo", "icon", "thumb" -> currentLogo = value
                            "group-title", "group_title", "group" -> currentGroup = value
                            "tvg-name", "tvg_name", "name" -> if (value.isNotEmpty()) currentName = value 
                            "user-agent", "http-user-agent" -> currentHeaders["User-Agent"] = value
                            "referer", "http-referer" -> currentHeaders["Referer"] = value
                            "cookie", "http-cookie" -> currentHeaders["Cookie"] = value
                            "origin", "http-origin" -> currentHeaders["Origin"] = value
                            "license-key", "license-url", "key" -> currentDrmLicense = value
                            "license-type", "drm-scheme", "drm" -> currentDrmScheme = value
                        }
                    }

                } else if (trimmedLine.startsWith("#EXTHTTP:")) {
                    if (currentUrl != null) saveAndReset()
                    val jsonStr = trimmedLine.substringAfter("HTTP:").trim()
                    try {
                        val jsonObject = org.json.JSONObject(jsonStr)
                        val headers = jsonObject.optJSONObject("headers")
                        if (headers != null) {
                            val keys = headers.keys()
                            while (keys.hasNext()) {
                                val key = keys.next() as String
                                currentHeaders[key] = headers.getString(key)
                            }
                        } else {
                            val keys = jsonObject.keys()
                            while (keys.hasNext()) {
                                val key = keys.next() as String
                                if (key != "payload" && key != "headers") {
                                    val normKey = when(key.lowercase()) {
                                        "cookie" -> "Cookie"
                                        "user-agent" -> "User-Agent"
                                        "referer" -> "Referer"
                                        "origin" -> "Origin"
                                        else -> key
                                    }
                                    currentHeaders[normKey] = jsonObject.getString(key)
                                }
                            }
                        }
                        val payload = jsonObject.optJSONObject("payload")
                        if (payload != null && payload.has("license_key")) {
                            currentDrmLicense = payload.getString("license_key")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse EXTHTTP JSON", e)
                    }

                } else if (trimmedLine.startsWith("#KODIPROP:")) {
                    if (currentUrl != null) saveAndReset()
                    val parts = trimmedLine.substringAfter("#KODIPROP:").split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        when (key) {
                            "inputstream.adaptive.license_type" -> {
                                currentDrmScheme = when(value) {
                                    "com.widevine.alpha" -> "widevine"
                                    "com.microsoft.playready" -> "playready"
                                    "org.w3.clearkey" -> "clearkey" 
                                    else -> value
                                }
                            }
                            "inputstream.adaptive.license_key" -> currentDrmLicense = value.split("|")[0] 
                            "inputstream.adaptive.stream_headers" -> {
                                value.split("&").forEach { pair ->
                                    val kv = pair.split("=", limit = 2)
                                    if (kv.size == 2) {
                                        val normKey = when(kv[0].lowercase()) {
                                            "user-agent" -> "User-Agent"
                                            "referer" -> "Referer"
                                            "cookie" -> "Cookie"
                                            "origin" -> "Origin"
                                            else -> kv[0]
                                        }
                                        currentHeaders[normKey] = kv[1]
                                    }
                                }
                            }
                        }
                    }
                } else if (trimmedLine.startsWith("#EXTVLCOPT:")) {
                    if (currentUrl != null) saveAndReset()
                    val parts = trimmedLine.substringAfter("#EXTVLCOPT:").split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim().lowercase()
                        val value = parts[1].trim()
                        when(key) {
                            "http-user-agent", "user-agent" -> currentHeaders["User-Agent"] = value
                            "http-referrer", "http-referer", "referer" -> currentHeaders["Referer"] = value
                            "http-origin", "origin" -> currentHeaders["Origin"] = value
                            "http-cookie", "cookie" -> currentHeaders["Cookie"] = value
                        }
                    }
                } else if (trimmedLine.startsWith("#EXTGRP:")) {
                    currentGroup = trimmedLine.substringAfter("#EXTGRP:").trim()
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
                        
                        if (currentName.isEmpty()) {
                            currentName = extractNameFromStarUrl(finalUrl) ?: ""
                        }
                        
                        currentUrl = finalUrl
                        saveAndReset() // In Kodi format/M3U simple, URL usually ends the block
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing line: $line", e)
            }
        }

        saveAndReset()
        return channels
    }

    private fun extractNameFromStarUrl(url: String): String? {
        return when {
            url.contains("/mp1/") -> url.substringAfter("/mp1/").substringBefore("/").replace("-", " ").capitalizeWords()
            url.contains("/mp2/") -> url.substringAfter("/mp2/").substringBefore("/").replace("-", " ").capitalizeWords()
            url.contains("/live/") -> {
                val segment = url.substringAfter("/live/").substringAfter("/")
                (if (segment.contains("/")) segment.substringBefore("/") else segment.substringBefore(".")).replace("-", " ").capitalizeWords()
            }
            else -> null
        }
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { 
        it.replaceFirstChar { char -> char.titlecase() } 
    }
}
