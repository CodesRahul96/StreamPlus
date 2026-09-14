package com.codesrahul.streamplus.data.parser

import com.codesrahul.streamplus.data.parser.StreamParser
import com.codesrahul.streamplus.data.parser.M3UStreamParser
import com.codesrahul.streamplus.data.parser.JsonStreamParser
import com.codesrahul.streamplus.data.parser.XmlPlsStreamParser

object StreamParserFactory {

    fun getParser(url: String, prefix: String): StreamParser {
        val m3uParser = M3UStreamParser()
        val jsonParser = JsonStreamParser()
        val xmlPlsParser = XmlPlsStreamParser()
        
        // If content contains M3U markers or ends with common m3u extensions, prioritize M3U
        val isM3u = url.endsWith(".m3u", true) || 
                    url.endsWith(".m3u8", true) || 
                    url.endsWith(".ts", true) || 
                    url.endsWith(".txt", true) ||
                    prefix.contains("#EXTM3U", ignoreCase = true) ||
                    prefix.contains("#EXTINF", ignoreCase = true)

        return when {
            isM3u -> m3uParser
            jsonParser.canParse(url, prefix) -> jsonParser
            xmlPlsParser.canParse(url, prefix) -> xmlPlsParser
            m3uParser.canParse(url, prefix) -> m3uParser
            else -> m3uParser // Default fallback
        }
    }
}
