package com.codesrahul.streamplus.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.database.Cursor

object AppUpdater {

    private const val UPDATE_URL = "https://raw.githubusercontent.com/CodesRahul96/StreamPlus-Updates/main/version.json"

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersionCode: Int,
        val latestVersionName: String,
        val content: String,
        val downloadUrl: String
    )

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(UPDATE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)

                val latestVersionCode = jsonObject.getInt("versionCode")
                val latestVersionName = jsonObject.getString("versionName")
                val content = jsonObject.optString("content", "Bug fixes and improvements")
                val downloadUrl = jsonObject.getString("url")

                val currentVersionCode = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
                } catch (e: Exception) {
                    // Fallback to 1 if we somehow can't read our own package info
                    1
                }

                return@withContext UpdateInfo(
                    isUpdateAvailable = latestVersionCode > currentVersionCode,
                    latestVersionCode = latestVersionCode,
                    latestVersionName = latestVersionName,
                    content = content,
                    downloadUrl = downloadUrl
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    fun downloadAndInstallUpdate(context: Context, downloadUrl: String, onDownloadStarted: () -> Unit) {
        val fileName = "StreamPlus-Update.apk"
        val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Downloading StreamPlus Update")
            .setDescription("Please wait...")
            .setDestinationUri(Uri.fromFile(destinationFile))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)
        onDownloadStarted()

        CoroutineScope(Dispatchers.IO).launch {
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    
                    if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                        val bytesDownloaded = cursor.getInt(bytesDownloadedIndex)
                        val bytesTotal = cursor.getInt(bytesTotalIndex)
                        if (bytesTotal > 0) {
                            _downloadProgress.value = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                        }
                    }
                    
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusIndex != -1) {
                        val status = cursor.getInt(statusIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                            downloading = false
                        }
                    }
                }
                cursor.close()
                delay(100)
            }
        }

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(context, destinationFile)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                // We should ideally wait for result, but for simplicity, 
                // we'll instruct the user to click the file again or retry.
                android.widget.Toast.makeText(context, "Please enable unknown sources and retry.", android.widget.Toast.LENGTH_LONG).show()
                return
            }
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for older devices or if provider fails
            val fallbackIntent = Intent(Intent.ACTION_VIEW)
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            fallbackIntent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
            try {
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
