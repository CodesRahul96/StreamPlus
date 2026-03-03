package com.codesrahul.streamplus.data.repository;

import android.content.Context;
import com.codesrahul.streamplus.data.epg.EPGManager;
import com.codesrahul.streamplus.data.local.ChannelDao;
import com.codesrahul.streamplus.data.local.FavoriteDao;
import com.codesrahul.streamplus.data.local.HistoryDao;
import com.codesrahul.streamplus.data.local.PlaylistDao;
import com.codesrahul.streamplus.data.network.StreamChecker;
import com.codesrahul.streamplus.data.settings.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class PlaylistRepositoryImpl_Factory implements Factory<PlaylistRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<StreamChecker> streamCheckerProvider;

  private final Provider<PlaylistDao> playlistDaoProvider;

  private final Provider<FavoriteDao> favoriteDaoProvider;

  private final Provider<ChannelDao> channelDaoProvider;

  private final Provider<HistoryDao> historyDaoProvider;

  private final Provider<OkHttpClient> clientProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<EPGManager> epgManagerProvider;

  public PlaylistRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<StreamChecker> streamCheckerProvider, Provider<PlaylistDao> playlistDaoProvider,
      Provider<FavoriteDao> favoriteDaoProvider, Provider<ChannelDao> channelDaoProvider,
      Provider<HistoryDao> historyDaoProvider, Provider<OkHttpClient> clientProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<EPGManager> epgManagerProvider) {
    this.contextProvider = contextProvider;
    this.streamCheckerProvider = streamCheckerProvider;
    this.playlistDaoProvider = playlistDaoProvider;
    this.favoriteDaoProvider = favoriteDaoProvider;
    this.channelDaoProvider = channelDaoProvider;
    this.historyDaoProvider = historyDaoProvider;
    this.clientProvider = clientProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.epgManagerProvider = epgManagerProvider;
  }

  @Override
  public PlaylistRepositoryImpl get() {
    return newInstance(contextProvider.get(), streamCheckerProvider.get(), playlistDaoProvider.get(), favoriteDaoProvider.get(), channelDaoProvider.get(), historyDaoProvider.get(), clientProvider.get(), settingsRepositoryProvider.get(), epgManagerProvider.get());
  }

  public static PlaylistRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<StreamChecker> streamCheckerProvider, Provider<PlaylistDao> playlistDaoProvider,
      Provider<FavoriteDao> favoriteDaoProvider, Provider<ChannelDao> channelDaoProvider,
      Provider<HistoryDao> historyDaoProvider, Provider<OkHttpClient> clientProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<EPGManager> epgManagerProvider) {
    return new PlaylistRepositoryImpl_Factory(contextProvider, streamCheckerProvider, playlistDaoProvider, favoriteDaoProvider, channelDaoProvider, historyDaoProvider, clientProvider, settingsRepositoryProvider, epgManagerProvider);
  }

  public static PlaylistRepositoryImpl newInstance(Context context, StreamChecker streamChecker,
      PlaylistDao playlistDao, FavoriteDao favoriteDao, ChannelDao channelDao,
      HistoryDao historyDao, OkHttpClient client, SettingsRepository settingsRepository,
      EPGManager epgManager) {
    return new PlaylistRepositoryImpl(context, streamChecker, playlistDao, favoriteDao, channelDao, historyDao, client, settingsRepository, epgManager);
  }
}
