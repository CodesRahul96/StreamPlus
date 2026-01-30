package com.codesrahul.streamplus.data.network

import com.codesrahul.streamplus.domain.model.StreamStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

class StreamChecker @Inject constructor(
    private val client: OkHttpClient
) {

    suspend fun checkStream(url: String, headers: Map<String, String> = emptyMap()): StreamStatus = withContext(Dispatchers.IO) {
        // Try HEAD first
        var requestBuilder = Request.Builder().url(url).head()
        for ((k, v) in headers) requestBuilder = requestBuilder.header(k, v)
        var request = requestBuilder.build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext StreamStatus.ACTIVE
                }
                // Fallback to GET for servers that don't support HEAD or require GET
            }

            // Try a lightweight GET to sniff content (download small range)
            requestBuilder = Request.Builder().url(url).get().header("Range", "bytes=0-4096")
            for ((k, v) in headers) requestBuilder = requestBuilder.header(k, v)
            request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val contentType = response.header("Content-Type") ?: ""
                    val bodySnippet = try {
                        response.body?.string()?.take(2048) ?: ""
                    } catch (e: Exception) { "" }

                    if (contentType.contains("mpegurl", ignoreCase = true) || contentType.contains("application/x-mpegurl", ignoreCase = true) ||
                        bodySnippet.contains("#EXTM3U") || bodySnippet.trimStart().startsWith("[") || bodySnippet.trimStart().startsWith("{")) {
                        // It's a playlist or valid response
                        return@withContext StreamStatus.ACTIVE
                    }
                    // If server returns a successful response, assume it's active
                    return@withContext StreamStatus.ACTIVE
                } else {
                    return@withContext if (response.code >= 400) StreamStatus.DEAD else StreamStatus.UNKNOWN
                }
            }
        } catch (e: IOException) {
            StreamStatus.TIMEOUT
        } catch (e: Exception) {
            StreamStatus.DEAD
        }
    }
}
