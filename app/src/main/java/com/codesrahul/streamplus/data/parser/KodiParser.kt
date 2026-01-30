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
        val globalHeaders = mutableMapOf<String, String>()
        val clearKeyRegex = Regex("^[0-9a-fA-F]{32}:[0-9a-fA-F]{32}$")


        while (reader.readLine().also { line = it } != null) {
            try {
                var trimmedLine = line?.trim() ?: continue
                if (trimmedLine.isEmpty()) continue

                // Handle common alternative prefixes like "/" or "# " or "//"
                while (trimmedLine.startsWith("/") || trimmedLine.startsWith("#")) {
                    if (trimmedLine.startsWith("# ")) {
                         trimmedLine = trimmedLine.substring(2).trimStart()
                         break
                    }
                    trimmedLine = trimmedLine.substring(1).trimStart()
                }
                
                // Now trimmedLine is likely just the tag name: "EXTINF", "EXTHTTP", etc.
                
                if (trimmedLine.startsWith("EXTM3U", ignoreCase = true)) {
                    extractProperties(trimmedLine, globalHeaders)
                    continue
                }

                if (trimmedLine.startsWith("EXTINF:", ignoreCase = true)) {
                    val lastCommaIndex = trimmedLine.lastIndexOf(',')
                    if (lastCommaIndex != -1) {
                        currentName = trimmedLine.substring(lastCommaIndex + 1).trim()
                    }
                    val propertiesPart = if (lastCommaIndex != -1) trimmedLine.substring(0, lastCommaIndex) else trimmedLine
                    extractProperties(propertiesPart, currentHeaders, logoCallback = { currentLogo = it }, groupCallback = { currentGroup = it }, nameCallback = { currentName = it }, drmLicenseCallback = { currentDrmLicense = it }, drmSchemeCallback = { currentDrmScheme = it })
                } 
                else if (trimmedLine.startsWith("EXTHTTP:", ignoreCase = true)) {
                    val jsonStr = trimmedLine.substringAfter("HTTP:").trim()
                    parseHttpJson(jsonStr, currentHeaders) { currentDrmLicense = it }
                } 
                else if (trimmedLine.startsWith("KODIPROP:", ignoreCase = true)) {
                    val parts = trimmedLine.substringAfter("KODIPROP:").split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        parseKodiProp(key, value, currentHeaders, { currentDrmScheme = it }, { currentDrmLicense = it })
                    }
                } 
                else if (trimmedLine.startsWith("EXTVLCOPT:", ignoreCase = true)) {
                    val parts = trimmedLine.substringAfter("EXTVLCOPT:").split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim().lowercase()
                        val value = parts[1].trim()
                        normalizeHeader(key, value, currentHeaders)
                    }
                } 
                else if (trimmedLine.startsWith("EXTGRP:", ignoreCase = true)) {
                    currentGroup = trimmedLine.substringAfter("EXTGRP:").trim()
                } 
                else {
                    // Check if it's a URL (doesn't start with '#' or '/' originally)
                    val originalLine = line?.trim() ?: ""
                    if (!originalLine.startsWith("#") && !originalLine.startsWith("/")) {
                        if (originalLine.contains("://")) {
                            val urlWithPipe = originalLine.replace("%7C", "|")
                            var finalUrl = urlWithPipe // Initialize finalUrl
                            if (urlWithPipe.contains("|")) {
                                val urlParts = urlWithPipe.split("|", limit = 2)
                                finalUrl = urlParts[0].trim()
                                urlParts[1].split("&").forEach { pair ->
                                    val kv = pair.split("=", limit = 2)
                                    if (kv.size == 2) normalizeHeader(kv[0], kv[1], currentHeaders)
                                }
                            }
                            
                            val snifferResult = MetadataSniffer.sniffMetadata(finalUrl, currentName)
                            
                            val finalScheme = if (currentDrmScheme == null && currentDrmLicense != null && clearKeyRegex.matches(currentDrmLicense!!)) {
                                "clearkey"
                            } else {
                                currentDrmScheme ?: snifferResult.drmScheme
                            }

                            val finalHeaders = globalHeaders.toMutableMap().apply { putAll(currentHeaders) }

                            channels.add(
                                M3UItem(
                                    name = snifferResult.name ?: "Channel ${channels.size + 1}",
                                    url = finalUrl,
                                    logoUrl = if (currentLogo.isNotEmpty()) currentLogo else (snifferResult.logo ?: ""),
                                    group = if (currentGroup.isNotEmpty()) currentGroup else (snifferResult.group ?: "Uncategorized"),
                                    headers = finalHeaders.toMap(),
                                    licenseUrl = if (currentDrmLicense != null) currentDrmLicense else snifferResult.drmLicense,
                                    drmScheme = finalScheme,
                                    isPlaylist = finalUrl.lowercase().run { contains(".m3u") || contains(".json") || contains("m3u=1") }
                                )
                            )
                            // Reset per-entry state
                            currentName = ""
                            currentLogo = ""
                            currentGroup = ""
                            currentHeaders = mutableMapOf()
                            currentDrmScheme = null
                            currentDrmLicense = null
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing line: $line", e)
            }
        }
        return channels
    }

    private fun extractProperties(
        line: String, 
        headerMap: MutableMap<String, String>,
        logoCallback: ((String) -> Unit)? = null,
        groupCallback: ((String) -> Unit)? = null,
        nameCallback: ((String) -> Unit)? = null,
        drmLicenseCallback: ((String) -> Unit)? = null,
        drmSchemeCallback: ((String) -> Unit)? = null
    ) {
        val matches = PROP_REGEX.findAll(line)
        for (match in matches) {
            val key = match.groupValues[1].lowercase() 
            val value = if (match.groupValues[2].isNotEmpty()) match.groupValues[2] else match.groupValues[3]
            
            when (key) {
                "tvg-logo", "tvg_logo", "logo", "icon", "thumb" -> logoCallback?.invoke(value)
                "group-title", "group_title", "group" -> groupCallback?.invoke(value)
                "tvg-name", "tvg_name", "name" -> if (value.isNotEmpty()) nameCallback?.invoke(value)
                "license-key", "license-url", "key" -> drmLicenseCallback?.invoke(value)
                "license-type", "drm-scheme", "drm" -> drmSchemeCallback?.invoke(value)
                else -> normalizeHeader(key, value, headerMap)
            }
        }
    }

    private fun normalizeHeader(key: String, value: String, map: MutableMap<String, String>) {
        val normKey = when (key.lowercase().replace("http-", "")) {
            "user-agent", "ua" -> "User-Agent"
            "referer", "referrer" -> "Referer"
            "cookie", "cookies" -> "Cookie"
            "origin" -> "Origin"
            "accept" -> "Accept"
            else -> key
        }
        map[normKey] = value
    }

    private fun parseHttpJson(jsonStr: String, headersMap: MutableMap<String, String>, drmCallback: (String) -> Unit) {
        try {
            val jsonObject = org.json.JSONObject(jsonStr)
            val headers = jsonObject.optJSONObject("headers") ?: jsonObject
            val keys = headers.keys()
            while (keys.hasNext()) {
                val key = keys.next() as String
                if (key != "payload" && key != "headers") {
                    normalizeHeader(key, headers.optString(key), headersMap)
                }
            }
            val payload = jsonObject.optJSONObject("payload")
            if (payload != null && payload.has("license_key")) {
                drmCallback(payload.getString("license_key"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse EXTHTTP JSON", e)
        }
    }

    private fun parseKodiProp(key: String, value: String, headersMap: MutableMap<String, String>, schemeCallback: (String) -> Unit, drmCallback: (String) -> Unit) {
        when (key) {
            "inputstream.adaptive.license_type" -> {
                schemeCallback(when(value) {
                    "com.widevine.alpha" -> "widevine"
                    "com.microsoft.playready" -> "playready"
                    "org.w3.clearkey" -> "clearkey" 
                    else -> value
                })
            }
            "inputstream.adaptive.license_key" -> drmCallback(value.split("|")[0])
            "inputstream.adaptive.stream_headers" -> {
                value.split("&").forEach { pair ->
                    val kv = pair.split("=", limit = 2)
                    if (kv.size == 2) normalizeHeader(kv[0], kv[1], headersMap)
                }
            }
        }
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { 
        it.replaceFirstChar { char -> char.titlecase() } 
    }
}
