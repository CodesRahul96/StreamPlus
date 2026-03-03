package com.codesrahul.streamplus.data.network

import okhttp3.Interceptor
import okhttp3.Response

class CrossDomainForwardingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var response = chain.proceed(originalRequest)

        while (response.isRedirect) {
            val location = response.header("Location") ?: break
            val redirectUrl = response.request.url.resolve(location) ?: break
            
            val originalRequest = response.request
            val builder = originalRequest.newBuilder().url(redirectUrl)
            
            // Standard HTTP Redirect Handling (RFC 7231):
            // 301, 302, 303: Method changes to GET and body is dropped *unless* it's a POST we want to preserve.
            // For DRM and certain APIs, even 302 should preserve POST if there is a body.
            if ((response.code == 301 || response.code == 302 || response.code == 303) && originalRequest.body == null) {
                builder.method("GET", null)
                builder.removeHeader("Content-Type")
                builder.removeHeader("Content-Length")
            }
            
            // Re-attach original headers that are often stripped by OkHttp on cross-domain
            val originalHeaders = response.request.headers
            for (i in 0 until originalHeaders.size) {
                val name = originalHeaders.name(i)
                if (name.equals("Host", ignoreCase = true) || 
                    name.equals("Content-Length", ignoreCase = true) ||
                    name.equals("Content-Type", ignoreCase = true)) continue
                
                // Use addHeader to avoid accidental overwrites of redirected headers
                builder.addHeader(name, originalHeaders.value(i))
            }
            
            response.close()
            response = chain.proceed(builder.build())
        }
        
        return response
    }
}
