package com.codesrahul.streamplus.di;

import com.codesrahul.streamplus.data.settings.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class NetworkModule_ProvideOkHttpClientFactory implements Factory<OkHttpClient> {
  private final NetworkModule module;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public NetworkModule_ProvideOkHttpClientFactory(NetworkModule module,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.module = module;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideOkHttpClient(module, settingsRepositoryProvider.get());
  }

  public static NetworkModule_ProvideOkHttpClientFactory create(NetworkModule module,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new NetworkModule_ProvideOkHttpClientFactory(module, settingsRepositoryProvider);
  }

  public static OkHttpClient provideOkHttpClient(NetworkModule instance,
      SettingsRepository settingsRepository) {
    return Preconditions.checkNotNullFromProvides(instance.provideOkHttpClient(settingsRepository));
  }
}
