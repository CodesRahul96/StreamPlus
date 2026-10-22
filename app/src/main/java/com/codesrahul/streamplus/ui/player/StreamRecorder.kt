package com.codesrahul.streamplus.ui.player

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class RecordingState(
    val isRecording: Boolean = false,
    val channelName: String = "",
    val durationSeconds: Long = 0L,
    val bytesWritten: Long = 0L,
    val filePath: String = ""
)

private data class HlsMediaInfo(
    val segmentUrls: List<String>,
    val targetDurationSeconds: Int,
    val hasEndList: Boolean
)

@Singleton
class StreamRecorder @Inject constructor() {

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var recordJob: Job? = null
    private var tickerJob: Job? = null
    private var currentFile: File? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun startRecording(
        context: Context,
        streamUrl: String,
        channelName: String,
        headers: Map<String, String> = emptyMap(),
        userAgent: String = ""
    ) {
        if (_recordingState.value.isRecording) {
            Log.w("StreamRecorder", "Already recording! Ignoring start request.")
            return
        }

        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "StreamPlus_Recordings")
        if (!dir.exists()) dir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cleanName = channelName.ifBlank { "Channel" }.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val outputFile = File(dir, "${cleanName}_${timestamp}.ts")
        currentFile = outputFile

        _recordingState.value = RecordingState(
            isRecording = true,
            channelName = channelName,
            durationSeconds = 0L,
            bytesWritten = 0L,
            filePath = outputFile.absolutePath
        )

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Started recording: $channelName", Toast.LENGTH_SHORT).show()
        }

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        tickerJob = scope.launch {
            while (isActive && _recordingState.value.isRecording) {
                delay(1000L)
                _recordingState.value = _recordingState.value.copy(
                    durationSeconds = _recordingState.value.durationSeconds + 1L,
                    bytesWritten = currentFile?.length() ?: 0L
                )
            }
        }

        recordJob = scope.launch {
            try {
                Log.d("StreamRecorder", "Beginning recording from $streamUrl -> ${outputFile.absolutePath}")

                val lowerUrl = streamUrl.substringBefore("?").lowercase()
                val isLikelyHls = lowerUrl.endsWith(".m3u8") || lowerUrl.contains("m3u8") || lowerUrl.contains("/hls/")

                if (isLikelyHls) {
                    recordHlsStream(streamUrl, outputFile, headers, userAgent)
                } else {
                    // Probe content type
                    var isHls = false
                    try {
                        val probeReq = buildRequest(streamUrl, headers, userAgent)
                        client.newCall(probeReq).execute().use { resp ->
                            val contentType = resp.header("Content-Type")?.lowercase() ?: ""
                            if (contentType.contains("mpegurl") || contentType.contains("application/x-mpegurl")) {
                                isHls = true
                            } else {
                                val peek = resp.peekBody(512).string()
                                if (peek.trim().startsWith("#EXTM3U")) {
                                    isHls = true
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("StreamRecorder", "Probe error: ${e.message}")
                    }

                    if (isHls) {
                        recordHlsStream(streamUrl, outputFile, headers, userAgent)
                    } else {
                        recordDirectStream(streamUrl, outputFile, headers, userAgent)
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e("StreamRecorder", "Recording stream error", e)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    stopRecording(context)
                }
            }
        }
    }

    private suspend fun recordHlsStream(
        streamUrl: String,
        outputFile: File,
        headers: Map<String, String>,
        userAgent: String
    ) = withContext(Dispatchers.IO) {
        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(outputFile, true)
            val downloadedSegments = Collections.synchronizedSet(LinkedHashSet<String>())
            var mediaPlaylistUrl = streamUrl

            val initialReq = buildRequest(streamUrl, headers, userAgent)
            val initialResp = client.newCall(initialReq).execute()
            val initialBody = initialResp.body?.string() ?: ""
            if (!initialResp.isSuccessful || initialBody.isBlank()) {
                Log.e("StreamRecorder", "Failed to fetch HLS playlist: HTTP ${initialResp.code}")
                return@withContext
            }

            if (initialBody.contains("#EXT-X-STREAM-INF")) {
                mediaPlaylistUrl = extractMediaPlaylistUrl(streamUrl, initialBody)
                Log.d("StreamRecorder", "Resolved HLS media playlist: $mediaPlaylistUrl")
            }

            var consecutiveErrors = 0
            while (isActive && _recordingState.value.isRecording) {
                try {
                    val req = buildRequest(mediaPlaylistUrl, headers, userAgent)
                    val resp = client.newCall(req).execute()
                    val playlistText = resp.body?.string() ?: ""

                    if (resp.isSuccessful && playlistText.isNotBlank()) {
                        consecutiveErrors = 0
                        val mediaInfo = parseMediaPlaylist(mediaPlaylistUrl, playlistText)
                        for (segUrl in mediaInfo.segmentUrls) {
                            if (!isActive || !_recordingState.value.isRecording) break
                            if (!downloadedSegments.contains(segUrl)) {
                                downloadedSegments.add(segUrl)
                                if (downloadedSegments.size > 2000) {
                                    val it = downloadedSegments.iterator()
                                    if (it.hasNext()) { it.next(); it.remove() }
                                }
                                downloadSegment(segUrl, outputStream, headers, userAgent)
                            }
                        }

                        if (mediaInfo.hasEndList) {
                            Log.d("StreamRecorder", "HLS reached #EXT-X-ENDLIST.")
                            break
                        }

                        val waitMs = (mediaInfo.targetDurationSeconds * 800L).coerceIn(1500L, 5000L)
                        delay(waitMs)
                    } else {
                        consecutiveErrors++
                        if (consecutiveErrors > 15) break
                        delay(2000L)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w("StreamRecorder", "Transient HLS poll error: ${e.message}")
                    consecutiveErrors++
                    if (consecutiveErrors > 15) break
                    delay(2000L)
                }
            }
        } finally {
            try { outputStream?.flush() } catch (ignored: Exception) {}
            try { outputStream?.close() } catch (ignored: Exception) {}
        }
    }

    private fun downloadSegment(
        segmentUrl: String,
        outputStream: FileOutputStream,
        headers: Map<String, String>,
        userAgent: String
    ) {
        try {
            val req = buildRequest(segmentUrl, headers, userAgent)
            val resp = client.newCall(req).execute()
            val body = resp.body
            if (resp.isSuccessful && body != null) {
                body.byteStream().use { inp ->
                    val buf = ByteArray(32 * 1024)
                    var r: Int
                    while (inp.read(buf).also { r = it } != -1 && _recordingState.value.isRecording) {
                        outputStream.write(buf, 0, r)
                    }
                }
                outputStream.flush()
            }
        } catch (e: Exception) {
            Log.w("StreamRecorder", "Segment download error: $segmentUrl -> ${e.message}")
        }
    }

    private suspend fun recordDirectStream(
        streamUrl: String,
        outputFile: File,
        headers: Map<String, String>,
        userAgent: String
    ) = withContext(Dispatchers.IO) {
        var outputStream: FileOutputStream? = null
        var inputStream: InputStream? = null
        try {
            outputStream = FileOutputStream(outputFile, true)
            val req = buildRequest(streamUrl, headers, userAgent)
            val resp = client.newCall(req).execute()
            val body = resp.body
            if (!resp.isSuccessful || body == null) {
                Log.e("StreamRecorder", "Failed to connect to direct stream: HTTP ${resp.code}")
                return@withContext
            }
            inputStream = body.byteStream()
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (isActive && _recordingState.value.isRecording) {
                bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
        } finally {
            try { inputStream?.close() } catch (ignored: Exception) {}
            try { outputStream?.flush() } catch (ignored: Exception) {}
            try { outputStream?.close() } catch (ignored: Exception) {}
        }
    }

    private fun extractMediaPlaylistUrl(masterUrl: String, content: String): String {
        val lines = content.lines()
        var selectedUri: String? = null
        var maxBandwidth = -1L

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                val bwMatch = Regex("BANDWIDTH=(\\d+)").find(line)
                val bw = bwMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                for (j in i + 1 until lines.size) {
                    val nextLine = lines[j].trim()
                    if (nextLine.isNotEmpty() && !nextLine.startsWith("#")) {
                        if (bw >= maxBandwidth) {
                            maxBandwidth = bw
                            selectedUri = nextLine
                        }
                        break
                    }
                }
            }
            i++
        }

        if (selectedUri == null) {
            selectedUri = lines.firstOrNull { it.isNotBlank() && !it.startsWith("#") }
        }

        return if (!selectedUri.isNullOrBlank()) {
            resolveUrl(masterUrl, selectedUri)
        } else {
            masterUrl
        }
    }

    private fun parseMediaPlaylist(playlistUrl: String, content: String): HlsMediaInfo {
        val lines = content.lines()
        val segmentUrls = mutableListOf<String>()
        var targetDuration = 4
        var hasEnd = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXT-X-TARGETDURATION:")) {
                targetDuration = trimmed.substringAfter(":").trim().toIntOrNull() ?: 4
            } else if (trimmed.startsWith("#EXT-X-ENDLIST")) {
                hasEnd = true
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val resolved = resolveUrl(playlistUrl, trimmed)
                segmentUrls.add(resolved)
            }
        }
        return HlsMediaInfo(segmentUrls, targetDuration, hasEnd)
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            if (relativeUrl.startsWith("http://", ignoreCase = true) || relativeUrl.startsWith("https://", ignoreCase = true)) {
                relativeUrl
            } else {
                URI(baseUrl).resolve(relativeUrl).toString()
            }
        } catch (e: Exception) {
            if (baseUrl.contains("/")) {
                baseUrl.substringBeforeLast("/") + "/" + relativeUrl.removePrefix("/")
            } else relativeUrl
        }
    }

    private fun buildRequest(url: String, headers: Map<String, String>, userAgent: String): Request {
        val reqBuilder = Request.Builder().url(url)
        if (userAgent.isNotBlank()) reqBuilder.header("User-Agent", userAgent)
        headers.forEach { (k, v) -> reqBuilder.header(k, v) }
        return reqBuilder.build()
    }

    fun stopRecording(context: Context) {
        if (!_recordingState.value.isRecording) return
        _recordingState.value = _recordingState.value.copy(isRecording = false)

        recordJob?.cancel()
        recordJob = null
        tickerJob?.cancel()
        tickerJob = null

        val file = currentFile
        if (file != null && file.exists() && file.length() > 0) {
            saveToPublicMovies(context, file)
        }
        Log.d("StreamRecorder", "Stream recording stopped.")
    }

    private fun saveToPublicMovies(context: Context, srcFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, srcFile.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp2t")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/StreamPlus_Recordings")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        srcFile.inputStream().use { inp ->
                            inp.copyTo(out)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                    Log.d("StreamRecorder", "Saved recording to MediaStore: $uri")
                }
            } else {
                @Suppress("DEPRECATION")
                val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "StreamPlus_Recordings")
                if (!publicDir.exists()) publicDir.mkdirs()
                val destFile = File(publicDir, srcFile.name)
                srcFile.copyTo(destFile, overwrite = true)
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("video/mp2t"), null)
            }
            MediaScannerConnection.scanFile(context, arrayOf(srcFile.absolutePath), arrayOf("video/mp2t"), null)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Recording saved: ${srcFile.name}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("StreamRecorder", "Failed to export recording to MediaStore", e)
        }
    }
}
