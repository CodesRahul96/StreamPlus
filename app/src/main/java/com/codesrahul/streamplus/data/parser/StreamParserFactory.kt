package com.codesrahul.streamplus.data.parser

import com.codesrahul.streamplus.data.parser.StreamParser
import com.codesrahul.streamplus.data.parser.M3UStreamParser
import com.codesrahul.streamplus.data.parser.JsonStreamParser

object StreamParserFactory {

    fun getParser(url: String, prefix: String): StreamParser {
        val m3uParser = M3UStreamParser()
        val jsonParser = JsonStreamParser()
        
        return when {
            jsonParser.canParse(url, prefix) -> jsonParser
            
            // Explicit extension check for common playlist types to force M3U parser
            url.endsWith(".m3u", true) || 
            url.endsWith(".m3u8", true) || 
            url.endsWith(".ts", true) || 
            url.endsWith(".txt", true) -> m3uParser

            m3uParser.canParse(url, prefix) -> m3uParser
            else -> m3uParser // Default fallback
        }
    }
}
