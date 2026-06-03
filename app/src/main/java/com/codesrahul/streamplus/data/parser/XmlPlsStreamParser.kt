package com.codesrahul.streamplus.data.parser

import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.domain.model.StreamStatus
import java.io.InputStream

class XmlPlsStreamParser : StreamParser {

    override suspend fun parse(input: InputStream, sourceUrl: String): List<M3UItem> {
        val items = mutableListOf<M3UItem>()
        try {
            val content = input.bufferedReader().use { it.readText() }
            val trimmed = content.trim()

            if (trimmed.contains("[playlist]", ignoreCase = true)) {
                // Parse PLS
                val files = mutableMapOf<Int, String>()
                val titles = mutableMapOf<Int, String>()
                val regex = """^(File|Title)(\d+)=(.*)$""".toRegex(RegexOption.IGNORE_CASE)
                
                trimmed.lineSequence().forEach { line ->
                    val match = regex.find(line.trim())
                    if (match != null) {
                        val keyType = match.groupValues[1].lowercase()
                        val index = match.groupValues[2].toInt()
                        val value = match.groupValues[3]
                        if (keyType == "file") {
                            files[index] = value
                        } else if (keyType == "title") {
                            titles[index] = value
                        }
                    }
                }
                for ((idx, fileUrl) in files) {
                    val title = titles[idx] ?: "Stream $idx"
                    items.add(M3UItem(
                        name = title,
                        url = fileUrl,
                        group = "PLS Playlist",
                        logoUrl = "",
                        status = StreamStatus.UNKNOWN,
                        headers = emptyMap(),
                        drmHeaders = emptyMap(),
                        licenseUrl = null,
                        drmScheme = null,
                        userAgent = "",
                        isPlaylist = false
                    ))
                }
            } else if (trimmed.startsWith("<") || trimmed.contains("<?xml") || trimmed.contains("<rss") || trimmed.contains("<asx", ignoreCase = true)) {
                // XML Parsing (ASX, WPL, RSS, etc.)
                // 1. ASX Entry Parsing
                val entryRegex = """(?s)<ENTRY>(.*?)</ENTRY>""".toRegex(RegexOption.IGNORE_CASE)
                val titleRegex = """<TITLE>(.*?)</TITLE>""".toRegex(RegexOption.IGNORE_CASE)
                val refRegex = """<REF.*?href=["'](.*?)["']""".toRegex(RegexOption.IGNORE_CASE)

                val entryMatches = entryRegex.findAll(trimmed).toList()
                if (entryMatches.isNotEmpty()) {
                    entryMatches.forEach { match ->
                        val entryContent = match.groupValues[1]
                        val title = titleRegex.find(entryContent)?.groupValues?.get(1)?.trim() ?: "Stream"
                        val url = refRegex.find(entryContent)?.groupValues?.get(1)?.trim()
                        if (!url.isNullOrBlank()) {
                            items.add(M3UItem(
                                name = title,
                                url = url,
                                group = "ASX Playlist",
                                logoUrl = "",
                                status = StreamStatus.UNKNOWN,
                                headers = emptyMap(),
                                drmHeaders = emptyMap(),
                                licenseUrl = null,
                                drmScheme = null,
                                userAgent = "",
                                isPlaylist = false
                            ))
                        }
                    }
                }

                // 2. RSS Item Parsing (if entry matches were empty)
                if (items.isEmpty()) {
                    val itemRegex = """(?s)<item>(.*?)</item>""".toRegex(RegexOption.IGNORE_CASE)
                    val rssTitleRegex = """<title>(.*?)</title>""".toRegex(RegexOption.IGNORE_CASE)
                    val enclosureRegex = """<enclosure.*?url=["'](.*?)["']""".toRegex(RegexOption.IGNORE_CASE)
                    val linkRegex = """<link>(.*?)</link>""".toRegex(RegexOption.IGNORE_CASE)

                    val itemMatches = itemRegex.findAll(trimmed).toList()
                    itemMatches.forEach { match ->
                        val itemContent = match.groupValues[1]
                        val title = rssTitleRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: "RSS Stream"
                        val url = enclosureRegex.find(itemContent)?.groupValues?.get(1)?.trim() 
                            ?: linkRegex.find(itemContent)?.groupValues?.get(1)?.trim()
                        if (!url.isNullOrBlank()) {
                            items.add(M3UItem(
                                name = title,
                                url = url,
                                group = "RSS Playlist",
                                logoUrl = "",
                                status = StreamStatus.UNKNOWN,
                                headers = emptyMap(),
                                drmHeaders = emptyMap(),
                                licenseUrl = null,
                                drmScheme = null,
                                userAgent = "",
                                isPlaylist = false
                            ))
                        }
                    }
                }

                // 3. WPL Media Parsing (if items are still empty)
                if (items.isEmpty()) {
                    val mediaRegex = """<media.*?src=["'](.*?)["']""".toRegex(RegexOption.IGNORE_CASE)
                    mediaRegex.findAll(trimmed).forEach { match ->
                        val url = match.groupValues[1].trim()
                        val title = try {
                            android.net.Uri.parse(url).lastPathSegment?.substringBefore(".") ?: "WPL Stream"
                        } catch (e: Exception) { "WPL Stream" }
                        items.add(M3UItem(
                            name = title,
                            url = url,
                            group = "WPL Playlist",
                            logoUrl = "",
                            status = StreamStatus.UNKNOWN,
                            headers = emptyMap(),
                            drmHeaders = emptyMap(),
                            licenseUrl = null,
                            drmScheme = null,
                            userAgent = "",
                            isPlaylist = false
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    override fun canParse(url: String, prefix: String): Boolean {
        val trimmed = prefix.trim()
        return url.endsWith(".pls", true) || 
               url.endsWith(".asx", true) || 
               url.endsWith(".wpl", true) || 
               url.endsWith(".xml", true) || 
               trimmed.contains("[playlist]", ignoreCase = true) || 
               trimmed.startsWith("<") || 
               trimmed.contains("<?xml")
    }
}
