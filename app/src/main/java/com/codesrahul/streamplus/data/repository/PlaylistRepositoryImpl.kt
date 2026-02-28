package com.codesrahul.streamplus.data.repository

import android.content.Context
import android.net.Uri
import com.codesrahul.streamplus.data.local.ChannelDao
import com.codesrahul.streamplus.data.local.ChannelEntity
import com.codesrahul.streamplus.data.local.FavoriteDao
import com.codesrahul.streamplus.data.local.FavoriteEntity
import com.codesrahul.streamplus.data.local.toDomain
import com.codesrahul.streamplus.data.local.toEntity
import com.codesrahul.streamplus.data.network.StreamChecker
import com.codesrahul.streamplus.data.parser.StreamParserFactory
import com.codesrahul.streamplus.data.parser.resolver.CompositeStreamResolver
import com.codesrahul.streamplus.data.parser.resolver.HeaderResolver
import com.codesrahul.streamplus.data.parser.resolver.DrmResolver
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.domain.model.StreamStatus
import com.codesrahul.streamplus.domain.repository.PlaylistRepository
import com.codesrahul.streamplus.data.settings.SettingsRepository
import com.codesrahul.streamplus.data.settings.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamChecker: StreamChecker,
    private val playlistDao: com.codesrahul.streamplus.data.local.PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val channelDao: ChannelDao,
    private val client: okhttp3.OkHttpClient,
    private val settingsRepository: SettingsRepository
) : PlaylistRepository {

    private val playlistCache = java.util.concurrent.ConcurrentHashMap<String, List<M3UItem>>()

    private val resolver = CompositeStreamResolver(listOf(HeaderResolver(), DrmResolver()))

    private suspend fun resolveUniversal(inputStream: InputStream, url: String): List<M3UItem> = withContext(Dispatchers.IO) {
        // Read a small prefix for format detection
        val bufferedInput = inputStream.buffered() // Buffered for lookahead
        bufferedInput.mark(8192)
        val buffer = ByteArray(8192)
        val read = bufferedInput.read(buffer)
        val prefix = if (read > 0) String(buffer, 0, read, Charsets.UTF_8) else ""
        bufferedInput.reset() // Reset to start for the actual parser
        
        val parser = StreamParserFactory.getParser(url, prefix)
        parser.parse(bufferedInput, url).map { resolver.resolve(it) }
    }

    override suspend fun savePlaylist(items: List<M3UItem>, outputStream: java.io.OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            outputStream.bufferedWriter().use { writer ->
                writer.write("#EXTM3U\n")
                items.forEach { item ->
                    val logoAttr = if (item.logoUrl.isNotBlank()) " tvg-logo=\"${item.logoUrl}\"" else ""
                    val groupAttr = if (item.group.isNotBlank()) " group-title=\"${item.group}\"" else ""
                    val uaAttr = if (item.userAgent.isNotBlank()) " user-agent=\"${item.userAgent}\"" else ""
                    
                    writer.write("#EXTINF:-1${logoAttr}${groupAttr}${uaAttr},${item.name}\n")
                    
                    // Export each header as EXTVLCOPT (skip User-Agent – already in EXTINF attr)
                    item.headers.forEach { (k, v) ->
                        if (k.equals("User-Agent", ignoreCase = true)) return@forEach
                        val vlcKey = when {
                            k.equals("Referer", ignoreCase = true) -> "http-referrer"
                            k.equals("Origin", ignoreCase = true)  -> "http-origin"
                            k.equals("Cookie", ignoreCase = true)  -> "cookie"
                            else -> "http-${k.lowercase()}"
                        }
                        writer.write("#EXTVLCOPT:$vlcKey=$v\n")
                    }
                    
                    writer.write("${item.url}\n")
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepo", "Failed to save playlist", e)
            false
        }
    }

    override suspend fun loadPlaylist(url: String, userAgent: String?): List<M3UItem> = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext emptyList()
        
        val settings = settingsRepository.settings.first()
        val playlistEntity = playlistDao.getAllPlaylistsList().find { it.url == url }
        val playlistId = playlistEntity?.id ?: -1

        // Check memory cache first
        if (settings.playlistCache) {
            playlistCache[url]?.let { return@withContext it }
        }

        // Check disk cache if enabled OR if network fails
        val cachedChannels = if (playlistId != -1) {
            channelDao.getChannelsByPlaylist(playlistId).map { it.toDomain() }
        } else emptyList()

        if (settings.playlistCache && cachedChannels.isNotEmpty()) {
            playlistCache[url] = cachedChannels
            return@withContext cachedChannels
        }

        val items = if (url.startsWith("content://") || url.startsWith("file://")) {
            try {
                context.contentResolver.openInputStream(Uri.parse(url))?.use { inputStream ->
                    resolveUniversal(inputStream, url)
                } ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("PlaylistRepo", "Error loading local playlist: $url", e)
                cachedChannels // Fallback to disk cache on error
            }
        } else {
            try {
                val requestBuilder = okhttp3.Request.Builder().url(url)
                if (!userAgent.isNullOrEmpty() && userAgent != "Auto" && userAgent != "Default") {
                    requestBuilder.header("User-Agent", userAgent)
                } else if (userAgent == "Default") {
                    requestBuilder.header("User-Agent", com.codesrahul.streamplus.util.UserAgentManager.DEFAULT_UA)
                }
                
                val request = requestBuilder.build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext cachedChannels
                    val body = response.body ?: return@withContext cachedChannels
                    resolveUniversal(body.byteStream(), url)
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaylistRepo", "Error loading remote playlist: $url", e)
                cachedChannels // Fallback to disk cache on error
            }
        }
        
        if (items.isNotEmpty()) {
            if (settings.playlistCache) {
                playlistCache[url] = items
            }
            // Update disk cache
            if (playlistId != -1) {
                channelDao.deleteChannelsByPlaylist(playlistId)
                channelDao.insertChannels(items.map { it.toEntity(playlistId) })
            }
        }
        items
    }

    override suspend fun parsePlaylist(inputStream: InputStream): List<M3UItem> = withContext(Dispatchers.IO) {
        resolveUniversal(inputStream, "direct_upload.m3u")
    }

    override suspend fun validatePlaylist(items: List<M3UItem>): List<M3UItem> = withContext(Dispatchers.IO) {
        // First expand items that are themselves playlists (m3u/json, etc.)
        val expanded = expandItems(items)

        // Concurrent validation using async/awaitAll
        // Chunking to avoid overwhelming the network/thread pool if list is huge
        expanded.chunked(50).flatMap { chunk ->
            chunk.map { item ->
                async {
                    try {
                        val status = streamChecker.checkStream(item.url, item.headers)
                        item.copy(status = status)
                    } catch (e: Exception) {
                        item.copy(status = com.codesrahul.streamplus.domain.model.StreamStatus.DEAD)
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun expandItems(items: List<M3UItem>, depth: Int = 0): List<M3UItem> = withContext(Dispatchers.IO) {
        if (depth >= 2) return@withContext items
        
        val expandedResults = items.map { item ->
            async {
                try {
                    val lower = item.url.lowercase()
                    if (lower.contains(".m3u") || lower.contains(".m3u8") || lower.contains(".json") || lower.contains("/playlist") || lower.contains("/m3u")) {
                        val requestBuilder = okhttp3.Request.Builder().url(item.url)
                        for ((k, v) in item.headers) requestBuilder.header(k, v)
                        val request = requestBuilder.get().build()
                        
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val bodyStream = response.body?.byteStream()
                                if (bodyStream != null) {
                                    val parsed = resolveUniversal(bodyStream, item.url)
                                    if (parsed.isNotEmpty()) {
                                        return@async parsed.map { p ->
                                            if (p.group.isBlank()) p.copy(group = item.name) else p
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PlaylistRepo", "Error expanding item: ${item.url}", e)
                }
                listOf(item)
            }
        }.awaitAll().flatten()

        // Recurse once more to expand any nested playlists found
        if (expandedResults.any { it.url.lowercase().contains(".m3u") || it.url.lowercase().contains(".json") }) {
            return@withContext expandItems(expandedResults, depth + 1)
        }
        return@withContext expandedResults
    }

    override fun getAllPlaylists() = playlistDao.getAllPlaylists()

    override fun getPlaylist(id: Int) = playlistDao.getPlaylist(id)

    override suspend fun insertPlaylist(playlist: com.codesrahul.streamplus.data.local.PlaylistEntity) {
        playlistDao.insertPlaylist(playlist)
    }

    override suspend fun deletePlaylist(playlist: com.codesrahul.streamplus.data.local.PlaylistEntity) {
        playlistDao.deletePlaylist(playlist)
    }

    // --- Favorites ---
    override fun getAllFavorites() = favoriteDao.getAllFavorites()

    override suspend fun addFavorite(item: FavoriteEntity) = favoriteDao.addFavorite(item)

    override suspend fun removeFavorite(url: String) = favoriteDao.removeFavorite(url)

    override suspend fun isFavorite(url: String) = favoriteDao.isFavorite(url)

    // --- Global Search ---
    override suspend fun searchAllPlaylists(query: String): List<M3UItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val settings = settingsRepository.settings.first()
        
        // If caching is on, searching the DB is MUCH faster and covers all playlists at once
        if (settings.playlistCache) {
            return@withContext channelDao.searchChannels(query).map { it.toDomain() }
        }

        // Fallback to legacy parallel search if cache is off
        val lowerQuery = query.lowercase()
        val playlists = playlistDao.getAllPlaylistsList()
        val jobs = playlists.map { playlist ->
            async {
                try {
                    val items = loadPlaylist(playlist.url, playlist.userAgent)
                    items.filter { item ->
                        item.name.lowercase().contains(lowerQuery) ||
                        item.group.lowercase().contains(lowerQuery)
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
        jobs.awaitAll().flatten()
    }
}
