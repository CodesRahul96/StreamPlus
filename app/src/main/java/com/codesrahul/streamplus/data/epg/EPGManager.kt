package com.codesrahul.streamplus.data.epg

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class EPGProgram(
    val title: String,
    val start: Long,
    val stop: Long,
    val description: String = "",
    val category: String = "",
    val iconUrl: String = ""
) {
    val isLive: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now in start until stop
        }

    val progress: Float
        get() {
            if (!isLive) return 0f
            val total = (stop - start).toFloat()
            if (total <= 0f) return 0f
            val elapsed = (System.currentTimeMillis() - start).toFloat()
            return (elapsed / total).coerceIn(0f, 1f)
        }

    val durationMinutes: Int
        get() = ((stop - start) / 60000L).toInt().coerceAtLeast(1)
}

@Singleton
class EPGManager @Inject constructor() {

    private val _epgState = MutableStateFlow<Map<String, List<EPGProgram>>>(emptyMap())
    val epgState: StateFlow<Map<String, List<EPGProgram>>> = _epgState.asStateFlow()

    @Volatile
    private var programs = mapOf<String, List<EPGProgram>>()

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

            connection.getInputStream().use { rawInput ->
                val inputStream = if (url.endsWith(".gz")) {
                    GZIPInputStream(rawInput)
                } else {
                    rawInput
                }

                val parsedPrograms = parseXml(inputStream)
                programs = parsedPrograms
                _epgState.value = parsedPrograms
            }
            Log.d("EPGManager", "Successfully loaded " + programs.size + " channels into EPG from " + url)
        } catch (e: Exception) {
            Log.e("EPGManager", "Failed to load EPG from " + url, e)
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
        var currentCategory: String? = null
        var currentIcon: String? = null
        var startTime: Long = 0
        var stopTime: Long = 0

        val now = System.currentTimeMillis()
        val pastLimit = now - (48 * 60 * 60 * 1000L)
        val futureLimit = now + (48 * 60 * 60 * 1000L)

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
                        "category" -> currentCategory = try { parser.nextText() } catch (e: Exception) { null }
                        "icon" -> currentIcon = parser.getAttributeValue(null, "src")
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme" && currentChannelId != null && currentTitle != null) {
                        if (stopTime > pastLimit && startTime < futureLimit) {
                            val prog = EPGProgram(
                                title = currentTitle,
                                start = startTime,
                                stop = stopTime,
                                description = currentDesc ?: "",
                                category = currentCategory ?: "",
                                iconUrl = currentIcon ?: ""
                            )
                            localPrograms.getOrPut(currentChannelId) { mutableListOf() }.add(prog)
                        }

                        currentChannelId = null
                        currentTitle = null
                        currentDesc = null
                        currentCategory = null
                        currentIcon = null
                    }
                }
            }
            eventType = try { parser.next() } catch (e: Exception) { XmlPullParser.END_DOCUMENT }
        }

        return localPrograms.mapValues { entry ->
            entry.value.sortedBy { it.start }
        }
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr == null) return 0
        return try {
            dateFormat.get()?.parse(dateStr)?.time ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getCurrentProgram(epgId: String?): EPGProgram? {
        if (epgId == null) return null
        val now = System.currentTimeMillis()
        return programs[epgId]?.find { now in it.start until it.stop }
    }

    fun getProgramsForChannel(epgId: String?, fromTime: Long, toTime: Long): List<EPGProgram> {
        if (epgId == null) return emptyList()
        val list = programs[epgId] ?: return emptyList()
        return list.filter { it.stop > fromTime && it.start < toTime }
    }

    fun updateChannelPrograms(channelId: String, newPrograms: List<EPGProgram>) {
        val updated = programs.toMutableMap()
        updated[channelId] = newPrograms.sortedBy { it.start }
        programs = updated
        _epgState.value = updated
    }
}
