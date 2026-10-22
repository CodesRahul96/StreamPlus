package com.codesrahul.streamplus

import com.codesrahul.streamplus.data.epg.EPGManager
import com.codesrahul.streamplus.data.epg.EPGProgram
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class EPGUnitTest {

    @Test
    fun testEPGProgramCalculations() {
        val now = System.currentTimeMillis()
        val start = now - (15 * 60 * 1000L) // Started 15 mins ago
        val stop = now + (15 * 60 * 1000L)  // Ends in 15 mins

        val prog = EPGProgram(
            title = "Prime News Live",
            start = start,
            stop = stop,
            description = "Daily headlines",
            category = "News"
        )

        assertTrue(prog.isLive)
        assertEquals(30, prog.durationMinutes)
        assertTrue(prog.progress in 0.45f..0.55f)
    }

    @Test
    fun testEPGManagerChannelFiltering() {
        val epgManager = EPGManager()
        val base = System.currentTimeMillis()

        val p1 = EPGProgram("Morning Show", base - 3600000, base - 1800000)
        val p2 = EPGProgram("Current News", base - 1800000, base + 1800000)
        val p3 = EPGProgram("Evening Movie", base + 1800000, base + 7200000)

        epgManager.updateChannelPrograms("channel_1", listOf(p1, p2, p3))

        val current = epgManager.getCurrentProgram("channel_1")
        assertNotNull(current)
        assertEquals("Current News", current?.title)

        // Query window overlapping p2 and p3
        val windowPrograms = epgManager.getProgramsForChannel("channel_1", base, base + 3600000)
        assertEquals(2, windowPrograms.size)
        assertEquals("Current News", windowPrograms[0].title)
        assertEquals("Evening Movie", windowPrograms[1].title)
    }

    @Test
    fun testCatchupUrlGeneration() {
        val channelUrl = "http://iptv.example.com:8080/live/myuser/mypass/12345.ts"
        val startMillis = 1718000000000L
        val stopMillis = startMillis + (3600 * 1000L) // 60 minutes
        val durationMinutes = ((stopMillis - startMillis) / 60000).coerceAtLeast(1)

        val xtreamPattern = Regex("(https?://[^/]+)/live/([^/]+)/([^/]+)/([0-9]+)(\\.[a-zA-Z0-9]+)?")
        val match = xtreamPattern.find(channelUrl)
        assertNotNull(match)

        val (base, user, pass, streamId, ext) = match!!.destructured
        val sdf = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US)
        val startTimeFormatted = sdf.format(Date(startMillis))
        val extension = if (ext.isNotEmpty()) ext else ".ts"
        val timeshiftUrl = "$base/timeshift/$user/$pass/$durationMinutes/$startTimeFormatted/$streamId$extension"

        assertEquals("http://iptv.example.com:8080/timeshift/myuser/mypass/60/$startTimeFormatted/12345.ts", timeshiftUrl)
    }
}
