package com.codesrahul.streamplus.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
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
public final class HistoryDao_Impl implements HistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HistoryEntity> __insertionAdapterOfHistoryEntity;

  private final MapTypeConverter __mapTypeConverter = new MapTypeConverter();

  private final EntityDeletionOrUpdateAdapter<HistoryEntity> __deletionAdapterOfHistoryEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByPlaylistId;

  public HistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHistoryEntity = new EntityInsertionAdapter<HistoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `history_table` (`name`,`group`,`playlistId`,`url`,`logoUrl`,`userAgent`,`headers`,`cookie`,`referer`,`origin`,`licenseUrl`,`drmScheme`,`drmHeaders`,`lastPosition`,`duration`,`timestamp`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HistoryEntity entity) {
        statement.bindString(1, entity.getName());
        statement.bindString(2, entity.getGroup());
        statement.bindLong(3, entity.getPlaylistId());
        statement.bindString(4, entity.getUrl());
        if (entity.getLogoUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getLogoUrl());
        }
        if (entity.getUserAgent() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getUserAgent());
        }
        final String _tmp = __mapTypeConverter.fromMap(entity.getHeaders());
        statement.bindString(7, _tmp);
        if (entity.getCookie() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getCookie());
        }
        if (entity.getReferer() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getReferer());
        }
        if (entity.getOrigin() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getOrigin());
        }
        if (entity.getLicenseUrl() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getLicenseUrl());
        }
        if (entity.getDrmScheme() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getDrmScheme());
        }
        final String _tmp_1 = __mapTypeConverter.fromMap(entity.getDrmHeaders());
        statement.bindString(13, _tmp_1);
        statement.bindLong(14, entity.getLastPosition());
        statement.bindLong(15, entity.getDuration());
        statement.bindLong(16, entity.getTimestamp());
      }
    };
    this.__deletionAdapterOfHistoryEntity = new EntityDeletionOrUpdateAdapter<HistoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `history_table` WHERE `name` = ? AND `group` = ? AND `playlistId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HistoryEntity entity) {
        statement.bindString(1, entity.getName());
        statement.bindString(2, entity.getGroup());
        statement.bindLong(3, entity.getPlaylistId());
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM history_table";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByPlaylistId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM history_table WHERE playlistId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertHistory(final HistoryEntity history,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHistoryEntity.insert(history);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteHistory(final HistoryEntity history,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfHistoryEntity.handle(history);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
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
          __preparedStmtOfClearAll.release(_stmt);
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
  public Flow<List<HistoryEntity>> getAllHistory() {
    final String _sql = "SELECT * FROM history_table ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"history_table"}, new Callable<List<HistoryEntity>>() {
      @Override
      @NonNull
      public List<HistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "group");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfUserAgent = CursorUtil.getColumnIndexOrThrow(_cursor, "userAgent");
          final int _cursorIndexOfHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "headers");
          final int _cursorIndexOfCookie = CursorUtil.getColumnIndexOrThrow(_cursor, "cookie");
          final int _cursorIndexOfReferer = CursorUtil.getColumnIndexOrThrow(_cursor, "referer");
          final int _cursorIndexOfOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "origin");
          final int _cursorIndexOfLicenseUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "licenseUrl");
          final int _cursorIndexOfDrmScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "drmScheme");
          final int _cursorIndexOfDrmHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "drmHeaders");
          final int _cursorIndexOfLastPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPosition");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<HistoryEntity> _result = new ArrayList<HistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HistoryEntity _item;
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpGroup;
            _tmpGroup = _cursor.getString(_cursorIndexOfGroup);
            final int _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getInt(_cursorIndexOfPlaylistId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final String _tmpUserAgent;
            if (_cursor.isNull(_cursorIndexOfUserAgent)) {
              _tmpUserAgent = null;
            } else {
              _tmpUserAgent = _cursor.getString(_cursorIndexOfUserAgent);
            }
            final Map<String, String> _tmpHeaders;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfHeaders);
            _tmpHeaders = __mapTypeConverter.fromString(_tmp);
            final String _tmpCookie;
            if (_cursor.isNull(_cursorIndexOfCookie)) {
              _tmpCookie = null;
            } else {
              _tmpCookie = _cursor.getString(_cursorIndexOfCookie);
            }
            final String _tmpReferer;
            if (_cursor.isNull(_cursorIndexOfReferer)) {
              _tmpReferer = null;
            } else {
              _tmpReferer = _cursor.getString(_cursorIndexOfReferer);
            }
            final String _tmpOrigin;
            if (_cursor.isNull(_cursorIndexOfOrigin)) {
              _tmpOrigin = null;
            } else {
              _tmpOrigin = _cursor.getString(_cursorIndexOfOrigin);
            }
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
            final long _tmpLastPosition;
            _tmpLastPosition = _cursor.getLong(_cursorIndexOfLastPosition);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new HistoryEntity(_tmpName,_tmpGroup,_tmpPlaylistId,_tmpUrl,_tmpLogoUrl,_tmpUserAgent,_tmpHeaders,_tmpCookie,_tmpReferer,_tmpOrigin,_tmpLicenseUrl,_tmpDrmScheme,_tmpDrmHeaders,_tmpLastPosition,_tmpDuration,_tmpTimestamp);
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
  public Object getPosition(final String name, final String group, final int playlistId,
      final Continuation<? super Long> $completion) {
    final String _sql = "SELECT lastPosition FROM history_table WHERE name = ? AND `group` = ? AND playlistId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, name);
    _argIndex = 2;
    _statement.bindString(_argIndex, group);
    _argIndex = 3;
    _statement.bindLong(_argIndex, playlistId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @Nullable
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            if (_cursor.isNull(0)) {
              _result = null;
            } else {
              _result = _cursor.getLong(0);
            }
          } else {
            _result = null;
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
