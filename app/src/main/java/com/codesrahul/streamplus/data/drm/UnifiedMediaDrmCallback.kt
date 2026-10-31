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

        // 1.5. Handle ClearKey direct keypair (clearkey.direct/kid:key or raw kid:key)
        if (uuid == C.CLEARKEY_UUID && (licenseUri.contains("clearkey.direct", ignoreCase = true) || 
            (licenseUri.contains(":") && !licenseUri.contains("?") && !licenseUri.contains("&")))) {
            val keyPairString = licenseUri.substringAfter("clearkey.direct/").removePrefix("http://").removePrefix("https://").trim('/')
            val pairMatch = Regex("([0-9a-zA-Z_\\-]{22,32}):([0-9a-zA-Z_\\-]{22,32})").find(keyPairString)
            if (pairMatch != null) {
                try {
                    val kidStr = pairMatch.groupValues[1]
                    val keyStr = pairMatch.groupValues[2]
                    val kidBytes = if (kidStr.length == 32 && kidStr.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) hexToByteArray(kidStr) else Base64.decode(kidStr, Base64.DEFAULT)
                    val keyBytes = if (keyStr.length == 32 && keyStr.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) hexToByteArray(keyStr) else Base64.decode(keyStr, Base64.DEFAULT)
                    val kidB64 = Base64.encodeToString(kidBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                    val keyB64 = Base64.encodeToString(keyBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                    val synthesizedJson = "{\"keys\":[{\"kty\":\"oct\",\"k\":\"$keyB64\",\"kid\":\"$kidB64\"}],\"type\":\"temporary\"}"
                    Log.d("UnifiedMediaDrm", "Synthesized ClearKey JSON from keypair: $synthesizedJson")
                    return synthesizedJson.toByteArray()
                } catch (e: Exception) {
                    Log.w("UnifiedMediaDrm", "Failed to synthesize ClearKey from keypair: ${e.message}")
                }
            }
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

    private fun hexToByteArray(hex: String): ByteArray {
        val s = hex.replace(" ", "").replace(":", "")
        if (s.length % 2 != 0) return ByteArray(0)
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun fetchClearKeyFromUrl(url: String, requestData: ByteArray): ByteArray? {
        // 1. Try GET first (most common for static keys and simple proxies, avoids POST timeouts)
        try {
            val reqBuilder = Request.Builder().url(url)
            allHeaders?.forEach { (k, v) -> reqBuilder.header(k.trim(), v.trim()) }
            Log.d("UnifiedMediaDrm", "Fetching ClearKey JSON via GET from: $url")
            okHttpClient.newCall(reqBuilder.get().build()).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    Log.d("UnifiedMediaDrm", "ClearKey GET response: $responseBody")
                    if (isValidClearKeyResponse(responseBody)) {
                        return responseBody.toByteArray()
                    }
                } else {
                    Log.w("UnifiedMediaDrm", "ClearKey GET failed with code: ${response.code}")
                }
                Unit
            }
        } catch (e: Exception) {
            Log.w("UnifiedMediaDrm", "ClearKey GET failed: ${e.message}")
        }

        // 2. Try POST with request data (standard W3C ClearKey protocol)
        try {
            val reqBuilder = Request.Builder().url(url)
            allHeaders?.forEach { (k, v) -> reqBuilder.header(k.trim(), v.trim()) }
            val body = requestData.toRequestBody("application/json".toMediaType())
            Log.d("UnifiedMediaDrm", "POSTing ClearKey request to: $url (body size: ${requestData.size})")
            okHttpClient.newCall(reqBuilder.post(body).build()).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    Log.d("UnifiedMediaDrm", "ClearKey POST response: $responseBody")
                    if (isValidClearKeyResponse(responseBody)) {
                        return responseBody.toByteArray()
                    }
                } else {
                    Log.w("UnifiedMediaDrm", "ClearKey POST failed with code: ${response.code}")
                }
                Unit
            }
        } catch (e: Exception) {
            Log.w("UnifiedMediaDrm", "ClearKey POST failed: ${e.message}")
        }

        return null
    }

    private fun isValidClearKeyResponse(body: String): Boolean {
        return body.contains("\"keys\"") && body.contains("\"k\"") && body.contains("\"kid\"")
    }
}
