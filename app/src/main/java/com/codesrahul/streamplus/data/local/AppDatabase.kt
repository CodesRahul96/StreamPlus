package com.codesrahul.streamplus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, HistoryEntity::class, FavoriteEntity::class, ChannelEntity::class],
    version = 18,
    exportSchema = false
)
@androidx.room.TypeConverters(MapTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun channelDao(): ChannelDao
}
