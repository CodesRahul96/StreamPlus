package com.codesrahul.streamplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.codesrahul.streamplus.ui.settings.SettingsViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        val isTv = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK) ||
                   (getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager)?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        if (!isTv && requestedOrientation != android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.codesrahul.streamplus.util.RefreshRateHelper.enableHighRefreshRate(this)
        setContent {
                 val viewModel: SettingsViewModel = hiltViewModel()
                 val settingsState by viewModel.settings.collectAsState(initial = com.codesrahul.streamplus.data.settings.AppSettings())
                 val playlistViewModel: com.codesrahul.streamplus.ui.playlist.PlaylistViewModel = hiltViewModel()
                 val playlistsState by playlistViewModel.playlists.collectAsState()
 
                 val context = androidx.compose.ui.platform.LocalContext.current
                 val locale = androidx.compose.runtime.remember(settingsState.languageCode) { java.util.Locale(settingsState.languageCode) }
 
                 val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                 val localContext = androidx.compose.runtime.remember(locale) {
                     val config = android.content.res.Configuration(configuration).apply {
                         setLocale(locale)
                     }
                     val configContext = context.createConfigurationContext(config)
                     LocalizedContext(context, configContext)
                 }
 
                 androidx.compose.runtime.CompositionLocalProvider(
                     androidx.compose.ui.platform.LocalContext provides localContext
                 ) {
                     com.codesrahul.streamplus.ui.theme.StreamPlusTheme(
                         themeMode = settingsState.themeMode
                     ) {
                         Surface(
                             modifier = Modifier.fillMaxSize(),
                             color = MaterialTheme.colorScheme.background
                         ) {
                             var hasLoadedOnce by androidx.compose.runtime.saveable.rememberSaveable {
                                 androidx.compose.runtime.mutableStateOf(false)
                             }
                             if (settingsState.isLoaded && playlistsState != null) {
                                 hasLoadedOnce = true
                             }

                             if (!hasLoadedOnce) {
                                 // Render empty box to prevent MainScreen NavHost flash
                                 androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize())
                             } else {
                                 val startTab = intent.getStringExtra("START_TAB")
                                 com.codesrahul.streamplus.ui.main.MainScreen(initialTab = startTab)
                             }
                         }
                     }
                 }
        }
    }
}

class LocalizedContext(
    base: android.content.Context,
    private val localizedContext: android.content.Context
) : android.content.ContextWrapper(base) {
    override fun getResources(): android.content.res.Resources {
        return localizedContext.resources
    }
    override fun getAssets(): android.content.res.AssetManager {
        return localizedContext.assets
    }
}
