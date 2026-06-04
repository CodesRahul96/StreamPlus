package com.codesrahul.streamplus.domain.repository

import com.codesrahul.streamplus.data.remote.model.*

sealed interface XtreamResult<out T> {
    data class Success<out T>(val data: T) : XtreamResult<T>
    data class Failure(val exception: Throwable) : XtreamResult<Nothing>

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
}

interface XtreamRepository {
    suspend fun authenticate(serverUrl: String, username: String, password: String): XtreamResult<XtreamAuthResponse>
    suspend fun getLiveCategories(serverUrl: String, username: String, password: String): XtreamResult<List<XtreamCategory>>
    suspend fun getLiveStreams(serverUrl: String, username: String, password: String, categoryId: String? = null): XtreamResult<List<XtreamLiveStream>>
    suspend fun getVodCategories(serverUrl: String, username: String, password: String): XtreamResult<List<XtreamCategory>>
    suspend fun getVodStreams(serverUrl: String, username: String, password: String, categoryId: String? = null): XtreamResult<List<XtreamVodStream>>
    suspend fun getSeriesCategories(serverUrl: String, username: String, password: String): XtreamResult<List<XtreamCategory>>
    suspend fun getSeries(serverUrl: String, username: String, password: String, categoryId: String? = null): XtreamResult<List<XtreamSeriesStream>>
    suspend fun getSeriesInfo(serverUrl: String, username: String, password: String, seriesId: Int): XtreamResult<XtreamSeriesInfo>
}
