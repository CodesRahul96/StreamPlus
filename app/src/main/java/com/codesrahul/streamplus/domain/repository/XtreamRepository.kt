package com.codesrahul.streamplus.domain.repository

import com.codesrahul.streamplus.data.remote.model.*

interface XtreamRepository {
    suspend fun authenticate(serverUrl: String, username: String, password: String): Result<XtreamAuthResponse>
    suspend fun getLiveCategories(serverUrl: String, username: String, password: String): Result<List<XtreamCategory>>
    suspend fun getLiveStreams(serverUrl: String, username: String, password: String, categoryId: String? = null): Result<List<XtreamLiveStream>>
    suspend fun getVodCategories(serverUrl: String, username: String, password: String): Result<List<XtreamCategory>>
    suspend fun getVodStreams(serverUrl: String, username: String, password: String, categoryId: String? = null): Result<List<XtreamVodStream>>
    suspend fun getSeriesCategories(serverUrl: String, username: String, password: String): Result<List<XtreamCategory>>
    suspend fun getSeries(serverUrl: String, username: String, password: String, categoryId: String? = null): Result<List<XtreamSeriesStream>>
    suspend fun getSeriesInfo(serverUrl: String, username: String, password: String, seriesId: Int): Result<XtreamSeriesInfo>
}
