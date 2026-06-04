package com.codesrahul.streamplus.data.repository

import com.codesrahul.streamplus.data.remote.XtreamApiService
import com.codesrahul.streamplus.data.remote.model.*
import com.codesrahul.streamplus.domain.repository.XtreamRepository
import com.codesrahul.streamplus.domain.repository.XtreamResult
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response

class XtreamRepositoryImpl(
    private val apiService: XtreamApiService
) : XtreamRepository {

    private suspend fun <T> safeApiCall(
        serverUrl: String,
        apiCall: suspend () -> Response<T>
    ): XtreamResult<T> {
        return try {
            // Retrofit does not allow dynamic base URLs easily per-request without an interceptor. 
            // We use a custom Interceptor that reads a Header, but since we cannot inject headers
            // dynamically to the existing calls easily without @Header annotations in the interface,
            // we will let OkHttp Interceptor do the routing. Wait, Retrofit interceptors work
            // globally. To make it thread-safe for concurrent different playlist requests,
            // we will need to modify our apiService interface or we can just hope calls don't overlap,
            // or even better, we modify our apiService to take a dynamic @Url. Let's stick with the Interceptor
            // approach but we MUST pass the Header. Since we didn't add @Header in XtreamApiService,
            // the safest approach is to create a new OkHttpClient instance per request or modify the interface.
            // Wait, we didn't add @Header to the API endpoints. Let's fix that.
            val response = apiCall()
            if (response.isSuccessful) {
                response.body()?.let {
                    XtreamResult.Success(it)
                } ?: XtreamResult.Failure(Exception("Empty response body"))
            } else {
                XtreamResult.Failure(Exception("API call failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            XtreamResult.Failure(e)
        }
    }

    // A better approach since we didn't add @Header to the interface is to recreate the Retrofit client here,
    // or just adjust the api interface in the next edit. We will assume the API interface is updated or we use @Url.
    // Actually, Retrofit requires endpoints to define @Header to pass them dynamically. So we should update XtreamApiService.
    
    // For now, let's implement assuming XtreamApiService takes targetUrl somehow, or we just rely on Okhttp. 
    // Actually we'll rewrite XtreamApiService to use @Url.

    override suspend fun authenticate(serverUrl: String, username: String, password: String) = safeApiCall(serverUrl) {
        apiService.authenticate(serverUrl + "/player_api.php", username, password)
    }

    override suspend fun getLiveCategories(serverUrl: String, username: String, password: String) = safeApiCall(serverUrl) {
        apiService.getLiveCategories(serverUrl + "/player_api.php", username, password)
    }

    override suspend fun getLiveStreams(serverUrl: String, username: String, password: String, categoryId: String?) = safeApiCall(serverUrl) {
        apiService.getLiveStreams(serverUrl + "/player_api.php", username, password, categoryId = categoryId)
    }

    override suspend fun getVodCategories(serverUrl: String, username: String, password: String) = safeApiCall(serverUrl) {
        apiService.getVodCategories(serverUrl + "/player_api.php", username, password)
    }

    override suspend fun getVodStreams(serverUrl: String, username: String, password: String, categoryId: String?) = safeApiCall(serverUrl) {
        apiService.getVodStreams(serverUrl + "/player_api.php", username, password, categoryId = categoryId)
    }

    override suspend fun getSeriesCategories(serverUrl: String, username: String, password: String) = safeApiCall(serverUrl) {
        apiService.getSeriesCategories(serverUrl + "/player_api.php", username, password)
    }

    override suspend fun getSeries(serverUrl: String, username: String, password: String, categoryId: String?) = safeApiCall(serverUrl) {
        apiService.getSeries(serverUrl + "/player_api.php", username, password, categoryId = categoryId)
    }

    override suspend fun getSeriesInfo(serverUrl: String, username: String, password: String, seriesId: Int) = safeApiCall(serverUrl) {
        apiService.getSeriesInfo(serverUrl + "/player_api.php", username, password, seriesId = seriesId)
    }
}
