package com.codesrahul.streamplus.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile PlaylistDao _playlistDao;

  private volatile HistoryDao _historyDao;

  private volatile FavoriteDao _favoriteDao;

  private volatile ChannelDao _channelDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(13) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `userAgent` TEXT NOT NULL, `type` TEXT NOT NULL, `icon` TEXT NOT NULL, `color` INTEGER NOT NULL, `epgUrl` TEXT, `isPinned` INTEGER NOT NULL, `isXtream` INTEGER NOT NULL, `serverUrl` TEXT, `username` TEXT, `password` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `history_table` (`name` TEXT NOT NULL, `group` TEXT NOT NULL, `playlistId` INTEGER NOT NULL, `url` TEXT NOT NULL, `logoUrl` TEXT, `userAgent` TEXT, `headers` TEXT NOT NULL, `cookie` TEXT, `referer` TEXT, `origin` TEXT, `licenseUrl` TEXT, `drmScheme` TEXT, `drmHeaders` TEXT NOT NULL, `lastPosition` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`name`, `group`, `playlistId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`name` TEXT NOT NULL, `groupTitle` TEXT NOT NULL, `playlistId` INTEGER NOT NULL, `url` TEXT NOT NULL, `logoUrl` TEXT NOT NULL, `userAgent` TEXT NOT NULL, `headers` TEXT NOT NULL, `licenseUrl` TEXT, `drmScheme` TEXT, `drmHeaders` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`name`, `groupTitle`, `playlistId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `channels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` INTEGER NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `group` TEXT NOT NULL, `logoUrl` TEXT NOT NULL, `userAgent` TEXT NOT NULL, `headers` TEXT NOT NULL, `licenseUrl` TEXT, `drmScheme` TEXT, `drmHeaders` TEXT NOT NULL, `epgId` TEXT, `isVod` INTEGER NOT NULL, FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_playlistId` ON `channels` (`playlistId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_url` ON `channels` (`url`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '8dca47a0a438608a665d0bda3eaf33f7')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `playlists`");
        db.execSQL("DROP TABLE IF EXISTS `history_table`");
        db.execSQL("DROP TABLE IF EXISTS `favorites`");
        db.execSQL("DROP TABLE IF EXISTS `channels`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsPlaylists = new HashMap<String, TableInfo.Column>(13);
        _columnsPlaylists.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("url", new TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("userAgent", new TableInfo.Column("userAgent", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("icon", new TableInfo.Column("icon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("color", new TableInfo.Column("color", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("epgUrl", new TableInfo.Column("epgUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("isPinned", new TableInfo.Column("isPinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("isXtream", new TableInfo.Column("isXtream", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("serverUrl", new TableInfo.Column("serverUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("username", new TableInfo.Column("username", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("password", new TableInfo.Column("password", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlaylists = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlaylists = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlaylists = new TableInfo("playlists", _columnsPlaylists, _foreignKeysPlaylists, _indicesPlaylists);
        final TableInfo _existingPlaylists = TableInfo.read(db, "playlists");
        if (!_infoPlaylists.equals(_existingPlaylists)) {
          return new RoomOpenHelper.ValidationResult(false, "playlists(com.codesrahul.streamplus.data.local.PlaylistEntity).\n"
                  + " Expected:\n" + _infoPlaylists + "\n"
                  + " Found:\n" + _existingPlaylists);
        }
        final HashMap<String, TableInfo.Column> _columnsHistoryTable = new HashMap<String, TableInfo.Column>(16);
        _columnsHistoryTable.put("name", new TableInfo.Column("name", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("group", new TableInfo.Column("group", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("playlistId", new TableInfo.Column("playlistId", "INTEGER", true, 3, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("url", new TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("logoUrl", new TableInfo.Column("logoUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("userAgent", new TableInfo.Column("userAgent", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("headers", new TableInfo.Column("headers", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("cookie", new TableInfo.Column("cookie", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("referer", new TableInfo.Column("referer", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("origin", new TableInfo.Column("origin", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("licenseUrl", new TableInfo.Column("licenseUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("drmScheme", new TableInfo.Column("drmScheme", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("drmHeaders", new TableInfo.Column("drmHeaders", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("lastPosition", new TableInfo.Column("lastPosition", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistoryTable.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHistoryTable = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHistoryTable = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHistoryTable = new TableInfo("history_table", _columnsHistoryTable, _foreignKeysHistoryTable, _indicesHistoryTable);
        final TableInfo _existingHistoryTable = TableInfo.read(db, "history_table");
        if (!_infoHistoryTable.equals(_existingHistoryTable)) {
          return new RoomOpenHelper.ValidationResult(false, "history_table(com.codesrahul.streamplus.data.local.HistoryEntity).\n"
                  + " Expected:\n" + _infoHistoryTable + "\n"
                  + " Found:\n" + _existingHistoryTable);
        }
        final HashMap<String, TableInfo.Column> _columnsFavorites = new HashMap<String, TableInfo.Column>(11);
        _columnsFavorites.put("name", new TableInfo.Column("name", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavorites.put("groupTitle", new TableInfo.Column("groupTitle", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavorites.put("playlistId", new TableInfo.Column("playlistId", "INTEGER", true, 3, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavorites.put("url", new TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavorites.put("logoUrl", new TableInfo.Column("logoUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavorites.put("userAgent", new TableInfo.Column("userAgent", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavorites.put("headers", new TableInfo.Column("headers", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavorites.put("licenseUrl", new TableInfo.Column("licenseUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavorites.put("drmScheme", new TableInfo.Column("drmScheme", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavorites.put("drmHeaders", new TableInfo.Column("drmHeaders", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavorites.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFavorites = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFavorites = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFavorites = new TableInfo("favorites", _columnsFavorites, _foreignKeysFavorites, _indicesFavorites);
        final TableInfo _existingFavorites = TableInfo.read(db, "favorites");
        if (!_infoFavorites.equals(_existingFavorites)) {
          return new RoomOpenHelper.ValidationResult(false, "favorites(com.codesrahul.streamplus.data.local.FavoriteEntity).\n"
                  + " Expected:\n" + _infoFavorites + "\n"
                  + " Found:\n" + _existingFavorites);
        }
        final HashMap<String, TableInfo.Column> _columnsChannels = new HashMap<String, TableInfo.Column>(13);
        _columnsChannels.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("playlistId", new TableInfo.Column("playlistId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("url", new TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("group", new TableInfo.Column("group", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("logoUrl", new TableInfo.Column("logoUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("userAgent", new TableInfo.Column("userAgent", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("headers", new TableInfo.Column("headers", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("licenseUrl", new TableInfo.Column("licenseUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("drmScheme", new TableInfo.Column("drmScheme", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("drmHeaders", new TableInfo.Column("drmHeaders", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("epgId", new TableInfo.Column("epgId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("isVod", new TableInfo.Column("isVod", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChannels = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysChannels.add(new TableInfo.ForeignKey("playlists", "CASCADE", "NO ACTION", Arrays.asList("playlistId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesChannels = new HashSet<TableInfo.Index>(2);
        _indicesChannels.add(new TableInfo.Index("index_channels_playlistId", false, Arrays.asList("playlistId"), Arrays.asList("ASC")));
        _indicesChannels.add(new TableInfo.Index("index_channels_url", false, Arrays.asList("url"), Arrays.asList("ASC")));
        final TableInfo _infoChannels = new TableInfo("channels", _columnsChannels, _foreignKeysChannels, _indicesChannels);
        final TableInfo _existingChannels = TableInfo.read(db, "channels");
        if (!_infoChannels.equals(_existingChannels)) {
          return new RoomOpenHelper.ValidationResult(false, "channels(com.codesrahul.streamplus.data.local.ChannelEntity).\n"
                  + " Expected:\n" + _infoChannels + "\n"
                  + " Found:\n" + _existingChannels);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "8dca47a0a438608a665d0bda3eaf33f7", "8a78aa6b56e7cb8cd371f661579ca952");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "playlists","history_table","favorites","channels");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `playlists`");
      _db.execSQL("DELETE FROM `history_table`");
      _db.execSQL("DELETE FROM `favorites`");
      _db.execSQL("DELETE FROM `channels`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(PlaylistDao.class, PlaylistDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HistoryDao.class, HistoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FavoriteDao.class, FavoriteDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ChannelDao.class, ChannelDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public PlaylistDao playlistDao() {
    if (_playlistDao != null) {
      return _playlistDao;
    } else {
      synchronized(this) {
        if(_playlistDao == null) {
          _playlistDao = new PlaylistDao_Impl(this);
        }
        return _playlistDao;
      }
    }
  }

  @Override
  public HistoryDao historyDao() {
    if (_historyDao != null) {
      return _historyDao;
    } else {
      synchronized(this) {
        if(_historyDao == null) {
          _historyDao = new HistoryDao_Impl(this);
        }
        return _historyDao;
      }
    }
  }

  @Override
  public FavoriteDao favoriteDao() {
    if (_favoriteDao != null) {
      return _favoriteDao;
    } else {
      synchronized(this) {
        if(_favoriteDao == null) {
          _favoriteDao = new FavoriteDao_Impl(this);
        }
        return _favoriteDao;
      }
    }
  }

  @Override
  public ChannelDao channelDao() {
    if (_channelDao != null) {
      return _channelDao;
    } else {
      synchronized(this) {
        if(_channelDao == null) {
          _channelDao = new ChannelDao_Impl(this);
        }
        return _channelDao;
      }
    }
  }
}
