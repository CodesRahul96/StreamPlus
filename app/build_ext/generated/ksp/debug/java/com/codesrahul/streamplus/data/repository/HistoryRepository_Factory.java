package com.codesrahul.streamplus.data.repository;

import com.codesrahul.streamplus.data.local.ChannelDao;
import com.codesrahul.streamplus.data.local.HistoryDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class HistoryRepository_Factory implements Factory<HistoryRepository> {
  private final Provider<HistoryDao> historyDaoProvider;

  private final Provider<ChannelDao> channelDaoProvider;

  public HistoryRepository_Factory(Provider<HistoryDao> historyDaoProvider,
      Provider<ChannelDao> channelDaoProvider) {
    this.historyDaoProvider = historyDaoProvider;
    this.channelDaoProvider = channelDaoProvider;
  }

  @Override
  public HistoryRepository get() {
    return newInstance(historyDaoProvider.get(), channelDaoProvider.get());
  }

  public static HistoryRepository_Factory create(Provider<HistoryDao> historyDaoProvider,
      Provider<ChannelDao> channelDaoProvider) {
    return new HistoryRepository_Factory(historyDaoProvider, channelDaoProvider);
  }

  public static HistoryRepository newInstance(HistoryDao historyDao, ChannelDao channelDao) {
    return new HistoryRepository(historyDao, channelDao);
  }
}
