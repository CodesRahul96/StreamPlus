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
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PlaylistDao_Impl implements PlaylistDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PlaylistEntity> __insertionAdapterOfPlaylistEntity;

  private final EntityDeletionOrUpdateAdapter<PlaylistEntity> __deletionAdapterOfPlaylistEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateEpgUrl;

  public PlaylistDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPlaylistEntity = new EntityInsertionAdapter<PlaylistEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `playlists` (`id`,`name`,`url`,`userAgent`,`type`,`icon`,`color`,`epgUrl`,`isPinned`,`isXtream`,`serverUrl`,`username`,`password`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlaylistEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getUrl());
        statement.bindString(4, entity.getUserAgent());
        statement.bindString(5, entity.getType());
        statement.bindString(6, entity.getIcon());
        statement.bindLong(7, entity.getColor());
        if (entity.getEpgUrl() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getEpgUrl());
        }
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.isXtream() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        if (entity.getServerUrl() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getServerUrl());
        }
        if (entity.getUsername() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getUsername());
        }
        if (entity.getPassword() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getPassword());
        }
      }
    };
    this.__deletionAdapterOfPlaylistEntity = new EntityDeletionOrUpdateAdapter<PlaylistEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `playlists` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlaylistEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateEpgUrl = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE playlists SET epgUrl = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertPlaylist(final PlaylistEntity playlist,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlaylistEntity.insert(playlist);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePlaylist(final PlaylistEntity playlist,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPlaylistEntity.handle(playlist);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateEpgUrl(final int id, final String epgUrl,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateEpgUrl.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, epgUrl);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfUpdateEpgUrl.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PlaylistEntity>> getAllPlaylists() {
    final String _sql = "SELECT * FROM playlists ORDER BY isPinned DESC, id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"playlists"}, new Callable<List<PlaylistEntity>>() {
      @Override
      @NonNull
      public List<PlaylistEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfUserAgent = CursorUtil.getColumnIndexOrThrow(_cursor, "userAgent");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfEpgUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "epgUrl");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsXtream = CursorUtil.getColumnIndexOrThrow(_cursor, "isXtream");
          final int _cursorIndexOfServerUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "serverUrl");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final List<PlaylistEntity> _result = new ArrayList<PlaylistEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlaylistEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpUserAgent;
            _tmpUserAgent = _cursor.getString(_cursorIndexOfUserAgent);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpIcon;
            _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final String _tmpEpgUrl;
            if (_cursor.isNull(_cursorIndexOfEpgUrl)) {
              _tmpEpgUrl = null;
            } else {
              _tmpEpgUrl = _cursor.getString(_cursorIndexOfEpgUrl);
            }
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsXtream;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsXtream);
            _tmpIsXtream = _tmp_1 != 0;
            final String _tmpServerUrl;
            if (_cursor.isNull(_cursorIndexOfServerUrl)) {
              _tmpServerUrl = null;
            } else {
              _tmpServerUrl = _cursor.getString(_cursorIndexOfServerUrl);
            }
            final String _tmpUsername;
            if (_cursor.isNull(_cursorIndexOfUsername)) {
              _tmpUsername = null;
            } else {
              _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            }
            final String _tmpPassword;
            if (_cursor.isNull(_cursorIndexOfPassword)) {
              _tmpPassword = null;
            } else {
              _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            }
            _item = new PlaylistEntity(_tmpId,_tmpName,_tmpUrl,_tmpUserAgent,_tmpType,_tmpIcon,_tmpColor,_tmpEpgUrl,_tmpIsPinned,_tmpIsXtream,_tmpServerUrl,_tmpUsername,_tmpPassword);
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
  public Object getAllPlaylistsList(final Continuation<? super List<PlaylistEntity>> $completion) {
    final String _sql = "SELECT * FROM playlists ORDER BY isPinned DESC, id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PlaylistEntity>>() {
      @Override
      @NonNull
      public List<PlaylistEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfUserAgent = CursorUtil.getColumnIndexOrThrow(_cursor, "userAgent");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfEpgUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "epgUrl");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsXtream = CursorUtil.getColumnIndexOrThrow(_cursor, "isXtream");
          final int _cursorIndexOfServerUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "serverUrl");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final List<PlaylistEntity> _result = new ArrayList<PlaylistEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlaylistEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpUserAgent;
            _tmpUserAgent = _cursor.getString(_cursorIndexOfUserAgent);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpIcon;
            _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final String _tmpEpgUrl;
            if (_cursor.isNull(_cursorIndexOfEpgUrl)) {
              _tmpEpgUrl = null;
            } else {
              _tmpEpgUrl = _cursor.getString(_cursorIndexOfEpgUrl);
            }
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsXtream;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsXtream);
            _tmpIsXtream = _tmp_1 != 0;
            final String _tmpServerUrl;
            if (_cursor.isNull(_cursorIndexOfServerUrl)) {
              _tmpServerUrl = null;
            } else {
              _tmpServerUrl = _cursor.getString(_cursorIndexOfServerUrl);
            }
            final String _tmpUsername;
            if (_cursor.isNull(_cursorIndexOfUsername)) {
              _tmpUsername = null;
            } else {
              _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            }
            final String _tmpPassword;
            if (_cursor.isNull(_cursorIndexOfPassword)) {
              _tmpPassword = null;
            } else {
              _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            }
            _item = new PlaylistEntity(_tmpId,_tmpName,_tmpUrl,_tmpUserAgent,_tmpType,_tmpIcon,_tmpColor,_tmpEpgUrl,_tmpIsPinned,_tmpIsXtream,_tmpServerUrl,_tmpUsername,_tmpPassword);
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
  public Flow<PlaylistEntity> getPlaylist(final int id) {
    final String _sql = "SELECT * FROM playlists WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"playlists"}, new Callable<PlaylistEntity>() {
      @Override
      @Nullable
      public PlaylistEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfUserAgent = CursorUtil.getColumnIndexOrThrow(_cursor, "userAgent");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfEpgUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "epgUrl");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsXtream = CursorUtil.getColumnIndexOrThrow(_cursor, "isXtream");
          final int _cursorIndexOfServerUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "serverUrl");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final PlaylistEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpUserAgent;
            _tmpUserAgent = _cursor.getString(_cursorIndexOfUserAgent);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpIcon;
            _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final String _tmpEpgUrl;
            if (_cursor.isNull(_cursorIndexOfEpgUrl)) {
              _tmpEpgUrl = null;
            } else {
              _tmpEpgUrl = _cursor.getString(_cursorIndexOfEpgUrl);
            }
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsXtream;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsXtream);
            _tmpIsXtream = _tmp_1 != 0;
            final String _tmpServerUrl;
            if (_cursor.isNull(_cursorIndexOfServerUrl)) {
              _tmpServerUrl = null;
            } else {
              _tmpServerUrl = _cursor.getString(_cursorIndexOfServerUrl);
            }
            final String _tmpUsername;
            if (_cursor.isNull(_cursorIndexOfUsername)) {
              _tmpUsername = null;
            } else {
              _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            }
            final String _tmpPassword;
            if (_cursor.isNull(_cursorIndexOfPassword)) {
              _tmpPassword = null;
            } else {
              _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            }
            _result = new PlaylistEntity(_tmpId,_tmpName,_tmpUrl,_tmpUserAgent,_tmpType,_tmpIcon,_tmpColor,_tmpEpgUrl,_tmpIsPinned,_tmpIsXtream,_tmpServerUrl,_tmpUsername,_tmpPassword);
          } else {
            _result = null;
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
