package com.codesrahul.streamplus.ui.player;

import com.codesrahul.streamplus.data.local.ChannelDao;
import com.codesrahul.streamplus.data.repository.HistoryRepository;
import com.codesrahul.streamplus.data.settings.SettingsRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@QualifierMetadata
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
public final class PlayerActivity_MembersInjector implements MembersInjector<PlayerActivity> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<HistoryRepository> historyRepositoryProvider;

  private final Provider<ChannelDao> channelDaoProvider;

  public PlayerActivity_MembersInjector(Provider<OkHttpClient> okHttpClientProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<HistoryRepository> historyRepositoryProvider,
      Provider<ChannelDao> channelDaoProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.historyRepositoryProvider = historyRepositoryProvider;
    this.channelDaoProvider = channelDaoProvider;
  }

  public static MembersInjector<PlayerActivity> create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<HistoryRepository> historyRepositoryProvider,
      Provider<ChannelDao> channelDaoProvider) {
    return new PlayerActivity_MembersInjector(okHttpClientProvider, settingsRepositoryProvider, historyRepositoryProvider, channelDaoProvider);
  }

  @Override
  public void injectMembers(PlayerActivity instance) {
    injectOkHttpClientProvider(instance, okHttpClientProvider);
    injectSettingsRepository(instance, settingsRepositoryProvider.get());
    injectHistoryRepository(instance, historyRepositoryProvider.get());
    injectChannelDao(instance, channelDaoProvider.get());
  }

  @InjectedFieldSignature("com.codesrahul.streamplus.ui.player.PlayerActivity.okHttpClientProvider")
  public static void injectOkHttpClientProvider(PlayerActivity instance,
      Provider<OkHttpClient> okHttpClientProvider) {
    instance.okHttpClientProvider = okHttpClientProvider;
  }

  @InjectedFieldSignature("com.codesrahul.streamplus.ui.player.PlayerActivity.settingsRepository")
  public static void injectSettingsRepository(PlayerActivity instance,
      SettingsRepository settingsRepository) {
    instance.settingsRepository = settingsRepository;
  }

  @InjectedFieldSignature("com.codesrahul.streamplus.ui.player.PlayerActivity.historyRepository")
  public static void injectHistoryRepository(PlayerActivity instance,
      HistoryRepository historyRepository) {
    instance.historyRepository = historyRepository;
  }

  @InjectedFieldSignature("com.codesrahul.streamplus.ui.player.PlayerActivity.channelDao")
  public static void injectChannelDao(PlayerActivity instance, ChannelDao channelDao) {
    instance.channelDao = channelDao;
  }
}
