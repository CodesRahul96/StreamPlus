package com.codesrahul.streamplus.di;

import com.codesrahul.streamplus.data.local.AppDatabase;
import com.codesrahul.streamplus.data.local.ChannelDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DatabaseModule_ProvideChannelDaoFactory implements Factory<ChannelDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvideChannelDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ChannelDao get() {
    return provideChannelDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideChannelDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvideChannelDaoFactory(databaseProvider);
  }

  public static ChannelDao provideChannelDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideChannelDao(database));
  }
}
