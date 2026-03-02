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

                com.codesrahul.streamplus.ui.theme.StreamPlusTheme(
                    themeMode = settingsState.themeMode
                ) {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val startTab = intent.getStringExtra("START_TAB")
                        com.codesrahul.streamplus.ui.main.MainScreen(initialTab = startTab)
                    }
                }
        }
    }
}
