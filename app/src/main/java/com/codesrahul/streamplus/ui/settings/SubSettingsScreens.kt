package com.codesrahul.streamplus.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.codesrahul.streamplus.data.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settingsState by viewModel.settings.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())
        ) {
            ListItem(
                headlineContent = { Text("Theme Mode") },
                supportingContent = { Text(settingsState.themeMode.name) },
                modifier = Modifier.clickable { showThemeDialog = true }
            )
            ListItem(
                headlineContent = { Text("Theme Style") },
                supportingContent = { Text(settingsState.themeStyle.name.replace("_", " ")) },
                modifier = Modifier.clickable { showStyleDialog = true }
            )
        }
    }

    if (showThemeDialog) {
        ThemeModeDialog(
            currentMode = settingsState.themeMode,
            onSelect = { viewModel.setThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    if (showStyleDialog) {
        ThemeStyleDialog(
            currentStyle = settingsState.themeStyle,
            onSelect = { viewModel.setThemeStyle(it) },
            onDismiss = { showStyleDialog = false }
        )
    }
}

@Composable
fun ThemeModeDialog(currentMode: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme Mode") },
        text = {
            Column {
                ThemeMode.values().forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (mode == currentMode),
                                onClick = {
                                    onSelect(mode)
                                    onDismiss()
                                }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (mode == currentMode),
                            onClick = null
                        )
                        Text(
                            text = mode.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ThemeStyleDialog(currentStyle: com.codesrahul.streamplus.data.settings.ThemeStyle, onSelect: (com.codesrahul.streamplus.data.settings.ThemeStyle) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme Style") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                com.codesrahul.streamplus.data.settings.ThemeStyle.values().forEach { style ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (style == currentStyle),
                                onClick = {
                                    onSelect(style)
                                    onDismiss()
                                }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (style == currentStyle),
                            onClick = null
                        )
                        Text(
                            text = if (style == com.codesrahul.streamplus.data.settings.ThemeStyle.DYNAMIC) "Dynamic (Material You)" 
                                   else style.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistChannelSettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settingsState by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlist and Channel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SwitchPreference(
                title = "Hide playlist URI",
                subtitle = "To prevent showing the url",
                checked = settingsState.hidePlaylistUri,
                onCheckedChange = { viewModel.setHidePlaylistUri(it) }
            )
            SwitchPreference(
                title = "Hide channel image",
                subtitle = "To prevent the image from being loaded",
                checked = settingsState.hideChannelImage,
                onCheckedChange = { viewModel.setHideChannelImage(it) }
            )
             SwitchPreference(
                title = "Playlist Cache",
                subtitle = "You can load cached playlists",
                checked = settingsState.playlistCache,
                onCheckedChange = { viewModel.setPlaylistCache(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val settingsState by viewModel.settings.collectAsState()
        val context = LocalContext.current
        var showResetDialog by remember { mutableStateOf(false) }
        
        Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
             SwitchPreference(
                title = "Allow non-HTTPS (Cleartext)",
                subtitle = "Enable playback for unencrypted streams",
                checked = settingsState.allowCleartext,
                onCheckedChange = { viewModel.setAllowCleartext(it) }
            )
             SwitchPreference(
                title = "Skip SSL Verification",
                subtitle = "Bypass certificate errors (Use with caution)",
                checked = settingsState.skipSslVerification,
                onCheckedChange = { viewModel.setSkipSslVerification(it) }
            )
             SwitchPreference(
                title = "Videos are always played in landscape mode",
                subtitle = "Directly launch the player in landscape mode",
                checked = settingsState.forceLandscape,
                onCheckedChange = { viewModel.setForceLandscape(it) }
            )
             SwitchPreference(
                title = "Volume gesture control",
                subtitle = "Use gestures to control player volume",
                checked = settingsState.volumeGestures,
                onCheckedChange = { viewModel.setVolumeGestures(it) }
            )
             SwitchPreference(
                title = "Brightness gesture control",
                subtitle = "Use gestures to control player brightness",
                checked = settingsState.brightnessGestures,
                onCheckedChange = { viewModel.setBrightnessGestures(it) }
            )
             SwitchPreference(
                title = "Resume playing",
                subtitle = "Continue playing after interruptions",
                checked = settingsState.resumePlaying,
                onCheckedChange = { viewModel.setResumePlaying(it) }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Button(
                onClick = {
                    try {
                        context.cacheDir.deleteRecursively()
                        android.widget.Toast.makeText(context, "Cache Cleared", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text("Clear Cache")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Text("Factory Reset App")
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Factory Reset") },
                text = { Text("Are you sure you want to reset all data and settings? This action cannot be undone and the app will restart.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.factoryReset()
                        android.widget.Toast.makeText(context, "App Reset Successfully", android.widget.Toast.LENGTH_LONG).show()
                        // Force restart app
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        val mainIntent = android.content.Intent.makeRestartActivityTask(intent?.component)
                        context.startActivity(mainIntent)
                        Runtime.getRuntime().exit(0)
                    }) { Text("Reset") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvanceVideoSettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advance Video & render options") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val settingsState by viewModel.settings.collectAsState(initial = com.codesrahul.streamplus.data.settings.AppSettings())
        Column(modifier = Modifier.padding(padding)) {
            SwitchPreference(
                title = "Enable tunneling",
                subtitle = "Use hardware acceleration for supported devices",
                checked = settingsState.enableTunneling,
                onCheckedChange = { viewModel.setEnableTunneling(it) }
            )
             SwitchPreference(
                title = "Prefer Software Decoder",
                subtitle = "Forces AV decoding in software (fixes some crashes)",
                checked = settingsState.preferSoftwareDecoder,
                onCheckedChange = { viewModel.setPreferSoftwareDecoder(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsSettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
     Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shortcuts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ListItem(
                 headlineContent = { Text("Captioning preferences") },
                 modifier = Modifier.clickable { }
            )
        }
    }
}

@Composable
fun SwitchPreference(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}
