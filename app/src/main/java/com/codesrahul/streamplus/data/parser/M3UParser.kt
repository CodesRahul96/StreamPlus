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
    private val xmlParser: GenericXmlParser,
    private val simpleListParser: SimpleListParser,
    private val kodiParser: KodiParser
) {
    private val TAG = "M3UParser"

    fun parseUniversal(inputStream: InputStream): List<M3UItem> {
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        val pushbackReader = java.io.PushbackReader(reader, 1024) // Increased buffer
        
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
            
            return when (val char = firstChar.toChar()) {
                '{', '[' -> {
                    Log.d(TAG, "Detected JSON format")
                    jsonParser.parse(pushbackReader)
                }
                '#', '/' -> {
                    Log.d(TAG, "Detected M3U/Kodi format")
                    val content = BufferedReader(pushbackReader).readText()
                    kodiParser.parse(content)
                }
                '<' -> {
                    Log.d(TAG, "Detected XML format")
                    val content = BufferedReader(pushbackReader).readText()
                    xmlParser.parse(content)
                }
                else -> {
                    Log.d(TAG, "Unknown format ($char), trying fallback")
                    val content = BufferedReader(pushbackReader).readText()
                    when {
                        content.trimStart().startsWith("{") || content.trimStart().startsWith("[") -> jsonParser.parse(content)
                        content.trimStart().startsWith("<") -> xmlParser.parse(content)
                        content.contains("EXTINF", ignoreCase = true) || content.contains("EXTHTTP", ignoreCase = true) -> kodiParser.parse(content)
                        else -> simpleListParser.parse(content)
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
        return if (trimmed.startsWith("#") || trimmed.startsWith("/")) {
             kodiParser.parse(trimmed)
        } else if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
             jsonParser.parse(trimmed)
        } else if (trimmed.contains("://")) {
             simpleListParser.parse(trimmed)
        } else {
             emptyList()
        }
    }
}
