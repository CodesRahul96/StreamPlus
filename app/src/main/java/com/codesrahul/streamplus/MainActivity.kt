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
import androidx.hilt.navigation.compose.hiltViewModel
import com.codesrahul.streamplus.ui.settings.SettingsViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
                val viewModel: SettingsViewModel = hiltViewModel()
                val settingsState by viewModel.settings.collectAsState(initial = com.codesrahul.streamplus.data.settings.AppSettings())

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
                        // A surface container using the 'background' color from the theme
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            if (!settingsState.isLoaded) {
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
