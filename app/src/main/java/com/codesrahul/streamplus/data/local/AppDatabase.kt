package com.codesrahul.streamplus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.codesrahul.streamplus.data.local.HistoryDao
import com.codesrahul.streamplus.data.local.HistoryEntity
import com.codesrahul.streamplus.data.local.PlaylistDao
import com.codesrahul.streamplus.data.local.PlaylistEntity

@Database(entities = [PlaylistEntity::class, HistoryEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
}
