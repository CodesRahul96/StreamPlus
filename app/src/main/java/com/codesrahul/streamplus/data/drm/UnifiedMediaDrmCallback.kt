package com.codesrahul.streamplus.data.drm

import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.drm.ExoMediaDrm
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback
import androidx.media3.exoplayer.drm.MediaDrmCallback
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID

@UnstableApi
class UnifiedMediaDrmCallback(
    private val defaultLicenseUrl: String?,
    private val httpDataSourceFactory: HttpDataSource.Factory,
    private val okHttpClient: okhttp3.Call.Factory,
    private val allHeaders: Map<String, String>? = null
) : MediaDrmCallback {

    private val httpCallback = HttpMediaDrmCallback(defaultLicenseUrl, httpDataSourceFactory).apply {
        allHeaders?.forEach { (k, v) ->
            // Sanitize headers for standard callback too
            setKeyRequestProperty(k.trim(), v.trim())
        }
    }

    override fun executeProvisionRequest(uuid: UUID, request: ExoMediaDrm.ProvisionRequest): ByteArray {
        return httpCallback.executeProvisionRequest(uuid, request)
    }

    override fun executeKeyRequest(uuid: UUID, request: ExoMediaDrm.KeyRequest): ByteArray {
        val rawUri = request.licenseServerUrl
        val finalLicenseUri = (if (rawUri.isNullOrBlank()) defaultLicenseUrl else rawUri) ?: ""
        val licenseUri = finalLicenseUri.trim()
        Log.d("UnifiedMediaDrm", "executeKeyRequest: UUID=$uuid, URL=$licenseUri")
        
        // 0. Check if ClearKey JSON is provided in headers (for embedded keys)
        val headers = allHeaders
        if (uuid == C.CLEARKEY_UUID && headers != null) {
            val clearKeyHeaderKey = headers.keys.find { it.equals("X-ClearKey-Data", ignoreCase = true) }
            if (clearKeyHeaderKey != null) {
                val clearKeyJson = headers[clearKeyHeaderKey] ?: ""
                Log.d("UnifiedMediaDrm", "Using ClearKey JSON from headers ($clearKeyHeaderKey): $clearKeyJson")
                return clearKeyJson.toByteArray()
            }
        }

        // 1. Handle "data:" URIs (Common for embedded ClearKey)
        if (licenseUri.startsWith("data:")) {
            Log.d("UnifiedMediaDrm", "Handling data URI")
            return handleDataUri(licenseUri)
        }

        // 2. Handle ClearKey over HTTP:
        //    POST the ExoPlayer key request body to the license URL (standard W3C ClearKey protocol).
        //    Many proxy servers (e.g. Jio/Cloudflare Workers) return the key JSON in response to a POST.
        //    We do NOT filter by URL content — any http URL for CLEARKEY_UUID gets the POST treatment.
        if (uuid == C.CLEARKEY_UUID && licenseUri.startsWith("http")) {
            val clearKeyBytes = fetchClearKeyFromUrl(licenseUri, request.data)
            if (clearKeyBytes != null) return clearKeyBytes
        }

        // 3. Fallback to Standard ExoPlayer HTTP Callback
        Log.d("UnifiedMediaDrm", "Falling back to standard HttpMediaDrmCallback for $licenseUri")
        try {
            val response = httpCallback.executeKeyRequest(uuid, request)
            Log.d("UnifiedMediaDrm", "Standard DRM Request Success! Response size: ${response.size}")
            return response
        } catch (e: Exception) {
            Log.e("UnifiedMediaDrm", "Standard DRM Request Failed for $licenseUri: ${e.message}", e)
            if (e is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                Log.e("UnifiedMediaDrm", "Response Code: ${e.responseCode}")
                Log.e("UnifiedMediaDrm", "Response Message: ${e.message}")
                e.headerFields.entries.forEach { entry -> Log.e("UnifiedMediaDrm", "Header: ${entry.key} = ${entry.value}") }
            }
            throw e
        }
    }

    private fun handleDataUri(uri: String): ByteArray {
        return try {
            val dataPart = uri.substringAfter(",")
            if (uri.contains("base64")) {
                try {
                    Base64.decode(dataPart, Base64.DEFAULT)
                } catch (e: IllegalArgumentException) {
                     Log.w("UnifiedMediaDrm", "Base64 DEFAULT failed, trying NO_WRAP")
                     Base64.decode(dataPart, Base64.NO_WRAP)
                }
            } else {
                dataPart.toByteArray()
            }
        } catch (e: Exception) {
            Log.e("UnifiedMediaDrm", "Failed to decode license from data URI", e)
            ByteArray(0)
        }
    }

    private fun fetchClearKeyFromUrl(url: String, requestData: ByteArray): ByteArray? {
        // First try: POST the ExoPlayer key request body (standard W3C ClearKey protocol)
        try {
            val reqBuilder = Request.Builder().url(url)
            allHeaders?.forEach { (k, v) -> reqBuilder.header(k.trim(), v.trim()) }
            val body = requestData.toRequestBody("application/json".toMediaType())
            Log.d("UnifiedMediaDrm", "POSTing ClearKey request to: $url (body size: ${requestData.size})")
            val response = okHttpClient.newCall(reqBuilder.post(body).build()).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                Log.d("UnifiedMediaDrm", "ClearKey POST response: $responseBody")
                if (isValidClearKeyResponse(responseBody)) return responseBody.toByteArray()
            } else {
                Log.w("UnifiedMediaDrm", "ClearKey POST failed with code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.w("UnifiedMediaDrm", "ClearKey POST failed, will try GET: ${e.message}")
        }

        // Fallback: GET (some servers just serve the JSON directly at the URL)
        return try {
            val reqBuilder = Request.Builder().url(url)
            allHeaders?.forEach { (k, v) -> reqBuilder.header(k.trim(), v.trim()) }
            Log.d("UnifiedMediaDrm", "Fetching ClearKey JSON via GET from: $url")
            val response = okHttpClient.newCall(reqBuilder.get().build()).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                Log.d("UnifiedMediaDrm", "ClearKey GET response: $responseBody")
                if (isValidClearKeyResponse(responseBody)) responseBody.toByteArray() else null
            } else null
        } catch (e: Exception) {
            Log.w("UnifiedMediaDrm", "Failed to fetch ClearKey JSON via GET: ${e.message}")
            null
        }
    }

    private fun isValidClearKeyResponse(body: String): Boolean {
        return body.contains("\"keys\"") && body.contains("\"k\"") && body.contains("\"kid\"")
    }
}
