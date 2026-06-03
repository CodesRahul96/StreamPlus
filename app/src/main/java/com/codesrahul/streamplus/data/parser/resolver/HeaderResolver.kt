package com.codesrahul.streamplus.data.parser.resolver

import com.codesrahul.streamplus.domain.model.M3UItem

class HeaderResolver : StreamResolver {

    override fun resolve(item: M3UItem): M3UItem {
        val currentHeaders = item.headers
        val headers = HashMap(currentHeaders)

        // Determine the final User-Agent:
        //  1. If item already has a non-blank userAgent (e.g., from the player intent), use it.
        //  2. Else fall back to whatever is in the headers map.
        //  3. Else use the app default UA.
        val finalUa = when {
            item.userAgent.isNotBlank() -> item.userAgent
            headers.containsKey("User-Agent") -> headers["User-Agent"]!!
            else -> com.codesrahul.streamplus.util.UserAgentManager.DEFAULT_UA
        }

        // Keep the headers map in sync so OkHttp also sees the correct UA
        headers["User-Agent"] = finalUa

        return item.copy(
            headers = headers,
            userAgent = finalUa
        )
    }
}
