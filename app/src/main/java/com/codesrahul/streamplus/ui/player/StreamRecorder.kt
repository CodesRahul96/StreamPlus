package com.codesrahul.streamplus.ui.player

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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
        val cleanName = channelName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val outputFile = File(dir, "${cleanName}_${timestamp}.ts")
        currentFile = outputFile

        _recordingState.value = RecordingState(
            isRecording = true,
            channelName = channelName,
            durationSeconds = 0L,
            bytesWritten = 0L,
            filePath = outputFile.absolutePath
        )

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
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                Log.d("StreamRecorder", "Beginning stream recording from $streamUrl -> ${outputFile.absolutePath}")
                val reqBuilder = Request.Builder().url(streamUrl)
                if (userAgent.isNotBlank()) reqBuilder.header("User-Agent", userAgent)
                headers.forEach { (k, v) -> reqBuilder.header(k, v) }

                val response = client.newCall(reqBuilder.build()).execute()
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    Log.e("StreamRecorder", "Failed to connect to stream: ${response.code}")
                    stopRecording(context)
                    return@launch
                }

                inputStream = body.byteStream()
                outputStream = FileOutputStream(outputFile)
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int

                while (isActive && _recordingState.value.isRecording) {
                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
                Log.d("StreamRecorder", "Finished recording to ${outputFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("StreamRecorder", "Recording stream error", e)
            } finally {
                try { inputStream?.close() } catch (ignored: Exception) {}
                try { outputStream?.close() } catch (ignored: Exception) {}
                stopRecording(context)
            }
        }
    }

    fun stopRecording(context: Context) {
        if (!_recordingState.value.isRecording) return

        recordJob?.cancel()
        tickerJob?.cancel()

        currentFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("video/mp2t")
                ) { path, uri ->
                    Log.d("StreamRecorder", "Recorded media scanned: $path -> $uri")
                }
            }
        }

        _recordingState.value = _recordingState.value.copy(isRecording = false)
        Log.d("StreamRecorder", "Stream recording stopped.")
    }
}
