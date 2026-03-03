package com.codesrahul.streamplus;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.codesrahul.streamplus.data.epg.EPGManager;
import com.codesrahul.streamplus.data.local.AppDatabase;
import com.codesrahul.streamplus.data.local.ChannelDao;
import com.codesrahul.streamplus.data.local.FavoriteDao;
import com.codesrahul.streamplus.data.local.HistoryDao;
import com.codesrahul.streamplus.data.local.PlaylistDao;
import com.codesrahul.streamplus.data.network.StreamChecker;
import com.codesrahul.streamplus.data.repository.HistoryRepository;
import com.codesrahul.streamplus.data.repository.PlaylistRepositoryImpl;
import com.codesrahul.streamplus.data.settings.SettingsRepository;
import com.codesrahul.streamplus.di.DatabaseModule_ProvideAppDatabaseFactory;
import com.codesrahul.streamplus.di.DatabaseModule_ProvideChannelDaoFactory;
import com.codesrahul.streamplus.di.DatabaseModule_ProvideFavoriteDaoFactory;
import com.codesrahul.streamplus.di.DatabaseModule_ProvideHistoryDaoFactory;
import com.codesrahul.streamplus.di.DatabaseModule_ProvidePlaylistDaoFactory;
import com.codesrahul.streamplus.di.NetworkModule;
import com.codesrahul.streamplus.di.NetworkModule_ProvideOkHttpClientFactory;
import com.codesrahul.streamplus.domain.repository.PlaylistRepository;
import com.codesrahul.streamplus.ui.home.HistoryViewModel;
import com.codesrahul.streamplus.ui.home.HistoryViewModel_HiltModules;
import com.codesrahul.streamplus.ui.local.LocalViewModel;
import com.codesrahul.streamplus.ui.local.LocalViewModel_HiltModules;
import com.codesrahul.streamplus.ui.player.PlayerActivity;
import com.codesrahul.streamplus.ui.player.PlayerActivity_MembersInjector;
import com.codesrahul.streamplus.ui.playlist.PlaylistDetailViewModel;
import com.codesrahul.streamplus.ui.playlist.PlaylistDetailViewModel_HiltModules;
import com.codesrahul.streamplus.ui.playlist.PlaylistViewModel;
import com.codesrahul.streamplus.ui.playlist.PlaylistViewModel_HiltModules;
import com.codesrahul.streamplus.ui.settings.SettingsViewModel;
import com.codesrahul.streamplus.ui.settings.SettingsViewModel_HiltModules;
import com.codesrahul.streamplus.ui.validator.ValidatorViewModel;
import com.codesrahul.streamplus.ui.validator.ValidatorViewModel_HiltModules;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

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
public final class DaggerM3UValidatorApp_HiltComponents_SingletonC {
  private DaggerM3UValidatorApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private NetworkModule networkModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public Builder networkModule(NetworkModule networkModule) {
      this.networkModule = Preconditions.checkNotNull(networkModule);
      return this;
    }

