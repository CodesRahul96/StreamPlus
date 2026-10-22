package com.codesrahul.streamplus.data.epg

import android.content.Context
import android.util.Log
import androidx.work.*
import com.codesrahul.streamplus.data.local.PlaylistDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class EPGSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface EPGSyncWorkerEntryPoint {
        fun playlistDao(): PlaylistDao
        fun epgManager(): EPGManager
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("EPGSyncWorker", "Starting periodic EPG background sync...")
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                EPGSyncWorkerEntryPoint::class.java
            )
            val playlistDao = entryPoint.playlistDao()
            val epgManager = entryPoint.epgManager()

            val playlists = playlistDao.getAllPlaylistsList()
            var syncedCount = 0

            for (pl in playlists) {
                val epgUrl = pl.epgUrl
                if (!epgUrl.isNullOrBlank()) {
                    try {
                        Log.d("EPGSyncWorker", "Syncing EPG for playlist '${pl.name}' from $epgUrl")
                        epgManager.loadEPG(epgUrl)
                        syncedCount++
                    } catch (e: Exception) {
                        Log.w("EPGSyncWorker", "Failed to sync EPG for '${pl.name}'", e)
                    }
                }
            }

            Log.d("EPGSyncWorker", "Periodic EPG sync completed successfully for $syncedCount playlists.")
            Result.success()
        } catch (e: Exception) {
            Log.e("EPGSyncWorker", "EPGSyncWorker failed with exception", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "com.codesrahul.streamplus.epg_sync_work"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<EPGSyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.d("EPGSyncWorker", "EPG 12-hour periodic sync worker scheduled.")
        }

        fun syncImmediately(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<EPGSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeRequest)
            Log.d("EPGSyncWorker", "Immediate one-time EPG sync enqueued.")
        }
    }
}
