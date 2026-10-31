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
            
            // OkHttp handles keeping most standard headers automatically across redirects.
            // Manually re-adding them here causes duplicate User-Agents, breaking some streaming nodes.
            // However, OkHttp deliberately drops `Set-Cookie` tokens on Cross-Domain redirects.
            // We MUST manually extract and inject the session cookie for the new CDN edge node.
            val cookies = response.headers("Set-Cookie")
            if (cookies.isNotEmpty()) {
                val cookieString = cookies.joinToString("; ") { it.substringBefore(";") }
                builder.header("Cookie", cookieString)
            }
            
            response.close()
            response = chain.proceed(builder.build())
        }
        
        return response
    }
}
