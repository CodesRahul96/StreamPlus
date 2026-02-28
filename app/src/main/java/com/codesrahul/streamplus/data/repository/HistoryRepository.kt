package com.codesrahul.streamplus.data.repository

import com.codesrahul.streamplus.data.local.HistoryDao
import com.codesrahul.streamplus.data.local.HistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun saveProgress(
        url: String, 
        name: String, 
        logo: String?, 
        group: String?, 
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
            group = group,
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

    suspend fun getLastPosition(url: String): Long {
        return historyDao.getPosition(url) ?: 0L
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }

    suspend fun deleteHistory(item: HistoryEntity) {
        historyDao.deleteHistory(item)
    }
}
