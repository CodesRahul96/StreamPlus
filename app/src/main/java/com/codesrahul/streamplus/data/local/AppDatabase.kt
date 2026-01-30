package com.codesrahul.streamplus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.codesrahul.streamplus.data.local.PlaylistDao
import com.codesrahul.streamplus.data.local.PlaylistEntity

@Database(entities = [PlaylistEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}
