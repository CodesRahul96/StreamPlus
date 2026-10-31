package com.codesrahul.streamplus

import com.codesrahul.streamplus.data.parser.M3UStreamParser
import com.codesrahul.streamplus.util.UrlParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class StreamPlusCoreTests {

    @Test
    fun testUrlParserPipedUrl() {
        val input = "http://example.com/live.m3u8|User-Agent=CustomUA&Referer=http://example.com"
        val (url, headers) = UrlParser.parsePipedUrl(input)
        assertEquals("http://example.com/live.m3u8", url)
        assertEquals("CustomUA", headers["User-Agent"])
        assertEquals("http://example.com", headers["Referer"])
    }

    @Test
    fun testM3UParserBasic() = runBlocking {
        val m3uContent = """
            #EXTM3U url-tvg="http://epg.example.com/epg.xml"
            #EXTINF:-1 tvg-id="ch1" tvg-name="Channel 1" tvg-logo="http://logo.png" group-title="News",News 24/7
            #EXTVLCOPT:http-user-agent=TestUA
            http://stream.example.com/ch1.m3u8
        """.trimIndent()

        val parser = M3UStreamParser()
        val items = parser.parse(ByteArrayInputStream(m3uContent.toByteArray()), "http://playlist.com/list.m3u")

        assertEquals(1, items.size)
        val channel = items[0]
        assertEquals("News 24/7", channel.name)
        assertEquals("News", channel.group)
        assertEquals("http://logo.png", channel.logoUrl)
        assertEquals("http://stream.example.com/ch1.m3u8", channel.url)
        assertEquals("TestUA", channel.headers["User-Agent"])
        assertFalse(channel.isVod)
    }

    @Test
    fun testM3UParserVodDetection() = runBlocking {
        val parser = M3UStreamParser()
        val liveContent = """
            #EXTM3U
            #EXTINF:-1 group-title="Movies",Live Movie TV
            http://stream.example.com/live/movie.m3u8
        """.trimIndent()

        val liveItems = parser.parse(ByteArrayInputStream(liveContent.toByteArray()), "http://test.com")
        assertEquals(1, liveItems.size)
        assertFalse("Channel with .m3u8 should not be marked as VOD", liveItems[0].isVod)

        val vodContent = """
            #EXTM3U
            #EXTINF:7200 group-title="Movies",The Inception (2010)
            http://stream.example.com/vod/movie.mp4
        """.trimIndent()

        val vodItems = parser.parse(ByteArrayInputStream(vodContent.toByteArray()), "http://test.com")
        assertEquals(1, vodItems.size)
        assertTrue("Duration >= 300 with mp4 in Movies should be marked as VOD", vodItems[0].isVod)
    }
}
