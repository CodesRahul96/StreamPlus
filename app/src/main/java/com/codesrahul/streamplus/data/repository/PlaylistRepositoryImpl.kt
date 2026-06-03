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
import com.codesrahul.streamplus.data.parser.M3UStreamParser
import com.codesrahul.streamplus.data.parser.StreamParserFactory
import com.codesrahul.streamplus.data.parser.resolver.CompositeStreamResolver
import com.codesrahul.streamplus.data.parser.resolver.HeaderResolver
import com.codesrahul.streamplus.data.epg.EPGManager
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
import kotlinx.coroutines.flow.map
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
    private val historyDao: com.codesrahul.streamplus.data.local.HistoryDao,
    private val client: okhttp3.OkHttpClient,
    private val settingsRepository: SettingsRepository,
    private val epgManager: EPGManager,
    private val xtreamRepository: com.codesrahul.streamplus.domain.repository.XtreamRepository
) : PlaylistRepository {

    private val playlistCache = java.util.concurrent.ConcurrentHashMap<String, List<M3UItem>>()

    private val resolver = CompositeStreamResolver(listOf(HeaderResolver(), DrmResolver()))

    /**
     * Parses the input stream and returns parsed channels + any detected EPG URL.
     * The EPG URL is extracted from the `#EXTM3U` header's `url-tvg` / `x-tvg-url` attributes.
     */
    private suspend fun resolveUniversal(inputStream: InputStream, url: String): Pair<List<M3UItem>, String?> = withContext(Dispatchers.IO) {
        // Read a small prefix for format detection
        val bufferedInput = inputStream.buffered()
        bufferedInput.mark(8192)
        val buffer = ByteArray(8192)
        val read = bufferedInput.read(buffer)
        val prefix = if (read > 0) String(buffer, 0, read, Charsets.UTF_8) else ""
        bufferedInput.reset()
        
        val parser = StreamParserFactory.getParser(url, prefix)
        val items = parser.parse(bufferedInput, url).map { resolver.resolve(it) }
        val detectedEpgUrl = (parser as? M3UStreamParser)?.lastDetectedEpgUrl
        Pair(items, detectedEpgUrl)
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

    override suspend fun loadPlaylist(url: String, userAgent: String?, forceRefresh: Boolean, playlistId: Int): List<M3UItem> = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext emptyList()
        
        val settings = settingsRepository.settings.first()
        val playlistEntity = if (playlistId != -1) {
            playlistDao.getPlaylist(playlistId).first()
        } else {
            playlistDao.getAllPlaylistsList().find { it.url == url }
        }
        val actualPlaylistId = playlistEntity?.id ?: -1

        // Check disk cache if enabled OR if network fails
        val cachedChannels = if (playlistId != -1) {
            channelDao.getChannelsByPlaylist(playlistId).map { it.toDomain() }
        } else emptyList()

        if (!forceRefresh) {
            // Check memory cache first
            if (settings.playlistCache) {
                playlistCache[url]?.let { return@withContext it }
            }

            if (settings.playlistCache && cachedChannels.isNotEmpty()) {
                // If Xtream, we MUST aggressively force refresh if the URLs don't contain the Uri.encoded username
                // otherwise the user will be stuck forever with mathematically corrupted 401 Stream DB records.
                // We'll just mandate a refresh if it's Xtream for now to guarantee the new logic applies.
                if (playlistEntity?.isXtream == true) {
                    android.util.Log.w("PlaylistRepo", "Bypassing stale Xtream cache to force new URI-Encoded URL logic.")
                } else {
                    playlistCache[url] = cachedChannels
                    return@withContext cachedChannels
                }
            }
        }

        val result = if (playlistEntity?.isXtream == true) {
            try {                // Strictly sanitize the Server URL. Millions of users paste strings ending with "/player_api.php"
                // or even full channel URLs. We MUST extract only the base server URL.
                val rawUrl = playlistEntity.serverUrl?.trim() ?: ""
                val serverBaseUrl = if (rawUrl.isEmpty()) "" else {
                    // 1. Strip query parameters
                    val noQuery = rawUrl.split("?")[0]
                    
                    // 2. Strip common Xtream filenames and path segments
                    var stripped = noQuery.removeSuffix("/")
                        .replace(Regex("/(player_api\\.php|get\\.php|panel_api\\.php|xmltv\\.php|get|playlist|live|movie|series)/?.*$", RegexOption.IGNORE_CASE), "")
                        .removeSuffix("/")
                    
                    // 3. New: Strip full short-links (/user/pass/id) which users often paste as the "server"
                    stripped = stripped.replace(Regex("/[^/]+/[^/]+/[0-9]+/?$", RegexOption.IGNORE_CASE), "")
                        .removeSuffix("/")
                    
                    // 4. Final validation: if it's just a protocol + host, keep it simple.
                    try {
                        val parsed = java.net.URL(stripped)
                        if (parsed.path.isNullOrEmpty() || parsed.path == "/") {
                            val portStr = if (parsed.port != -1) ":${parsed.port}" else ""
                            "${parsed.protocol}://${parsed.host}$portStr"
                        } else {
                            stripped
                        }
                    } catch (e: Exception) {
                        stripped
                    }
                }


                val username = android.net.Uri.encode(playlistEntity.username ?: "")
                val password = android.net.Uri.encode(playlistEntity.password ?: "")

                // Fetch data concurrently for better performance.
                // Note: xtreamRepository internally appends "/player_api.php" where needed,
                // so we MUST only pass the sanitized serverBaseUrl here.
                val authTask = async { xtreamRepository.authenticate(serverBaseUrl, username, password) }
                val liveCategoriesTask = async { xtreamRepository.getLiveCategories(serverBaseUrl, username, password) }
                val liveStreamsTask = async { xtreamRepository.getLiveStreams(serverBaseUrl, username, password) }
                val vodCategoriesTask = async { xtreamRepository.getVodCategories(serverBaseUrl, username, password) }
                val vodStreamsTask = async { xtreamRepository.getVodStreams(serverBaseUrl, username, password) }
                val seriesCategoriesTask = async { xtreamRepository.getSeriesCategories(serverBaseUrl, username, password) }
                val seriesTask = async { xtreamRepository.getSeries(serverBaseUrl, username, password) }

                val authResult = authTask.await()
                val liveCategoriesResult = liveCategoriesTask.await()
                val liveStreamsResult = liveStreamsTask.await()
                val vodCategoriesResult = vodCategoriesTask.await()
                val vodStreamsResult = vodStreamsTask.await()
                val seriesCategoriesResult = seriesCategoriesTask.await()
                val seriesResult = seriesTask.await()

                val items = mutableListOf<M3UItem>()
                val xtreamUa = "TiviMate/5.0.4 (Linux; Android 11)"

                // Create category maps with robust ID normalization helper
                fun normalizeId(id: String?): String = id?.trim()?.lowercase() ?: ""

                val liveCatMap = liveCategoriesResult.getOrNull()?.associate { normalizeId(it.categoryId) to it.categoryName } ?: emptyMap()
                val vodCatMap = vodCategoriesResult.getOrNull()?.associate { normalizeId(it.categoryId) to it.categoryName } ?: emptyMap()
                val seriesCatMap = seriesCategoriesResult.getOrNull()?.associate { normalizeId(it.categoryId) to it.categoryName } ?: emptyMap()

                android.util.Log.d("PlaylistRepo", "Xtream Categories: Live ${liveCatMap.size}, VOD ${vodCatMap.size}, Series ${seriesCatMap.size}")
                
                // Debug categories if empty or suspect
                if (liveCatMap.isEmpty() && liveCategoriesResult.getOrNull()?.isNotEmpty() == true) {
                    val sample = liveCategoriesResult.getOrNull()?.take(5)
                    android.util.Log.e("PlaylistRepo", "Category Map empty! Sample categories: $sample")
                }

                // Determine allowed formats (fallback to 'ts' if empty, if only 'm3u8' is allowed use it)
                val allowedFormats = authResult.getOrNull()?.userInfo?.allowedOutputFormats ?: emptyList()
                val liveFormat = if (allowedFormats.contains("m3u8") && !allowedFormats.contains("ts")) {
                    "m3u8"
                } else {
                    "ts" // Assume .ts mostly works for ExoPlayer unless server strictly denies it natively
                }
                
                // API calls and media streams both benefit from a TiviMate UA to avoid blocking or connection drops.
                val xtreamStreamUa = "TiviMate/5.0.4 (Linux; Android 11)"

                // Map Live Streams
                liveStreamsResult.getOrNull()?.forEach { stream ->
                    val catId = normalizeId(stream.categoryId)
                    val categoryName = if (catId.isNotEmpty()) liveCatMap[catId] ?: "Group $catId" else "Live"
                    
                    // Smart Path Detection: Check if the original input URL was a short-link.
                    // Pattern: host/user/pass/stream_id
                    val isShortPathServer = url.contains(Regex("/[^/]+/[^/]+/[0-9]+", RegexOption.IGNORE_CASE))
                    
                    val serverSuggestedUrl = authResult.getOrNull()?.serverInfo?.url?.trim()?.removeSuffix("/")
                    val finalStreamBaseUrl = if (!serverSuggestedUrl.isNullOrBlank() && serverSuggestedUrl.startsWith("http")) {
                        serverSuggestedUrl
                    } else {
                        serverBaseUrl
                    }

                    val safeUrl = if (!stream.directSource.isNullOrBlank()) {
                        stream.directSource
                    } else {
                        if (isShortPathServer) {
                            // XUI / Short-Path format: host/user/pass/id (usually no extension)
                            "$finalStreamBaseUrl/$username/$password/${stream.streamId}"
                        } else {
                            // Standard Xtream format: host/live/user/pass/id.ts
                            val ext = if (liveFormat.isNotBlank()) ".$liveFormat" else ".ts"
                            "$finalStreamBaseUrl/live/$username/$password/${stream.streamId}$ext"
                        }
                    }

                    if (items.size < 5) {
                        android.util.Log.d("PlaylistRepo", "Smart Path Detection: isShortPath=$isShortPathServer | URL: $safeUrl")
                    }

                    items.add(
                        M3UItem(
                            name = stream.name ?: "Unknown channel",
                            url = safeUrl,
                            logoUrl = stream.streamIcon ?: "",
                            group = categoryName,
                            playlistId = actualPlaylistId,
                            userAgent = xtreamStreamUa,
                            streamType = "live"
                        )
                    )
                }

                // Map VOD Streams
                vodStreamsResult.getOrNull()?.forEach { stream ->
                    val catId = normalizeId(stream.categoryId)
                    val categoryName = if (catId.isNotEmpty()) vodCatMap[catId] ?: "Group $catId" else "VOD"
                    
                    // Use server suggested URL if available
                    val serverSuggestedUrl = authResult.getOrNull()?.serverInfo?.url?.trim()?.removeSuffix("/")
                    val finalStreamBaseUrl = if (!serverSuggestedUrl.isNullOrBlank() && serverSuggestedUrl.startsWith("http")) {
                        serverSuggestedUrl
                    } else {
                        serverBaseUrl
                    }

                    // Determine if the original input URL was a short-link for adaptive path construction
                    val isShortPathServer = url.contains(Regex("/[^/]+/[^/]+/[0-9]+", RegexOption.IGNORE_CASE))

                    val safeUrl = if (!stream.directSource.isNullOrBlank()) {
                        stream.directSource
                    } else {
                        val ext = stream.containerExtension ?: "mp4"
                        if (isShortPathServer) {
                            // XUI / Short-Path format: host/user/pass/id (no extension)
                            "$finalStreamBaseUrl/$username/$password/${stream.streamId}"
                        } else {
                            // Standard Xtream format: host/movie/user/pass/id.ext
                            "$finalStreamBaseUrl/movie/$username/$password/${stream.streamId}.$ext"
                        }
                    }

                    items.add(
                        M3UItem(
                            name = stream.name ?: "Unknown VOD",
                            url = safeUrl,
                            logoUrl = stream.streamIcon ?: "",
                            group = categoryName,
                            playlistId = actualPlaylistId,
                            userAgent = xtreamStreamUa,
                            isVod = true,
                            streamType = "vod"
                        )
                    )
                }

                // Map Series
                seriesResult.getOrNull()?.forEach { show ->
                    val catId = normalizeId(show.categoryId)
                    val categoryName = if (catId.isNotEmpty()) seriesCatMap[catId] ?: "Group $catId" else "Series"
                    val safeUrl = "${show.seriesId}" // We only need the ID for navigation
                    items.add(
                        M3UItem(
                            name = show.name ?: "Unknown Series",
                            url = safeUrl,
                            logoUrl = show.cover ?: "",
                            group = categoryName,
                            playlistId = actualPlaylistId,
                            userAgent = xtreamStreamUa,
                            isVod = true,
                            streamType = "series"
                        )
                    )
                }
                
                // Auto-construct Xtream EPG URL if not provided. 
                // Note: Xtream EPG is usually at /xmltv.php but we should use encoded credentials.
                val xtreamEpgUrl = "$serverBaseUrl/xmltv.php?username=$username&password=$password"
                
                Pair(items, (playlistEntity.epgUrl ?: xtreamEpgUrl))
            } catch (e: Exception) {
                android.util.Log.e("PlaylistRepo", "Error loading Xtream playlist: $url", e)
                // If it's a 401, we MUST NOT return the stale cache because the cache itself 
                // might be contaminated with bad credentials or old formats causing the player to 401 later.
                if (forceRefresh || e.message?.contains("401") == true) throw e
                Pair(cachedChannels, null)
            }
        } else if (url.startsWith("content://") || url.startsWith("file://")) {
            try {
                context.contentResolver.openInputStream(Uri.parse(url))?.use { inputStream ->
                    resolveUniversal(inputStream, url)
                } ?: Pair(emptyList<M3UItem>(), null)
            } catch (e: Exception) {
                android.util.Log.e("PlaylistRepo", "Error loading local playlist: $url", e)
                Pair(cachedChannels, null) // Fallback to disk cache on error
            }
        } else {
            try {
                val requestBuilder = okhttp3.Request.Builder().url(url)
                val finalUA = when {
                    !userAgent.isNullOrEmpty() && userAgent != "Auto" && userAgent != "Default" -> userAgent
                    userAgent == "Default" -> com.codesrahul.streamplus.util.UserAgentManager.DEFAULT_UA
                    // Auto mode: Detect if it's an Xtream link (short-link or API) and use TiviMate.
                    // Updated regex to support optional extensions like .ts, .m3u8, etc.
                    url.contains(Regex("/[^/]+/[^/]+/[0-9]+(\\.[^/]+)?$", RegexOption.IGNORE_CASE)) || 
                    url.contains("player_api.php") || url.contains("get.php") -> {
                        android.util.Log.d("PlaylistRepo", "Auto-detected Xtream/IPTV link, using TiviMate UA")
                        "TiviMate/5.0.4 (Linux; Android 11)"
                    }
                    else -> com.codesrahul.streamplus.util.UserAgentManager.DEFAULT_UA
                }
                
                requestBuilder.header("User-Agent", finalUA)
                
                val request = requestBuilder.build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Pair(cachedChannels, null)
                    } else {
                        val body = response.body
                        if (body == null) Pair(cachedChannels, null)
                        else resolveUniversal(body.byteStream(), url)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaylistRepo", "Error loading remote playlist: $url", e)
                if (forceRefresh) throw e
                Pair(cachedChannels, null) // Fallback to disk cache on error
            }
        }
        
        val items = result.first
        val detectedEpgUrl = result.second

        var finalItems = items
        if (items.isNotEmpty()) {
            // Update disk cache
            if (actualPlaylistId != -1) {
                channelDao.deleteChannelsByPlaylist(actualPlaylistId)
                channelDao.insertChannels(items.map { it.toEntity(actualPlaylistId) })
                val reloaded = channelDao.getChannelsByPlaylist(actualPlaylistId).map { it.toDomain() }
                if (reloaded.isNotEmpty()) {
                    finalItems = reloaded
                }
            }
            if (settings.playlistCache) {
                playlistCache[url] = finalItems
            }
        }

        // Auto-save and auto-load detected EPG URL
        if (!detectedEpgUrl.isNullOrBlank() && actualPlaylistId != -1) {
            val existingEpgUrl = playlistEntity?.epgUrl
            if (existingEpgUrl.isNullOrBlank() || existingEpgUrl != detectedEpgUrl) {
                android.util.Log.d("PlaylistRepo", "Auto-saving detected EPG URL: $detectedEpgUrl")
                playlistDao.updateEpgUrl(actualPlaylistId, detectedEpgUrl)
            }
            // Always load EPG on playlist refresh to keep guide data fresh
            epgManager.loadEPG(detectedEpgUrl)
        } else {
            // Load previously saved EPG URL if a new one wasn't found in headers
            playlistEntity?.epgUrl?.let { savedUrl ->
                epgManager.loadEPG(savedUrl)
            }
        }

        finalItems
    }

    override suspend fun parsePlaylist(inputStream: InputStream): List<M3UItem> = withContext(Dispatchers.IO) {
        resolveUniversal(inputStream, "direct_upload.m3u").first
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
                    if ((lower.contains(".m3u") && !lower.contains(".m3u8")) || lower.contains(".json") || lower.contains("/playlist") || lower.contains("/m3u")) {
                        val requestBuilder = okhttp3.Request.Builder().url(item.url)
                        for ((k, v) in item.headers) requestBuilder.header(k, v)
                        val request = requestBuilder.get().build()
                        
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val bodyStream = response.body?.byteStream()
                                if (bodyStream != null) {
                                    val parsed = resolveUniversal(bodyStream, item.url).first
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
        if (expandedResults.any { (it.url.lowercase().contains(".m3u") && !it.url.lowercase().contains(".m3u8")) || it.url.lowercase().contains(".json") }) {
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
        withContext(Dispatchers.IO) {
            favoriteDao.deleteByPlaylistId(playlist.id)
            historyDao.deleteByPlaylistId(playlist.id)
            channelDao.deleteChannelsByPlaylist(playlist.id)
            playlistDao.deletePlaylist(playlist)
        }
    }

    // --- Favorites ---
    override fun getAllFavorites(): kotlinx.coroutines.flow.Flow<List<FavoriteEntity>> {
        return favoriteDao.getAllFavorites()
    }

    override suspend fun addFavorite(item: FavoriteEntity) = favoriteDao.addFavorite(item)

    override suspend fun removeFavorite(name: String, group: String, playlistId: Int) = 
        favoriteDao.removeFavorite(name, group, playlistId)

    override suspend fun isFavorite(name: String, group: String, playlistId: Int) = 
        favoriteDao.isFavorite(name, group, playlistId)

    // --- Global Search ---
    override suspend fun searchAllPlaylists(query: String): List<M3UItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val settings = settingsRepository.settings.first()

        // If caching is on, searching the DB is MUCH faster and covers all playlists at once
        if (settings.playlistCache) {
            return@withContext channelDao.searchChannels(query).map { it.toDomain() }
        }

        // Fallback: search by re-downloading playlists (cache OFF path).
        // We process playlists sequentially (not in parallel) and cap results per playlist
        // to prevent OutOfMemoryError on large playlists.
        val lowerQuery = query.lowercase()
        val playlists = playlistDao.getAllPlaylistsList()
        val results = mutableListOf<M3UItem>()
        for (playlist in playlists) {
            if (results.size >= 200) break // global cap – enough results to show
            try {
                val items = loadPlaylist(playlist.url, playlist.userAgent)
                val matched = items.filter { item ->
                    item.name.lowercase().contains(lowerQuery) ||
                    item.group.lowercase().contains(lowerQuery)
                }.take(50).map { it.copy(playlistId = playlist.id) } // Assign playlistId
                results.addAll(matched)
            } catch (_: OutOfMemoryError) {
                System.gc()
                android.util.Log.w("PlaylistRepo", "OOM during global search for playlist ${playlist.name}, skipping")
            } catch (_: Exception) {
                // silently skip failed playlists
            }
        }
        results
    }

    override suspend fun getSeriesInfo(playlistId: Int, seriesId: Int): com.codesrahul.streamplus.data.remote.model.XtreamSeriesInfo? = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistSync(playlistId) ?: return@withContext null
        if (!playlist.isXtream) return@withContext null
        
        val rawUrl = playlist.serverUrl?.trim() ?: ""
        val serverBaseUrl = if (rawUrl.isEmpty()) "" else {
            val noQuery = rawUrl.split("?")[0]
            noQuery.removeSuffix("/")
                .replace(Regex("/(player_api\\.php|get\\.php|panel_api\\.php|xmltv\\.php|get|playlist|live|movie|series)/?.*$", RegexOption.IGNORE_CASE), "")
                .removeSuffix("/")
        }
        
        val username = android.net.Uri.encode(playlist.username ?: "")
        val password = android.net.Uri.encode(playlist.password ?: "")
        
        xtreamRepository.getSeriesInfo(serverBaseUrl, username, password, seriesId).getOrNull()
    }
}
