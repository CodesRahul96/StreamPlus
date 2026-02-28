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

    private val programs = mutableMapOf<String, MutableList<EPGProgram>>()
    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

    suspend fun loadEPG(url: String) = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            var inputStream: InputStream = connection.getInputStream()
            
            if (url.endsWith(".gz")) {
                inputStream = GZIPInputStream(inputStream)
            }
            
            parseXml(inputStream)
            Log.d("EPGManager", "Successfully loaded EPG from $url")
        } catch (e: Exception) {
            Log.e("EPGManager", "Failed to load EPG from $url", e)
        }
    }

    private fun parseXml(inputStream: InputStream) {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)
        
        var eventType = parser.eventType
        var currentChannelId: String? = null
        var currentTitle: String? = null
        var currentDesc: String? = null
        var startTime: Long = 0
        var stopTime: Long = 0

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            startTime = parseDate(parser.getAttributeValue(null, "start"))
                            stopTime = parseDate(parser.getAttributeValue(null, "stop"))
                        }
                        "title" -> currentTitle = parser.nextText()
                        "desc" -> currentDesc = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme" && currentChannelId != null && currentTitle != null) {
                        val prog = EPGProgram(currentTitle, startTime, stopTime, currentDesc ?: "")
                        programs.getOrPut(currentChannelId) { mutableListOf() }.add(prog)
                        
                        currentChannelId = null
                        currentTitle = null
                        currentDesc = null
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr == null) return 0
        return try {
            dateFormat.parse(dateStr)?.time ?: 0
        } catch (e: Exception) { 0 }
    }

    fun getCurrentProgram(epgId: String?): EPGProgram? {
        if (epgId == null) return null
        val now = System.currentTimeMillis()
        return programs[epgId]?.find { now in it.start until it.stop }
    }
}
