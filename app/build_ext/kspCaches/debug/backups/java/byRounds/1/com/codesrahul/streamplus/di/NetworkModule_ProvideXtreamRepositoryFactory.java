package com.codesrahul.streamplus.di;

import com.codesrahul.streamplus.data.remote.XtreamApiService;
import com.codesrahul.streamplus.domain.repository.XtreamRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class NetworkModule_ProvideXtreamRepositoryFactory implements Factory<XtreamRepository> {
  private final NetworkModule module;

  private final Provider<XtreamApiService> apiServiceProvider;

  public NetworkModule_ProvideXtreamRepositoryFactory(NetworkModule module,
      Provider<XtreamApiService> apiServiceProvider) {
    this.module = module;
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public XtreamRepository get() {
    return provideXtreamRepository(module, apiServiceProvider.get());
  }

  public static NetworkModule_ProvideXtreamRepositoryFactory create(NetworkModule module,
      Provider<XtreamApiService> apiServiceProvider) {
    return new NetworkModule_ProvideXtreamRepositoryFactory(module, apiServiceProvider);
  }

  public static XtreamRepository provideXtreamRepository(NetworkModule instance,
      XtreamApiService apiService) {
    return Preconditions.checkNotNullFromProvides(instance.provideXtreamRepository(apiService));
  }
}
