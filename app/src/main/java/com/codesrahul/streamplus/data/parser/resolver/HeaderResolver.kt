package com.codesrahul.streamplus.data.parser.resolver

import com.codesrahul.streamplus.domain.model.M3UItem

class HeaderResolver : StreamResolver {

    override fun resolve(item: M3UItem): M3UItem {
        val currentHeaders = item.headers
        val hasUa = currentHeaders.containsKey("User-Agent")
        val finalUa = when {
            item.userAgent.isNotBlank() -> item.userAgent
            hasUa -> currentHeaders["User-Agent"]!!
            else -> com.codesrahul.streamplus.util.UserAgentManager.DEFAULT_UA
        }

        if (item.userAgent == finalUa && hasUa && currentHeaders["User-Agent"] == finalUa) {
            return item
        }

        val headers = HashMap(currentHeaders)
        headers["User-Agent"] = finalUa

        return item.copy(
            headers = headers,
            userAgent = finalUa
        )
    }
}
