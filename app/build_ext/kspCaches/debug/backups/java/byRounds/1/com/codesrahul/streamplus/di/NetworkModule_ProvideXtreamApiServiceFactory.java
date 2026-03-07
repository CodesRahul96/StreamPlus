package com.codesrahul.streamplus.di;

import com.codesrahul.streamplus.data.remote.XtreamApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideXtreamApiServiceFactory implements Factory<XtreamApiService> {
  private final NetworkModule module;

  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideXtreamApiServiceFactory(NetworkModule module,
      Provider<Retrofit> retrofitProvider) {
    this.module = module;
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public XtreamApiService get() {
    return provideXtreamApiService(module, retrofitProvider.get());
  }

  public static NetworkModule_ProvideXtreamApiServiceFactory create(NetworkModule module,
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideXtreamApiServiceFactory(module, retrofitProvider);
  }

  public static XtreamApiService provideXtreamApiService(NetworkModule instance,
      Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(instance.provideXtreamApiService(retrofit));
  }
}
