package com.codesrahul.streamplus.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
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

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ChannelDao_Impl implements ChannelDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChannelEntity> __insertionAdapterOfChannelEntity;

  private final MapTypeConverter __mapTypeConverter = new MapTypeConverter();

  private final SharedSQLiteStatement __preparedStmtOfDeleteChannelsByPlaylist;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public ChannelDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChannelEntity = new EntityInsertionAdapter<ChannelEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `channels` (`id`,`playlistId`,`name`,`url`,`group`,`logoUrl`,`userAgent`,`headers`,`licenseUrl`,`drmScheme`,`drmHeaders`,`epgId`,`isVod`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChannelEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getPlaylistId());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getUrl());
        statement.bindString(5, entity.getGroup());
        statement.bindString(6, entity.getLogoUrl());
        statement.bindString(7, entity.getUserAgent());
        final String _tmp = __mapTypeConverter.fromMap(entity.getHeaders());
        statement.bindString(8, _tmp);
        if (entity.getLicenseUrl() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getLicenseUrl());
        }
        if (entity.getDrmScheme() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getDrmScheme());
        }
        final String _tmp_1 = __mapTypeConverter.fromMap(entity.getDrmHeaders());
        statement.bindString(11, _tmp_1);
        if (entity.getEpgId() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getEpgId());
        }
        final int _tmp_2 = entity.isVod() ? 1 : 0;
        statement.bindLong(13, _tmp_2);
      }
    };
    this.__preparedStmtOfDeleteChannelsByPlaylist = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM channels WHERE playlistId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM channels";
        return _query;
      }
    };
  }

  @Override
  public Object insertChannels(final List<ChannelEntity> channels,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChannelEntity.insert(channels);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteChannelsByPlaylist(final int playlistId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteChannelsByPlaylist.acquire();
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
          __preparedStmtOfDeleteChannelsByPlaylist.release(_stmt);
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
  public Object getChannelsByPlaylist(final int playlistId,
      final Continuation<? super List<ChannelEntity>> $completion) {
    final String _sql = "SELECT * FROM channels WHERE playlistId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ChannelEntity>>() {
      @Override
      @NonNull
      public List<ChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "group");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfUserAgent = CursorUtil.getColumnIndexOrThrow(_cursor, "userAgent");
          final int _cursorIndexOfHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "headers");
          final int _cursorIndexOfLicenseUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "licenseUrl");
          final int _cursorIndexOfDrmScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "drmScheme");
          final int _cursorIndexOfDrmHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "drmHeaders");
          final int _cursorIndexOfEpgId = CursorUtil.getColumnIndexOrThrow(_cursor, "epgId");
          final int _cursorIndexOfIsVod = CursorUtil.getColumnIndexOrThrow(_cursor, "isVod");
          final List<ChannelEntity> _result = new ArrayList<ChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChannelEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getInt(_cursorIndexOfPlaylistId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpGroup;
            _tmpGroup = _cursor.getString(_cursorIndexOfGroup);
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
            final String _tmpEpgId;
            if (_cursor.isNull(_cursorIndexOfEpgId)) {
              _tmpEpgId = null;
            } else {
              _tmpEpgId = _cursor.getString(_cursorIndexOfEpgId);
            }
            final boolean _tmpIsVod;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsVod);
            _tmpIsVod = _tmp_2 != 0;
            _item = new ChannelEntity(_tmpId,_tmpPlaylistId,_tmpName,_tmpUrl,_tmpGroup,_tmpLogoUrl,_tmpUserAgent,_tmpHeaders,_tmpLicenseUrl,_tmpDrmScheme,_tmpDrmHeaders,_tmpEpgId,_tmpIsVod);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getChannelByIdentity(final String name, final String group, final int playlistId,
      final Continuation<? super ChannelEntity> $completion) {
    final String _sql = "SELECT * FROM channels WHERE name = ? AND `group` = ? AND playlistId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, name);
    _argIndex = 2;
    _statement.bindString(_argIndex, group);
    _argIndex = 3;
    _statement.bindLong(_argIndex, playlistId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ChannelEntity>() {
      @Override
      @Nullable
      public ChannelEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "group");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfUserAgent = CursorUtil.getColumnIndexOrThrow(_cursor, "userAgent");
          final int _cursorIndexOfHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "headers");
          final int _cursorIndexOfLicenseUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "licenseUrl");
          final int _cursorIndexOfDrmScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "drmScheme");
          final int _cursorIndexOfDrmHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "drmHeaders");
          final int _cursorIndexOfEpgId = CursorUtil.getColumnIndexOrThrow(_cursor, "epgId");
          final int _cursorIndexOfIsVod = CursorUtil.getColumnIndexOrThrow(_cursor, "isVod");
          final ChannelEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getInt(_cursorIndexOfPlaylistId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpGroup;
            _tmpGroup = _cursor.getString(_cursorIndexOfGroup);
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
            final String _tmpEpgId;
            if (_cursor.isNull(_cursorIndexOfEpgId)) {
              _tmpEpgId = null;
            } else {
              _tmpEpgId = _cursor.getString(_cursorIndexOfEpgId);
            }
            final boolean _tmpIsVod;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsVod);
            _tmpIsVod = _tmp_2 != 0;
            _result = new ChannelEntity(_tmpId,_tmpPlaylistId,_tmpName,_tmpUrl,_tmpGroup,_tmpLogoUrl,_tmpUserAgent,_tmpHeaders,_tmpLicenseUrl,_tmpDrmScheme,_tmpDrmHeaders,_tmpEpgId,_tmpIsVod);
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

  @Override
  public Object searchChannels(final String query,
      final Continuation<? super List<ChannelEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM channels \n"
            + "        WHERE name LIKE '%' || ? || '%' \n"
            + "        OR `group` LIKE '%' || ? || '%' \n"
            + "        LIMIT 100\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ChannelEntity>>() {
      @Override
      @NonNull
      public List<ChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "group");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfUserAgent = CursorUtil.getColumnIndexOrThrow(_cursor, "userAgent");
          final int _cursorIndexOfHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "headers");
          final int _cursorIndexOfLicenseUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "licenseUrl");
          final int _cursorIndexOfDrmScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "drmScheme");
          final int _cursorIndexOfDrmHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "drmHeaders");
          final int _cursorIndexOfEpgId = CursorUtil.getColumnIndexOrThrow(_cursor, "epgId");
          final int _cursorIndexOfIsVod = CursorUtil.getColumnIndexOrThrow(_cursor, "isVod");
          final List<ChannelEntity> _result = new ArrayList<ChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChannelEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getInt(_cursorIndexOfPlaylistId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpGroup;
            _tmpGroup = _cursor.getString(_cursorIndexOfGroup);
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
            final String _tmpEpgId;
            if (_cursor.isNull(_cursorIndexOfEpgId)) {
              _tmpEpgId = null;
            } else {
              _tmpEpgId = _cursor.getString(_cursorIndexOfEpgId);
            }
            final boolean _tmpIsVod;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsVod);
            _tmpIsVod = _tmp_2 != 0;
            _item = new ChannelEntity(_tmpId,_tmpPlaylistId,_tmpName,_tmpUrl,_tmpGroup,_tmpLogoUrl,_tmpUserAgent,_tmpHeaders,_tmpLicenseUrl,_tmpDrmScheme,_tmpDrmHeaders,_tmpEpgId,_tmpIsVod);
            _result.add(_item);
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
