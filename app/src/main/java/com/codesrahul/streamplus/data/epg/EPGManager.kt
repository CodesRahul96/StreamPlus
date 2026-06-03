package com.codesrahul.streamplus.data.epg

import android.content.Context
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.zip.GZIPInputStream

data class EPGProgram(
    val title: String,
    val start: Long,
    val stop: Long,
    val description: String = ""
)

@Singleton
class EPGManager @Inject constructor() {

    @Volatile
    private var programs = mapOf<String, List<EPGProgram>>()
    
    // ThreadLocal prevents concurrency bugs with SimpleDateFormat
    private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
        }
    }

    suspend fun loadEPG(url: String) = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            
            // Auto-close streams
            connection.getInputStream().use { rawInput ->
                val inputStream = if (url.endsWith(".gz")) {
                    GZIPInputStream(rawInput)
                } else {
                    rawInput
                }
                
                // Parse to local map first to avoid exposing partial state
                val parsedPrograms = parseXml(inputStream)
                
                // Atomically update the global state
                programs = parsedPrograms
            }
            Log.d("EPGManager", "Successfully loaded EPG from $url")
        } catch (e: Exception) {
            Log.e("EPGManager", "Failed to load EPG from $url", e)
        }
    }

    private fun parseXml(inputStream: InputStream): Map<String, List<EPGProgram>> {
        val localPrograms = mutableMapOf<String, MutableList<EPGProgram>>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)
        
        var eventType = parser.eventType
        var currentChannelId: String? = null
        var currentTitle: String? = null
        var currentDesc: String? = null
        var startTime: Long = 0
        var stopTime: Long = 0

        val now = System.currentTimeMillis()
        val limit = now + (24 * 60 * 60 * 1000) // Keep next 24 hours

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            startTime = parseDate(parser.getAttributeValue(null, "start"))
                            stopTime = parseDate(parser.getAttributeValue(null, "stop"))
                        }
                        "title" -> currentTitle = try { parser.nextText() } catch (e: Exception) { null }
                        "desc" -> currentDesc = try { parser.nextText() } catch (e: Exception) { null }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme" && currentChannelId != null && currentTitle != null) {
                        // Only store programs that are currently active or upcoming in the next 24h
                        if (stopTime > now && startTime < limit) {
                            val prog = EPGProgram(currentTitle, startTime, stopTime, currentDesc ?: "")
                            localPrograms.getOrPut(currentChannelId) { mutableListOf() }.add(prog)
                        }
                        
                        currentChannelId = null
                        currentTitle = null
                        currentDesc = null
                    }
                }
            }
            eventType = try { parser.next() } catch (e: Exception) { XmlPullParser.END_DOCUMENT }
        }
        return localPrograms
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr == null) return 0
        return try {
            dateFormat.get()?.parse(dateStr)?.time ?: 0
        } catch (e: Exception) { 0 }
    }

    fun getCurrentProgram(epgId: String?): EPGProgram? {
        if (epgId == null) return null
        val now = System.currentTimeMillis()
        return programs[epgId]?.find { now in it.start until it.stop }
    }
}
