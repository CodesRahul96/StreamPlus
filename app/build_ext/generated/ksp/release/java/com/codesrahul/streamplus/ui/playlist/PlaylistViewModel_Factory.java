package com.codesrahul.streamplus.ui.playlist;

import com.codesrahul.streamplus.data.repository.HistoryRepository;
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
public final class PlaylistViewModel_Factory implements Factory<PlaylistViewModel> {
  private final Provider<PlaylistRepository> repositoryProvider;

  private final Provider<HistoryRepository> historyRepositoryProvider;

  public PlaylistViewModel_Factory(Provider<PlaylistRepository> repositoryProvider,
      Provider<HistoryRepository> historyRepositoryProvider) {
    this.repositoryProvider = repositoryProvider;
    this.historyRepositoryProvider = historyRepositoryProvider;
  }

  @Override
  public PlaylistViewModel get() {
    return newInstance(repositoryProvider.get(), historyRepositoryProvider.get());
  }

  public static PlaylistViewModel_Factory create(Provider<PlaylistRepository> repositoryProvider,
      Provider<HistoryRepository> historyRepositoryProvider) {
    return new PlaylistViewModel_Factory(repositoryProvider, historyRepositoryProvider);
  }

  public static PlaylistViewModel newInstance(PlaylistRepository repository,
      HistoryRepository historyRepository) {
    return new PlaylistViewModel(repository, historyRepository);
  }
}
