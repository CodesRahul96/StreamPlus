package com.codesrahul.streamplus.data.network

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

enum class StreamProtocol(val displayName: String) {
    HLS("HLS (m3u8)"),
    DASH("MPEG-DASH"),
    SMOOTH_STREAMING("SmoothStreaming"),
    RTSP("RTSP Live"),
    RTMP("RTMP"),
    PROGRESSIVE("Progressive Direct"),
    AUDIO_ONLY("Audio Stream"),
    UNKNOWN("Auto-Detected")
}

data class StreamProfile(
    val cleanUrl: String,
    val protocol: StreamProtocol,
    val containerFormat: String,
    val drmScheme: String? = null,
    val drmLicenseUrl: String? = null,
    val drmKeyId: String? = null,
    val drmKey: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val isLive: Boolean = true,
    val aiDiagnosticSummary: String = "",
    val suggestedTitle: String = ""
)

sealed class HealingAction {
    data class UpdateHeaders(val newHeaders: Map<String, String>, val reason: String) : HealingAction()
    data class FallbackDecoder(val useSoftware: Boolean, val reason: String) : HealingAction()
    object TrustAllSsl : HealingAction()
    object RestartLiveWindow : HealingAction()
    object None : HealingAction()
}

@Singleton
class StreamIntelligenceEngine @Inject constructor(
    private val client: OkHttpClient
) {

    private val PIPE_REGEX = "(?i)(\\?\\|)|(\\?%7C)|(\\|)|(%7C)".toRegex()
    private val CLEARKEY_HEX_REGEX = "([0-9a-fA-F]{32}):([0-9a-fA-F]{32})".toRegex()

    /**
     * Fast, zero-blocking AI inspection of raw URLs or multi-line configurations.
     */
    fun analyzeUrlFast(rawInput: String): StreamProfile {
        var url = rawInput.trim()
        val headers = mutableMapOf<String, String>()
        var drmScheme: String? = null
        var drmLicenseUrl: String? = null
        var drmKeyId: String? = null
        var drmKey: String? = null

        // 1. Parse M3U directives if pasted
        if (url.contains("#EXTINF") || url.contains("#KODIPROP") || url.contains("#EXTHTTP")) {
            val lines = url.lines()
            for (line in lines) {
                val t = line.trim()
                if (t.startsWith("http://", ignoreCase = true) || 
                    t.startsWith("https://", ignoreCase = true) ||
                    t.startsWith("rtsp://", ignoreCase = true) ||
                    t.startsWith("rtmp://", ignoreCase = true)) {
                    url = t
                } else if (t.startsWith("#EXTHTTP:")) {
                    try {
                        val json = org.json.JSONObject(t.removePrefix("#EXTHTTP:"))
                        json.keys().forEach { k -> headers[k] = json.getString(k) }
                    } catch (_: Exception) {}
                } else if (t.startsWith("#EXTVLCOPT:http-user-agent=")) {
                    headers["User-Agent"] = t.removePrefix("#EXTVLCOPT:http-user-agent=")
                } else if (t.startsWith("#EXTVLCOPT:http-referrer=")) {
                    headers["Referer"] = t.removePrefix("#EXTVLCOPT:http-referrer=")
                } else if (t.startsWith("#KODIPROP:inputstream.adaptive.license_type=")) {
                    drmScheme = t.removePrefix("#KODIPROP:inputstream.adaptive.license_type=").trim()
                } else if (t.startsWith("#KODIPROP:inputstream.adaptive.license_key=")) {
                    drmLicenseUrl = t.removePrefix("#KODIPROP:inputstream.adaptive.license_key=").trim()
                }
            }
        }

        // 2. Parse Pipe parameters (URL|User-Agent=...&Referer=...)
        val pipeMatch = PIPE_REGEX.find(url)
        if (pipeMatch != null) {
            val urlPart = url.substring(0, pipeMatch.range.first).trim()
            val params = url.substring(pipeMatch.range.last + 1)
            url = urlPart
            params.split("&").forEach { pair ->
                val kv = pair.split("=", limit = 2)
                if (kv.size == 2) {
                    val key = kv[0].trim()
                    val value = try { java.net.URLDecoder.decode(kv[1], "UTF-8") } catch (_: Exception) { kv[1] }
                    when {
                        key.equals("User-Agent", ignoreCase = true) -> headers["User-Agent"] = value
                        key.equals("Referer", ignoreCase = true) -> headers["Referer"] = value
                        key.equals("Origin", ignoreCase = true) -> headers["Origin"] = value
                        key.equals("Cookie", ignoreCase = true) -> headers["Cookie"] = value
                        key.equals("license_key", ignoreCase = true) || key.equals("DRM-LicenseUrl", ignoreCase = true) -> drmLicenseUrl = value
                        key.equals("license_type", ignoreCase = true) || key.equals("DRM-Scheme", ignoreCase = true) -> drmScheme = value
                        else -> headers[key] = value
                    }
                }
            }
        }

        // 3. ClearKey Hex extraction (e.g. from key string or license url)
        val targetToCheck = "${url} ${drmLicenseUrl ?: ""}"
        val ckMatch = CLEARKEY_HEX_REGEX.find(targetToCheck)
        if (ckMatch != null) {
            drmScheme = "clearkey"
            drmKeyId = ckMatch.groupValues[1]
            drmKey = ckMatch.groupValues[2]
            drmLicenseUrl = "https://clearkey.direct/${drmKeyId}:${drmKey}"
        }

        // 4. Protocol & Container Detection
        val lowerUrl = url.lowercase()
        val (protocol, container, isLive) = when {
            lowerUrl.startsWith("rtsp://") -> Triple(StreamProtocol.RTSP, "RTSP Live Stream", true)
            lowerUrl.startsWith("rtmp://") -> Triple(StreamProtocol.RTMP, "RTMP Stream", true)
            lowerUrl.contains(".m3u8") || lowerUrl.contains("/hls/") || lowerUrl.contains("format=m3u8") -> Triple(StreamProtocol.HLS, "HLS Manifest (.m3u8)", true)
            lowerUrl.contains(".mpd") || lowerUrl.contains("/dash/") || lowerUrl.contains("format=mpd") -> Triple(StreamProtocol.DASH, "MPEG-DASH (.mpd)", true)
            lowerUrl.contains(".ism") || lowerUrl.contains("/manifest") -> Triple(StreamProtocol.SMOOTH_STREAMING, "SmoothStreaming (.ism)", true)
            lowerUrl.contains(".mp4") || lowerUrl.contains(".m4v") -> Triple(StreamProtocol.PROGRESSIVE, "MP4 Video", false)
            lowerUrl.contains(".mkv") -> Triple(StreamProtocol.PROGRESSIVE, "Matroska (MKV)", false)
            lowerUrl.contains(".ts") || lowerUrl.contains(".m2ts") -> Triple(StreamProtocol.PROGRESSIVE, "MPEG-TS Stream", true)
            lowerUrl.contains(".webm") -> Triple(StreamProtocol.PROGRESSIVE, "WebM Video", false)
            lowerUrl.contains(".flv") -> Triple(StreamProtocol.PROGRESSIVE, "Flash Video (FLV)", true)
            lowerUrl.contains(".avi") -> Triple(StreamProtocol.PROGRESSIVE, "Audio Video Interleave (AVI)", false)
            lowerUrl.contains(".mp3") || lowerUrl.contains(".aac") || lowerUrl.contains(".flac") || lowerUrl.contains(".ogg") -> Triple(StreamProtocol.AUDIO_ONLY, "Audio Container", false)
            else -> Triple(StreamProtocol.UNKNOWN, "Auto-Adaptive Stream", true)
        }

        // 5. Anti-Hotlink & Anti-Block Header Auto-Synthesis
        val domain = try { URI(url).host ?: "" } catch (_: Exception) { "" }
        if (domain.isNotEmpty()) {
            if (!headers.containsKey("User-Agent")) {
                headers["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            }
            if (!headers.containsKey("Referer") && (lowerUrl.contains("m3u8") || lowerUrl.contains("stream") || lowerUrl.contains("live") || lowerUrl.contains("cdn"))) {
                val scheme = if (url.startsWith("https://")) "https" else "http"
                headers["Referer"] = "$scheme://$domain/"
            }
            if (!headers.containsKey("Origin") && headers.containsKey("Referer")) {
                val scheme = if (url.startsWith("https://")) "https" else "http"
                headers["Origin"] = "$scheme://$domain"
            }
        }

        // 6. Generate AI Diagnostic Summary
        val drmInfo = when (drmScheme?.lowercase()) {
            "widevine" -> "Widevine Modular DRM"
            "clearkey" -> "ClearKey Hex Decryption"
            "playready" -> "Microsoft PlayReady DRM"
            else -> "No DRM (Clear Stream)"
        }
        val summary = "${protocol.displayName} • $container • $drmInfo • Anti-Block Active"

        val derivedTitle = try {
            val path = URI(url).path
            val lastSegment = path?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            lastSegment ?: domain
        } catch (_: Exception) { url }

        return StreamProfile(
            cleanUrl = url,
            protocol = protocol,
            containerFormat = container,
            drmScheme = drmScheme,
            drmLicenseUrl = drmLicenseUrl,
            drmKeyId = drmKeyId,
            drmKey = drmKey,
            headers = headers,
            isLive = isLive,
            aiDiagnosticSummary = summary,
            suggestedTitle = derivedTitle
        )
    }

    /**
     * Deep probe for tokenized or extension-less URLs via network sniffing.
     */
    suspend fun probeStreamDeep(url: String, headers: Map<String, String> = emptyMap()): StreamProfile = withContext(Dispatchers.IO) {
        val fastProfile = analyzeUrlFast(url)
        if (fastProfile.protocol != StreamProtocol.UNKNOWN) {
            return@withContext fastProfile
        }

        try {
            val reqBuilder = Request.Builder().url(fastProfile.cleanUrl).head()
            fastProfile.headers.forEach { (k, v) -> reqBuilder.header(k, v) }
            headers.forEach { (k, v) -> reqBuilder.header(k, v) }
            
            client.newCall(reqBuilder.build()).execute().use { response ->
                val cType = response.header("Content-Type")?.lowercase() ?: ""
                val resolvedProtocol = when {
                    cType.contains("mpegurl") || cType.contains("m3u8") -> StreamProtocol.HLS
                    cType.contains("dash") || cType.contains("mpd") -> StreamProtocol.DASH
                    cType.contains("smooth") || cType.contains("ms-sstr") -> StreamProtocol.SMOOTH_STREAMING
                    cType.contains("video/mp4") -> StreamProtocol.PROGRESSIVE
                    cType.contains("video/x-matroska") -> StreamProtocol.PROGRESSIVE
                    cType.contains("video/mp2t") -> StreamProtocol.PROGRESSIVE
                    cType.contains("audio/") -> StreamProtocol.AUDIO_ONLY
                    else -> StreamProtocol.HLS // Default IPTV live fallback
                }
                val format = when (resolvedProtocol) {
                    StreamProtocol.HLS -> "HLS Manifest (Sniffed)"
                    StreamProtocol.DASH -> "MPEG-DASH (Sniffed)"
                    StreamProtocol.SMOOTH_STREAMING -> "SmoothStreaming (Sniffed)"
                    StreamProtocol.PROGRESSIVE -> "Progressive Video ($cType)"
                    StreamProtocol.AUDIO_ONLY -> "Audio Stream ($cType)"
                    else -> "Adaptive Stream"
                }
                return@withContext fastProfile.copy(
                    protocol = resolvedProtocol,
                    containerFormat = format,
                    aiDiagnosticSummary = "${resolvedProtocol.displayName} • $format • AI Verified"
                )
            }
        } catch (_: Exception) {
            fastProfile.copy(
                protocol = StreamProtocol.HLS,
                containerFormat = "HLS Stream (Auto-Assumed)",
                aiDiagnosticSummary = "HLS Stream • Fallback Auto-Assumed"
            )
        }
    }

    /**
     * Evaluates playback errors and provides self-healing instructions.
     */
    fun evaluatePlaybackError(error: Throwable, currentUrl: String, currentHeaders: Map<String, String>): HealingAction {
        val msg = error.message?.lowercase() ?: ""
        val errorStr = error.toString().lowercase()

        return when {
            // 403 Forbidden / 401 Unauthorized -> Anti-Hotlink retry with Android TV UA and direct Origin
            errorStr.contains("response code: 403") || errorStr.contains("response code: 401") || msg.contains("403") -> {
                val newHeaders = currentHeaders.toMutableMap()
                newHeaders["User-Agent"] = "Mozilla/5.0 (Linux; Android 12; BRAVIA 4K Build/STT2.200922.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
                try {
                    val domain = URI(currentUrl).host
                    if (domain != null) {
                        newHeaders["Referer"] = "https://$domain/"
                        newHeaders["Origin"] = "https://$domain"
                    }
                } catch (_: Exception) {}
                HealingAction.UpdateHeaders(newHeaders, "Bypassing 403 Forbidden with Smart TV Headers")
            }

            // Decoder init failures / MediaCodec errors -> Fall back to software Google codecs
            errorStr.contains("mediacodec") || errorStr.contains("decoderinitexception") || errorStr.contains("unsupported") -> {
                HealingAction.FallbackDecoder(useSoftware = true, reason = "Switching to Software Decoder for unsupported profile")
            }

            // SSL certificate errors -> Trust all SSL
            errorStr.contains("sslhandshakeexception") || errorStr.contains("certpathvalidatorexception") || errorStr.contains("trust") -> {
                HealingAction.TrustAllSsl
            }

            // Behind live window in HLS/DASH -> Seek to default live edge
            errorStr.contains("behindlivewindowexception") -> {
                HealingAction.RestartLiveWindow
            }

            else -> HealingAction.None
        }
    }
}
