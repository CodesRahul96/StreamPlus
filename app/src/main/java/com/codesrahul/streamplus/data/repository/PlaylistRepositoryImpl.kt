package com.codesrahul.streamplus.data.repository

import android.content.Context
import android.net.Uri
import com.codesrahul.streamplus.data.network.StreamChecker
import com.codesrahul.streamplus.data.parser.M3UParser
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.domain.model.StreamStatus
import com.codesrahul.streamplus.domain.repository.PlaylistRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: M3UParser,
    private val streamChecker: StreamChecker,
    private val playlistDao: com.codesrahul.streamplus.data.local.PlaylistDao,
    private val client: okhttp3.OkHttpClient
) : PlaylistRepository {

    override suspend fun parsePlaylist(inputStream: InputStream): List<M3UItem> = withContext(Dispatchers.IO) {
        parser.parseUniversal(inputStream)
    }

    override suspend fun validatePlaylist(items: List<M3UItem>): List<M3UItem> = withContext(Dispatchers.IO) {
        // First expand items that are themselves playlists (m3u/json, etc.)
        val expanded = expandItems(items)

        // Concurrent validation using async/awaitAll
        // Chunking to avoid overwhelming the network/thread pool if list is huge
        expanded.chunked(50).flatMap { chunk ->
            chunk.map { item ->
                async {
                    val status = streamChecker.checkStream(item.url, item.headers)
                    item.copy(status = status)
                }
            }.awaitAll()
        }
    }

    private suspend fun expandItems(items: List<M3UItem>, depth: Int = 0): List<M3UItem> = withContext(Dispatchers.IO) {
        if (depth >= 2) return@withContext items
        val result = mutableListOf<M3UItem>()
        for (item in items) {
            try {
                val lower = item.url.lowercase()
                if (lower.contains(".m3u") || lower.contains(".m3u8") || lower.contains(".json") || lower.contains("/playlist") || lower.contains("/m3u")) {
                    // Attempt to fetch and parse as nested playlist
                    val requestBuilder = okhttp3.Request.Builder().url(item.url)
                    for ((k, v) in item.headers) requestBuilder.header(k, v)
                    val request = requestBuilder.get().build()
                    var handled = false
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStream = response.body?.byteStream()
                            if (bodyStream != null) {
                                val parsed = parser.parseUniversal(bodyStream)
                                if (parsed.isNotEmpty()) {
                                    // Give parsed items a parent group if missing
                                    val adjusted = parsed.map { p ->
                                        if (p.group.isBlank()) p.copy(group = item.name) else p
                                    }
                                    result.addAll(adjusted)
                                    handled = true
                                }
                            }
                        }
                    }
                    if (handled) continue
                } else {
                    // Try a light HEAD/GET to sniff content type for non-obvious playlist URLs
                    val headReqBuilder = okhttp3.Request.Builder().url(item.url).head()
                    for ((k, v) in item.headers) headReqBuilder.header(k, v)
                    val headReq = headReqBuilder.build()
                    var treatedAsPlaylist = false
                    try {
                        client.newCall(headReq).execute().use { headResp ->
                            if (headResp.isSuccessful) {
                                val ct = headResp.header("Content-Type") ?: ""
                                // Treat common playlist content-types and text as possible playlists
                                val likelyPlaylistContent = ct.contains("mpegurl", ignoreCase = true) ||
                                        ct.contains("application/json", ignoreCase = true) ||
                                        ct.startsWith("text", ignoreCase = true) ||
                                        ct.contains("vnd.apple.mpegurl", ignoreCase = true)

                                if (likelyPlaylistContent) {
                                    // Do a full GET and parse
                                    client.newCall(okhttp3.Request.Builder().url(item.url).get().build()).execute().use { resp ->
                                        if (resp.isSuccessful) {
                                            val parsed = parser.parseUniversal(resp.body!!.byteStream())
                                            if (parsed.isNotEmpty()) {
                                                val adjusted = parsed.map { p -> if (p.group.isBlank()) p.copy(group = item.name) else p }
                                                result.addAll(adjusted)
                                                treatedAsPlaylist = true
                                            }
                                        }
                                    }
                                } else {
                                    // As a last resort try fetching a small prefix (Range) and sniff for playlist markers
                                    try {
                                        client.newCall(okhttp3.Request.Builder().url(item.url).header("Range", "bytes=0-8191").get().build()).execute().use { prefixResp ->
                                            if (prefixResp.isSuccessful) {
                                                val snippet = prefixResp.body?.byteStream()?.buffered()?.use { bis ->
                                                    val buf = ByteArray(8192)
                                                    val read = bis.read(buf)
                                                    if (read > 0) String(buf, 0, read, Charsets.UTF_8) else ""
                                                } ?: ""

                                                val looksLike = snippet.contains("#EXTM3U", ignoreCase = true) || snippet.contains("#EXTINF", ignoreCase = true) ||
                                                        snippet.contains("EXT-X-STREAM-INF", ignoreCase = true) || snippet.trimStart().startsWith("{") || snippet.trimStart().startsWith("[")

                                                if (looksLike) {
                                                    client.newCall(okhttp3.Request.Builder().url(item.url).get().build()).execute().use { fullResp ->
                                                        if (fullResp.isSuccessful) {
                                                            val parsed = parser.parseUniversal(fullResp.body!!.byteStream())
                                                            if (parsed.isNotEmpty()) {
                                                                val adjusted = parsed.map { p -> if (p.group.isBlank()) p.copy(group = item.name) else p }
                                                                result.addAll(adjusted)
                                                                treatedAsPlaylist = true
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // ignore sniff failures
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) { /* ignore */ }

                    if (treatedAsPlaylist) continue
                }
            } catch (e: Exception) {
                // ignore and fallback to keeping the original item
            }
            result.add(item)
        }
        // Recurse once more to expand any nested playlists found
        if (result.any { it.url.lowercase().contains(".m3u") || it.url.lowercase().contains(".json") }) {
            return@withContext expandItems(result, depth + 1)
        }
        return@withContext result
    }

    override suspend fun savePlaylist(items: List<M3UItem>, filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            file.bufferedWriter().use { writer ->
                writer.write("#EXTM3U\n")
                items.forEach { item ->
                    // Reconstruct #EXTINF line
                    // #EXTINF:-1 tvg-logo="url" group-title="Group",Channel Name
                    var extInf = "#EXTINF:-1"
                    if (item.logoUrl.isNotEmpty()) extInf += " tvg-logo=\"${item.logoUrl}\""
                    if (item.group.isNotEmpty()) extInf += " group-title=\"${item.group}\""
                    extInf += ",${item.name}"
                    
                    writer.write(extInf + "\n")
                    writer.write(item.url + "\n")
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    override fun getAllPlaylists() = playlistDao.getAllPlaylists()

    override fun getPlaylist(id: Int) = playlistDao.getPlaylist(id)

    override suspend fun insertPlaylist(playlist: com.codesrahul.streamplus.data.local.PlaylistEntity) {
        playlistDao.insertPlaylist(playlist)
    }


    override suspend fun deletePlaylist(playlist: com.codesrahul.streamplus.data.local.PlaylistEntity) {
        playlistDao.deletePlaylist(playlist)
    }

    override suspend fun loadPlaylist(url: String, userAgent: String?): List<M3UItem> = withContext(Dispatchers.IO) {
        if (url.startsWith("content://") || url.startsWith("file://")) {
            try {
                context.contentResolver.openInputStream(Uri.parse(url))?.use { inputStream ->
                    parser.parseUniversal(inputStream)
                } ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } else {
            val requestBuilder = okhttp3.Request.Builder().url(url)
            if (!userAgent.isNullOrEmpty() && userAgent != "Default") {
                requestBuilder.header("User-Agent", userAgent)
            }
            val request = requestBuilder.build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw java.io.IOException("Unexpected code $response")
                    response.body?.byteStream()?.use { inputStream ->
                        parser.parseUniversal(inputStream)
                    } ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
