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
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.codesrahul.streamplus.data.settings.ThemeMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material.icons.filled.Sync

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
                title = { Text("Playlist & Channel Management") },
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
                title = "Mask Playlist URI",
                subtitle = "Hide the full playlist URL for privacy",
                checked = settingsState.hidePlaylistUri,
                onCheckedChange = { viewModel.setHidePlaylistUri(it) }
            )
            SwitchPreference(
                title = "Disable Channel Logos",
                subtitle = "Prevent channel icons from loading to save data",
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
                title = "Allow Insecure Streams (Cleartext)",
                subtitle = "Enable playback for non-HTTPS sources",
                checked = settingsState.allowCleartext,
                onCheckedChange = { viewModel.setAllowCleartext(it) }
            )
             SwitchPreference(
                title = "Skip SSL Verification",
                subtitle = "Bypass certificate validation errors (Caution)",
                checked = settingsState.skipSslVerification,
                onCheckedChange = { viewModel.setSkipSslVerification(it) }
            )
             SwitchPreference(
                title = "Force Landscape Mode",
                subtitle = "Always launch player in landscape orientation",
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
    val settingsState by viewModel.settings.collectAsState(initial = com.codesrahul.streamplus.data.settings.AppSettings())
    var showDecoderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playback & Rendering") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            
            ListItem(
                 headlineContent = { Text("Core Playback Configuration") },
                 colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )

            ListItem(
                headlineContent = { Text("Decoder Backend") },
                supportingContent = { 
                    Text(when(settingsState.decoderPriority) {
                        1 -> "Application (Software)"
                        2 -> "Hardware Only (Strict)"
                        else -> "System Default (Hardware)"
                    }) 
                },
                leadingContent = { Icon(Icons.Default.Memory, contentDescription = null) },
                modifier = Modifier.clickable { showDecoderDialog = true }
            )

            SwitchPreference(
                title = "Audio Tunneling",
                subtitle = "Improve A/V sync on supported devices (4K/HDR)",
                checked = settingsState.enableTunneling,
                onCheckedChange = { viewModel.setEnableTunneling(it) }
            )

            SwitchPreference(
                title = "Dolby Vision Support",
                subtitle = "Enable Profile 7 compatibility mode",
                checked = settingsState.dolbyVision,
                onCheckedChange = { viewModel.setDolbyVision(it) }
            )

            SwitchPreference(
                title = "Frame Rate Matching",
                subtitle = "Sync display refresh rate to content (ignore check)",
                checked = settingsState.disableFrameRateCheck,
                onCheckedChange = { viewModel.setDisableFrameRateCheck(it) }
            )
        }
    }

    if (showDecoderDialog) {
        AlertDialog(
            onDismissRequest = { showDecoderDialog = false },
            title = { Text("Decoder Backend") },
            text = {
                Column {
                    val options = listOf(
                        0 to "System Default (Hardware)",
                        1 to "Application (Software)",
                        2 to "Hardware Only (Strict)"
                    )
                    options.forEach { (value, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (value == settingsState.decoderPriority),
                                    onClick = {
                                        viewModel.setDecoderPriority(value)
                                        showDecoderDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (value == settingsState.decoderPriority),
                                onClick = null
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDecoderDialog = false }) { Text("Cancel") }
            }
        )
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
            val context = LocalContext.current
            ListItem(
                 headlineContent = { Text("Captioning Preferences") },
                 modifier = Modifier.clickable { 
                     try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_CAPTIONING_SETTINGS)
                        context.startActivity(intent)
                     } catch (e: Exception) {
                         android.widget.Toast.makeText(context, "Cannot open captioning settings", android.widget.Toast.LENGTH_SHORT).show()
                     }
                 }
            )
        }
    }
}

@Composable
fun SwitchPreference(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}
