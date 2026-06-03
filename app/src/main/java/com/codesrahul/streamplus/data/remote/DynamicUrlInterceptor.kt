package com.codesrahul.streamplus.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class DynamicUrlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        
        // Check for our custom header that specifies the target base URL
        val targetUrlStr = request.header("X-Target-Url")
        if (!targetUrlStr.isNullOrBlank()) {
            val targetUrl = targetUrlStr.toHttpUrlOrNull()
            if (targetUrl != null) {
                // Remove the custom header so it isn't sent to the remote server
                val newUrl = request.url.newBuilder()
                    .scheme(targetUrl.scheme)
                    .host(targetUrl.host)
                    .port(targetUrl.port)
                    // Keep the original path segments (e.g. player_api.php)
                    .build()
                
                request = request.newBuilder()
                    .url(newUrl)
                    .removeHeader("X-Target-Url")
                    .build()
            }
        }
        return chain.proceed(request)
    }
}
