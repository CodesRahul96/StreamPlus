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
    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory().map { history ->
        history.map { entry ->
            // Try to find a fresher URL in the channels table
            val refreshed = channelDao.getChannelByIdentity(entry.name, entry.group, entry.playlistId)
            if (refreshed != null && refreshed.url != entry.url) {
                entry.copy(url = refreshed.url)
            } else {
                entry
            }
        }
    }

    suspend fun saveProgress(
        url: String, 
        name: String, 
        logo: String?, 
        group: String?, 
        playlistId: Int,
        userAgent: String? = null,
        cookie: String? = null,
        referer: String? = null,
        origin: String? = null,
        licenseUrl: String? = null,
        drmScheme: String? = null,
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
            cookie = cookie,
            referer = referer,
            origin = origin,
            licenseUrl = licenseUrl,
            drmScheme = drmScheme,
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
