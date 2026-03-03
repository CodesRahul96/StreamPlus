package com.codesrahul.streamplus.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class FavoriteDao_Impl implements FavoriteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FavoriteEntity> __insertionAdapterOfFavoriteEntity;

  private final MapTypeConverter __mapTypeConverter = new MapTypeConverter();

  private final SharedSQLiteStatement __preparedStmtOfRemoveFavorite;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByPlaylistId;

  public FavoriteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFavoriteEntity = new EntityInsertionAdapter<FavoriteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `favorites` (`name`,`groupTitle`,`playlistId`,`url`,`logoUrl`,`userAgent`,`headers`,`licenseUrl`,`drmScheme`,`drmHeaders`,`addedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FavoriteEntity entity) {
        statement.bindString(1, entity.getName());
        statement.bindString(2, entity.getGroupTitle());
        statement.bindLong(3, entity.getPlaylistId());
        statement.bindString(4, entity.getUrl());
        statement.bindString(5, entity.getLogoUrl());
        statement.bindString(6, entity.getUserAgent());
        final String _tmp = __mapTypeConverter.fromMap(entity.getHeaders());
        statement.bindString(7, _tmp);
        if (entity.getLicenseUrl() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getLicenseUrl());
        }
        if (entity.getDrmScheme() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getDrmScheme());
        }
        final String _tmp_1 = __mapTypeConverter.fromMap(entity.getDrmHeaders());
        statement.bindString(10, _tmp_1);
        statement.bindLong(11, entity.getAddedAt());
      }
    };
    this.__preparedStmtOfRemoveFavorite = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM favorites WHERE name = ? AND groupTitle = ? AND playlistId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByPlaylistId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM favorites WHERE playlistId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object addFavorite(final FavoriteEntity favorite,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFavoriteEntity.insert(favorite);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object removeFavorite(final String name, final String group, final int playlistId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRemoveFavorite.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, name);
        _argIndex = 2;
        _stmt.bindString(_argIndex, group);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, playlistId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfRemoveFavorite.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByPlaylistId(final int playlistId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByPlaylistId.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByPlaylistId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<FavoriteEntity>> getAllFavorites() {
    final String _sql = "SELECT * FROM favorites ORDER BY addedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"favorites"}, new Callable<List<FavoriteEntity>>() {
      @Override
      @NonNull
      public List<FavoriteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfGroupTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "groupTitle");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfUserAgent = CursorUtil.getColumnIndexOrThrow(_cursor, "userAgent");
          final int _cursorIndexOfHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "headers");
          final int _cursorIndexOfLicenseUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "licenseUrl");
          final int _cursorIndexOfDrmScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "drmScheme");
          final int _cursorIndexOfDrmHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "drmHeaders");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<FavoriteEntity> _result = new ArrayList<FavoriteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FavoriteEntity _item;
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpGroupTitle;
            _tmpGroupTitle = _cursor.getString(_cursorIndexOfGroupTitle);
            final int _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getInt(_cursorIndexOfPlaylistId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpLogoUrl;
            _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            final String _tmpUserAgent;
            _tmpUserAgent = _cursor.getString(_cursorIndexOfUserAgent);
            final Map<String, String> _tmpHeaders;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfHeaders);
            _tmpHeaders = __mapTypeConverter.fromString(_tmp);
            final String _tmpLicenseUrl;
            if (_cursor.isNull(_cursorIndexOfLicenseUrl)) {
              _tmpLicenseUrl = null;
            } else {
              _tmpLicenseUrl = _cursor.getString(_cursorIndexOfLicenseUrl);
            }
            final String _tmpDrmScheme;
            if (_cursor.isNull(_cursorIndexOfDrmScheme)) {
              _tmpDrmScheme = null;
            } else {
              _tmpDrmScheme = _cursor.getString(_cursorIndexOfDrmScheme);
            }
            final Map<String, String> _tmpDrmHeaders;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfDrmHeaders);
            _tmpDrmHeaders = __mapTypeConverter.fromString(_tmp_1);
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _item = new FavoriteEntity(_tmpName,_tmpGroupTitle,_tmpPlaylistId,_tmpUrl,_tmpLogoUrl,_tmpUserAgent,_tmpHeaders,_tmpLicenseUrl,_tmpDrmScheme,_tmpDrmHeaders,_tmpAddedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object isFavorite(final String name, final String group, final int playlistId,
      final Continuation<? super Boolean> $completion) {
    final String _sql = "SELECT EXISTS(SELECT 1 FROM favorites WHERE name = ? AND groupTitle = ? AND playlistId = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, name);
    _argIndex = 2;
    _statement.bindString(_argIndex, group);
    _argIndex = 3;
    _statement.bindLong(_argIndex, playlistId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp != 0;
          } else {
            _result = false;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
