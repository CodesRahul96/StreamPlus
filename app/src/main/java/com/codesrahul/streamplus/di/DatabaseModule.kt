package com.codesrahul.streamplus.di

import android.content.Context
import androidx.room.Room
import com.codesrahul.streamplus.data.local.AppDatabase
import com.codesrahul.streamplus.data.local.FavoriteDao
import com.codesrahul.streamplus.data.local.HistoryDao
import com.codesrahul.streamplus.data.local.PlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN isXtream INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN serverUrl TEXT")
                db.execSQL("ALTER TABLE playlists ADD COLUMN username TEXT")
                db.execSQL("ALTER TABLE playlists ADD COLUMN password TEXT")
            }
        }
        
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "stream_doctor_db"
        ).addMigrations(MIGRATION_11_12, MIGRATION_12_13)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao = database.historyDao()

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideChannelDao(database: AppDatabase): com.codesrahul.streamplus.data.local.ChannelDao = database.channelDao()
}
