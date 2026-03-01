# JNI and Android specific

# Keep data classes and entity names for serialization/deserialization correctly (Room + Gson)
-keepclasseswithmembers class * {
    @androidx.room.Query <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-keep class * extends com.codesrahul.streamplus.data.local.** { *; }
-keep class com.codesrahul.streamplus.data.local.PlaylistEntity { *; }
-keep class com.codesrahul.streamplus.data.local.ChannelEntity { *; }
-keep class com.codesrahul.streamplus.data.local.HistoryEntity { *; }
-keep class com.codesrahul.streamplus.data.local.FavoriteEntity { *; }
-keep class com.codesrahul.streamplus.data.local.SettingsEntity { *; }

# Keep DTOs that might be logged or serialized
-keep class com.codesrahul.streamplus.domain.model.** { *; }
-keep class com.codesrahul.streamplus.data.parser.M3UItem { *; }
-keep class com.codesrahul.streamplus.data.epg.EPGProgram { *; }

# Remove Android Log calls in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Hilt Dependencies
-keep,allowobfuscation,allowshrinking @interface dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class * extends android.app.Application { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
