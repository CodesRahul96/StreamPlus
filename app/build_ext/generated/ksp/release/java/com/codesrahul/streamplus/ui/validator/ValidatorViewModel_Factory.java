package com.codesrahul.streamplus.ui.validator;

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
public final class ValidatorViewModel_Factory implements Factory<ValidatorViewModel> {
  private final Provider<PlaylistRepository> repositoryProvider;

  public ValidatorViewModel_Factory(Provider<PlaylistRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ValidatorViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static ValidatorViewModel_Factory create(Provider<PlaylistRepository> repositoryProvider) {
    return new ValidatorViewModel_Factory(repositoryProvider);
  }

  public static ValidatorViewModel newInstance(PlaylistRepository repository) {
    return new ValidatorViewModel(repository);
  }
}
