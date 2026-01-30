package com.codesrahul.streamplus.data.parser

import android.util.Log
import com.codesrahul.streamplus.domain.model.M3UItem
import java.io.BufferedReader
import java.io.StringReader

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleListParser @Inject constructor() {
    private val TAG = "SimpleListParser"

    fun parse(content: String): List<M3UItem> {
        val list = mutableListOf<M3UItem>()
        val reader = BufferedReader(StringReader(content))
        var line: String? = reader.readLine()
        var lastText: String? = null

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                line = reader.readLine()
                continue
            }

            var finalUrl: String? = null
            var finalName: String? = null

            // Pattern 1: Direct URL or Name followed by URL
            if (trimmed.startsWith("http")) {
                finalUrl = trimmed
                finalName = lastText // Use preceding line as name if available
            } else if (trimmed.contains("://")) {
                // Check for delimiters like " ", "|", ",", ":", "-"
                val delimiters = listOf("|", ",", " - ", " : ", " ", ":")
                var foundComplex = false
                for (delim in delimiters) {
                    val index = trimmed.lastIndexOf(delim)
                    if (index > 2) {
                        val part2 = trimmed.substring(index + delim.length).trim()
                        if (part2.startsWith("http")) {
                            finalName = trimmed.substring(0, index).trim()
                            finalUrl = part2
                            foundComplex = true
                            break
                        }
                    }
                }
                
                if (!foundComplex) {
                    // Fallback to searching for the first "http" in the string
                    val httpIndex = trimmed.indexOf("http")
                    if (httpIndex > 0) {
                        finalName = trimmed.substring(0, httpIndex).trim().trimEnd('-', ':', '|')
                        finalUrl = trimmed.substring(httpIndex).trim()
                    } else if (httpIndex == 0) {
                        finalUrl = trimmed
                    }
                }
            } else {
                // Not a URL, store as potential name for the next line
                lastText = trimmed
            }

            if (finalUrl != null) {
                val snifferResult = MetadataSniffer.sniffMetadata(finalUrl, finalName)
                list.add(
                    M3UItem(
                        name = snifferResult.name ?: "Channel ${list.size + 1}",
                        url = finalUrl,
                        logoUrl = snifferResult.logo ?: "",
                        group = snifferResult.group ?: "Uncategorized",
                        drmScheme = snifferResult.drmScheme,
                        licenseUrl = snifferResult.drmLicense,
                        isPlaylist = finalUrl.lowercase().run { contains(".m3u") || contains(".json") || contains("m3u=1") }
                    )
                )
                lastText = null // Reset after using
            }

            line = reader.readLine()
        }

        if (list.isNotEmpty()) {
            Log.i(TAG, "Parsed Simple List: ${list.size} channels")
        }
        return list
    }
}
