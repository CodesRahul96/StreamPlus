package com.codesrahul.streamplus.data.epg;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class EPGManager_Factory implements Factory<EPGManager> {
  @Override
  public EPGManager get() {
    return newInstance();
  }

  public static EPGManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static EPGManager newInstance() {
    return new EPGManager();
  }

  private static final class InstanceHolder {
    private static final EPGManager_Factory INSTANCE = new EPGManager_Factory();
  }
}
