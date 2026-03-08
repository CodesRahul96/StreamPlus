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
        
        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Wipe stale cached items to force the app to generate new mathematically correct stream URLs
                try {
                    db.execSQL("DELETE FROM channels")
                    db.execSQL("DELETE FROM history")
                    db.execSQL("DELETE FROM favorites")
                } catch (e: Exception) {
                    android.util.Log.e("DatabaseModule", "Failed to clear cache tables internally", e)
                }
            }
        }
        
        val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Wipe stale cached items to force the app to generate new mathematically correct stream URLs
                try {
                    db.execSQL("DELETE FROM channels")
                    db.execSQL("DELETE FROM history")
                    db.execSQL("DELETE FROM favorites")
                } catch (e: Exception) {
                    android.util.Log.e("DatabaseModule", "Failed to clear cache tables internally", e)
                }
            }
        }
        
        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Wipe stale cached items to force the app to generate new mathematically correct stream URLs
                try {
                    db.execSQL("DELETE FROM channels")
                    db.execSQL("DELETE FROM history")
                    db.execSQL("DELETE FROM favorites")
                } catch (e: Exception) {
                    android.util.Log.e("DatabaseModule", "Failed to clear cache tables internally", e)
                }
            }
        }
        
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "stream_doctor_db"
        ).addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
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
