package com.codesrahul.streamplus.ui.local;

import android.content.Context;
import com.codesrahul.streamplus.data.settings.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class LocalViewModel_Factory implements Factory<LocalViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public LocalViewModel_Factory(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public LocalViewModel get() {
    return newInstance(contextProvider.get(), settingsRepositoryProvider.get());
  }

  public static LocalViewModel_Factory create(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new LocalViewModel_Factory(contextProvider, settingsRepositoryProvider);
  }

  public static LocalViewModel newInstance(Context context, SettingsRepository settingsRepository) {
    return new LocalViewModel(context, settingsRepository);
  }
}
