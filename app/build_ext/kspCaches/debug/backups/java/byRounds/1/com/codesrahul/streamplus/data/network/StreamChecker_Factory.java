package com.codesrahul.streamplus.data.network;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class StreamChecker_Factory implements Factory<StreamChecker> {
  private final Provider<OkHttpClient> clientProvider;

  public StreamChecker_Factory(Provider<OkHttpClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public StreamChecker get() {
    return newInstance(clientProvider.get());
  }

  public static StreamChecker_Factory create(Provider<OkHttpClient> clientProvider) {
    return new StreamChecker_Factory(clientProvider);
  }

  public static StreamChecker newInstance(OkHttpClient client) {
    return new StreamChecker(client);
  }
}
