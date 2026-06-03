package com.codesrahul.streamplus.data.repository

import com.codesrahul.streamplus.data.local.HistoryDao
import com.codesrahul.streamplus.data.local.HistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.codesrahul.streamplus.data.local.ChannelDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val channelDao: ChannelDao
) {
    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun saveProgress(
        url: String, 
        name: String, 
        logo: String?, 
        group: String?, 
        playlistId: Int,
        userAgent: String? = null,
        headers: Map<String, String> = emptyMap(),
        cookie: String? = null,
        referer: String? = null,
        origin: String? = null,
        licenseUrl: String? = null,
        drmScheme: String? = null,
        drmHeaders: Map<String, String> = emptyMap(),
        position: Long, 
        duration: Long
    ) {
        val entity = HistoryEntity(
            url = url,
            name = name,
            logoUrl = logo,
            group = group ?: "Default",
            playlistId = playlistId,
            userAgent = userAgent,
            headers = headers,
            cookie = cookie,
            referer = referer,
            origin = origin,
            licenseUrl = licenseUrl,
            drmScheme = drmScheme,
            drmHeaders = drmHeaders,
            streamType = "", // Standard video playback might not always have info, but we can enhance this later
            lastPosition = position,
            duration = duration,
            timestamp = System.currentTimeMillis()
        )
        historyDao.insertHistory(entity)
    }

    suspend fun getLastPosition(name: String, group: String, playlistId: Int): Long {
        return historyDao.getPosition(name, group, playlistId) ?: 0L
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }

    suspend fun deleteHistory(item: HistoryEntity) {
        historyDao.deleteHistory(item)
    }
}
