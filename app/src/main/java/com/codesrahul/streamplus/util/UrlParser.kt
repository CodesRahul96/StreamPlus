package com.codesrahul.streamplus.util

object UrlParser {
    private val PIPE_REGEX = "(?i)(\\?\\|)|(\\?%7C)|(\\|)|(%7C)".toRegex()

    fun parsePipedUrl(raw: String): Pair<String, Map<String, String>> {
        val match = PIPE_REGEX.find(raw) ?: return Pair(raw, emptyMap())
        val url = raw.substring(0, match.range.first).trim()
        val params = raw.substring(match.range.last + 1)
        val headers = mutableMapOf<String, String>()
        params.split("&").forEach { 
            val kv = it.split("=", limit = 2)
            if (kv.size == 2) {
                try {
                    headers[kv[0]] = java.net.URLDecoder.decode(kv[1], "UTF-8")
                } catch (e: Exception) {
                    headers[kv[0]] = kv[1]
                }
            }
        }
        return Pair(url, headers)
    }
}
