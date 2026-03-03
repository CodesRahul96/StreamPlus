package com.codesrahul.streamplus.ui.playlist;

import androidx.lifecycle.SavedStateHandle;
import com.codesrahul.streamplus.data.epg.EPGManager;
import com.codesrahul.streamplus.domain.repository.PlaylistRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class PlaylistDetailViewModel_Factory implements Factory<PlaylistDetailViewModel> {
  private final Provider<PlaylistRepository> repositoryProvider;

  private final Provider<EPGManager> epgManagerProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public PlaylistDetailViewModel_Factory(Provider<PlaylistRepository> repositoryProvider,
      Provider<EPGManager> epgManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repositoryProvider = repositoryProvider;
    this.epgManagerProvider = epgManagerProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public PlaylistDetailViewModel get() {
    return newInstance(repositoryProvider.get(), epgManagerProvider.get(), savedStateHandleProvider.get());
  }

  public static PlaylistDetailViewModel_Factory create(
      Provider<PlaylistRepository> repositoryProvider, Provider<EPGManager> epgManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new PlaylistDetailViewModel_Factory(repositoryProvider, epgManagerProvider, savedStateHandleProvider);
  }

  public static PlaylistDetailViewModel newInstance(PlaylistRepository repository,
      EPGManager epgManager, SavedStateHandle savedStateHandle) {
    return new PlaylistDetailViewModel(repository, epgManager, savedStateHandle);
  }
}