    public M3UValidatorApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      if (networkModule == null) {
        this.networkModule = new NetworkModule();
      }
      return new SingletonCImpl(applicationContextModule, networkModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements M3UValidatorApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public M3UValidatorApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements M3UValidatorApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public M3UValidatorApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements M3UValidatorApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public M3UValidatorApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements M3UValidatorApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public M3UValidatorApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements M3UValidatorApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public M3UValidatorApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements M3UValidatorApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public M3UValidatorApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements M3UValidatorApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public M3UValidatorApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends M3UValidatorApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends M3UValidatorApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends M3UValidatorApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends M3UValidatorApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public void injectPlayerActivity(PlayerActivity playerActivity) {
      injectPlayerActivity2(playerActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(6).put(LazyClassKeyProvider.com_codesrahul_streamplus_ui_home_HistoryViewModel, HistoryViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_codesrahul_streamplus_ui_local_LocalViewModel, LocalViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_codesrahul_streamplus_ui_playlist_PlaylistDetailViewModel, PlaylistDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_codesrahul_streamplus_ui_playlist_PlaylistViewModel, PlaylistViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_codesrahul_streamplus_ui_settings_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_codesrahul_streamplus_ui_validator_ValidatorViewModel, ValidatorViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @CanIgnoreReturnValue
    private PlayerActivity injectPlayerActivity2(PlayerActivity instance) {
      PlayerActivity_MembersInjector.injectOkHttpClientProvider(instance, singletonCImpl.provideOkHttpClientProvider);
      PlayerActivity_MembersInjector.injectSettingsRepository(instance, singletonCImpl.settingsRepositoryProvider.get());
      PlayerActivity_MembersInjector.injectHistoryRepository(instance, singletonCImpl.historyRepositoryProvider.get());
      PlayerActivity_MembersInjector.injectChannelDao(instance, singletonCImpl.channelDao());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_codesrahul_streamplus_ui_validator_ValidatorViewModel = "com.codesrahul.streamplus.ui.validator.ValidatorViewModel";

      static String com_codesrahul_streamplus_ui_playlist_PlaylistDetailViewModel = "com.codesrahul.streamplus.ui.playlist.PlaylistDetailViewModel";

      static String com_codesrahul_streamplus_ui_playlist_PlaylistViewModel = "com.codesrahul.streamplus.ui.playlist.PlaylistViewModel";

      static String com_codesrahul_streamplus_ui_settings_SettingsViewModel = "com.codesrahul.streamplus.ui.settings.SettingsViewModel";

      static String com_codesrahul_streamplus_ui_local_LocalViewModel = "com.codesrahul.streamplus.ui.local.LocalViewModel";

      static String com_codesrahul_streamplus_ui_home_HistoryViewModel = "com.codesrahul.streamplus.ui.home.HistoryViewModel";

      @KeepFieldType
      ValidatorViewModel com_codesrahul_streamplus_ui_validator_ValidatorViewModel2;

      @KeepFieldType
      PlaylistDetailViewModel com_codesrahul_streamplus_ui_playlist_PlaylistDetailViewModel2;

      @KeepFieldType
      PlaylistViewModel com_codesrahul_streamplus_ui_playlist_PlaylistViewModel2;

      @KeepFieldType
      SettingsViewModel com_codesrahul_streamplus_ui_settings_SettingsViewModel2;

      @KeepFieldType
      LocalViewModel com_codesrahul_streamplus_ui_local_LocalViewModel2;

      @KeepFieldType
      HistoryViewModel com_codesrahul_streamplus_ui_home_HistoryViewModel2;
    }
  }

  private static final class ViewModelCImpl extends M3UValidatorApp_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<HistoryViewModel> historyViewModelProvider;

    private Provider<LocalViewModel> localViewModelProvider;

    private Provider<PlaylistDetailViewModel> playlistDetailViewModelProvider;

    private Provider<PlaylistViewModel> playlistViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<ValidatorViewModel> validatorViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.historyViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.localViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.playlistDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.playlistViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.validatorViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(6).put(LazyClassKeyProvider.com_codesrahul_streamplus_ui_home_HistoryViewModel, ((Provider) historyViewModelProvider)).put(LazyClassKeyProvider.com_codesrahul_streamplus_ui_local_LocalViewModel, ((Provider) localViewModelProvider)).put(LazyClassKeyProvider.com_codesrahul_streamplus_ui_playlist_PlaylistDetailViewModel, ((Provider) playlistDetailViewModelProvider)).put(LazyClassKeyProvider.com_codesrahul_streamplus_ui_playlist_PlaylistViewModel, ((Provider) playlistViewModelProvider)).put(LazyClassKeyProvider.com_codesrahul_streamplus_ui_settings_SettingsViewModel, ((Provider) settingsViewModelProvider)).put(LazyClassKeyProvider.com_codesrahul_streamplus_ui_validator_ValidatorViewModel, ((Provider) validatorViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_codesrahul_streamplus_ui_settings_SettingsViewModel = "com.codesrahul.streamplus.ui.settings.SettingsViewModel";

      static String com_codesrahul_streamplus_ui_home_HistoryViewModel = "com.codesrahul.streamplus.ui.home.HistoryViewModel";

      static String com_codesrahul_streamplus_ui_local_LocalViewModel = "com.codesrahul.streamplus.ui.local.LocalViewModel";

      static String com_codesrahul_streamplus_ui_validator_ValidatorViewModel = "com.codesrahul.streamplus.ui.validator.ValidatorViewModel";

      static String com_codesrahul_streamplus_ui_playlist_PlaylistViewModel = "com.codesrahul.streamplus.ui.playlist.PlaylistViewModel";

      static String com_codesrahul_streamplus_ui_playlist_PlaylistDetailViewModel = "com.codesrahul.streamplus.ui.playlist.PlaylistDetailViewModel";

      @KeepFieldType
      SettingsViewModel com_codesrahul_streamplus_ui_settings_SettingsViewModel2;

      @KeepFieldType
      HistoryViewModel com_codesrahul_streamplus_ui_home_HistoryViewModel2;

      @KeepFieldType
      LocalViewModel com_codesrahul_streamplus_ui_local_LocalViewModel2;

      @KeepFieldType
      ValidatorViewModel com_codesrahul_streamplus_ui_validator_ValidatorViewModel2;

      @KeepFieldType
      PlaylistViewModel com_codesrahul_streamplus_ui_playlist_PlaylistViewModel2;

      @KeepFieldType
      PlaylistDetailViewModel com_codesrahul_streamplus_ui_playlist_PlaylistDetailViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.codesrahul.streamplus.ui.home.HistoryViewModel 
          return (T) new HistoryViewModel(singletonCImpl.historyRepositoryProvider.get());

          case 1: // com.codesrahul.streamplus.ui.local.LocalViewModel 
          return (T) new LocalViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.settingsRepositoryProvider.get());

          case 2: // com.codesrahul.streamplus.ui.playlist.PlaylistDetailViewModel 
          return (T) new PlaylistDetailViewModel(singletonCImpl.bindPlaylistRepositoryProvider.get(), singletonCImpl.ePGManagerProvider.get(), viewModelCImpl.savedStateHandle);

          case 3: // com.codesrahul.streamplus.ui.playlist.PlaylistViewModel 
          return (T) new PlaylistViewModel(singletonCImpl.bindPlaylistRepositoryProvider.get(), singletonCImpl.historyRepositoryProvider.get());

          case 4: // com.codesrahul.streamplus.ui.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.settingsRepositoryProvider.get(), singletonCImpl.bindPlaylistRepositoryProvider.get());

          case 5: // com.codesrahul.streamplus.ui.validator.ValidatorViewModel 
          return (T) new ValidatorViewModel(singletonCImpl.bindPlaylistRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends M3UValidatorApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends M3UValidatorApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends M3UValidatorApp_HiltComponents.SingletonC {
    private final NetworkModule networkModule;

    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<SettingsRepository> settingsRepositoryProvider;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<AppDatabase> provideAppDatabaseProvider;

    private Provider<HistoryRepository> historyRepositoryProvider;

    private Provider<EPGManager> ePGManagerProvider;

    private Provider<PlaylistRepositoryImpl> playlistRepositoryImplProvider;

    private Provider<PlaylistRepository> bindPlaylistRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam,
        NetworkModule networkModuleParam) {
      this.networkModule = networkModuleParam;
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam, networkModuleParam);

    }

    private HistoryDao historyDao() {
      return DatabaseModule_ProvideHistoryDaoFactory.provideHistoryDao(provideAppDatabaseProvider.get());
    }

    private ChannelDao channelDao() {
      return DatabaseModule_ProvideChannelDaoFactory.provideChannelDao(provideAppDatabaseProvider.get());
    }

    private StreamChecker streamChecker() {
      return new StreamChecker(provideOkHttpClientProvider.get());
    }

    private PlaylistDao playlistDao() {
      return DatabaseModule_ProvidePlaylistDaoFactory.providePlaylistDao(provideAppDatabaseProvider.get());
    }

    private FavoriteDao favoriteDao() {
      return DatabaseModule_ProvideFavoriteDaoFactory.provideFavoriteDao(provideAppDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam,
        final NetworkModule networkModuleParam) {
      this.settingsRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SettingsRepository>(singletonCImpl, 1));
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 0));
      this.provideAppDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<AppDatabase>(singletonCImpl, 3));
      this.historyRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<HistoryRepository>(singletonCImpl, 2));
      this.ePGManagerProvider = DoubleCheck.provider(new SwitchingProvider<EPGManager>(singletonCImpl, 5));
      this.playlistRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 4);
      this.bindPlaylistRepositoryProvider = DoubleCheck.provider((Provider) playlistRepositoryImplProvider);
    }

    @Override
    public void injectM3UValidatorApp(M3UValidatorApp m3UValidatorApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideOkHttpClientFactory.provideOkHttpClient(singletonCImpl.networkModule, singletonCImpl.settingsRepositoryProvider.get());

          case 1: // com.codesrahul.streamplus.data.settings.SettingsRepository 
          return (T) new SettingsRepository(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.codesrahul.streamplus.data.repository.HistoryRepository 
          return (T) new HistoryRepository(singletonCImpl.historyDao(), singletonCImpl.channelDao());

          case 3: // com.codesrahul.streamplus.data.local.AppDatabase 
          return (T) DatabaseModule_ProvideAppDatabaseFactory.provideAppDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 4: // com.codesrahul.streamplus.data.repository.PlaylistRepositoryImpl 
          return (T) new PlaylistRepositoryImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.streamChecker(), singletonCImpl.playlistDao(), singletonCImpl.favoriteDao(), singletonCImpl.channelDao(), singletonCImpl.historyDao(), singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.settingsRepositoryProvider.get(), singletonCImpl.ePGManagerProvider.get());

          case 5: // com.codesrahul.streamplus.data.epg.EPGManager 
          return (T) new EPGManager();

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
