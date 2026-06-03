package com.codesrahul.streamplus.data.remote

import com.codesrahul.streamplus.data.remote.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApiService {

    @GET
    suspend fun authenticate(
        @retrofit2.http.Url url: String,
        @Query("username") username: String,
        @Query("password") password: String
    ): retrofit2.Response<XtreamAuthResponse>

    @GET
    suspend fun getLiveCategories(
        @retrofit2.http.Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): retrofit2.Response<List<XtreamCategory>>

    @GET
    suspend fun getLiveStreams(
        @retrofit2.http.Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null
    ): retrofit2.Response<List<XtreamLiveStream>>

    @GET
    suspend fun getVodCategories(
        @retrofit2.http.Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): retrofit2.Response<List<XtreamCategory>>

    @GET
    suspend fun getVodStreams(
        @retrofit2.http.Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String? = null
    ): retrofit2.Response<List<XtreamVodStream>>

    @GET
    suspend fun getSeriesCategories(
        @retrofit2.http.Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): retrofit2.Response<List<XtreamCategory>>

    @GET
    suspend fun getSeries(
        @retrofit2.http.Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series",
        @Query("category_id") categoryId: String? = null
    ): retrofit2.Response<List<XtreamSeriesStream>>

    @GET
    suspend fun getSeriesInfo(
        @retrofit2.http.Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: Int
    ): retrofit2.Response<XtreamSeriesInfo>
}
