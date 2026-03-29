package com.codesrahul.streamplus.ui.settings
 
import com.codesrahul.streamplus.data.settings.ThemeMode
import com.codesrahul.streamplus.data.settings.PlayerOrientation

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TopAppBarDefaults
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settingsState by viewModel.settings.collectAsState()
    
    com.codesrahul.streamplus.ui.components.PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Appearance", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Theme Mode Section
                SettingsCard {
                    SettingsItemHeader(
                        title = "Theme Mode",
                        subtitle = "Choose your preferred theme appearance",
                        icon = Icons.Default.BrightnessMedium
                    )
                    
                    SegmentedButton(
                        options = ThemeMode.values().toList(),
                        selectedOption = settingsState.themeMode,
                        onOptionSelected = { viewModel.setThemeMode(it) },
                        labelProvider = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() }.replace("_", " ") },
                        iconProvider = { mode ->
                            Icon(
                                imageVector = when(mode) {
                                    ThemeMode.SYSTEM -> Icons.Default.SettingsSuggest
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                    ThemeMode.PITCH_BLACK -> Icons.Default.NightsStay
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when(settingsState.themeMode) {
                            ThemeMode.SYSTEM -> "Follows device"
                            ThemeMode.LIGHT -> "Always Light"
                            ThemeMode.DARK -> "Always Dark"
                            ThemeMode.PITCH_BLACK -> "Pitch Black OLED"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }


                // UI Customization Section
                SettingsCard {
                    SettingsItemHeader(
                        title = "Hide Navigation Labels",
                        icon = Icons.Default.LabelOff
                    )
                    
                    SettingsSwitchItem(
                        title = "Bottom navigation labels are ${if (settingsState.hideNavLabels) "hidden" else "visible"}",
                        checked = settingsState.hideNavLabels,
                        onCheckedChange = { viewModel.setHideNavLabels(it) },
                        showDivider = false
                    )
                }

                // Default Tab Section
                SettingsCard {
                    SettingsItemHeader(
                        title = "Default Tab",
                        subtitle = "Choose which tab opens when app starts",
                        icon = Icons.Default.Home
                    )
                    
                    val tabs = listOf("playlist" to "Playlist", "local" to "Gallery", "direct_play" to "Stream")
                    
                    SegmentedButton(
                        options = tabs,
                        selectedOption = tabs.find { it.first == settingsState.defaultTab } ?: tabs[0],
                        onOptionSelected = { viewModel.setDefaultTab(it.first) },
                        labelProvider = { it.second },
                        iconProvider = { tab ->
                            Icon(
                                imageVector = when(tab.first) {
                                    "playlist" -> Icons.Default.PlaylistPlay
                                    "local" -> Icons.Default.VideoLibrary
                                    "direct_play" -> Icons.Default.Stream
                                    else -> Icons.Default.Home
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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
                            text = mode.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " "),
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showIconDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showExportImportDialog by remember { mutableStateOf(false) }

    // QR Share state
    var qrUrlInput by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val playlists = viewModel.getAllPlaylistsOnce()
                val jsonArray = JSONArray()
                playlists.forEach { pl ->
                    val obj = JSONObject().apply {
                        put("name", pl.name)
                        put("url", pl.url)
                        put("userAgent", pl.userAgent)
                        put("icon", pl.icon)
                        put("color", pl.color)
                    }
                    jsonArray.put(obj)
                }
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonArray.toString(2).toByteArray())
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "✅ Exported ${playlists.size} playlist(s)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        showExportImportDialog = false
    }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                val jsonArray = JSONArray(raw)
                var count = 0
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    viewModel.importPlaylist(
                        name = obj.optString("name", "Playlist $i"),
                        url = obj.optString("url", ""),
                        userAgent = obj.optString("userAgent", "Auto"),
                        icon = obj.optString("icon", "default"),
                        color = obj.optInt("color", -16777216)
                    )
                    count++
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "✅ Imported $count playlist(s)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        showExportImportDialog = false
    }

    com.codesrahul.streamplus.ui.components.PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Playlist", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                SettingsCard {
                    SettingsClickableItem(
                        title = "Default Playlist Icon",
                        subtitle = settingsState.defaultPlaylistIcon,
                        icon = Icons.Default.List,
                        onClick = { showIconDialog = true },
                        showDivider = true
                    )
                    
                    SettingsClickableItem(
                        title = "Sort Order",
                        subtitle = settingsState.playlistSortOrder,
                        icon = Icons.Default.SortByAlpha,
                        onClick = { showSortDialog = true },
                        showDivider = true
                    )
                    
                    SettingsSwitchItem(
                        title = "Mask Playlist URLs",
                        subtitle = "Hide the actual URL for security & privacy",
                        icon = Icons.Default.VpnKey,
                        checked = settingsState.hidePlaylistUri,
                        onCheckedChange = { viewModel.setHidePlaylistUri(it) },
                        showDivider = true
                    )
                    
                    SettingsClickableItem(
                        title = "QR Share",
                        subtitle = "Generate a QR code for any playlist URL",
                        icon = Icons.Default.QrCode,
                        onClick = { showQrDialog = true },
                        showDivider = true
                    )
                    
                    SettingsClickableItem(
                        title = "Import & Export",
                        subtitle = "Backup or restore your playlists data",
                        icon = Icons.Default.Backup,
                        onClick = { showExportImportDialog = true },
                        showDivider = false
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ── QR Share Dialog ────────────────────────────────────────────────────────
    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false; qrBitmap = null },
            title = { Text("QR Share") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = qrUrlInput,
                        onValueChange = { qrUrlInput = it; qrBitmap = null },
                        label = { Text("Playlist URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    qrBitmap?.let { bmp ->
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Image(
                                painter = androidx.compose.ui.graphics.painter.BitmapPainter(bmp.asImageBitmap()),
                                contentDescription = "QR Code",
                                modifier = Modifier.size(220.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // Share QR image via system sheet
                        Button(
                            onClick = {
                                try {
                                    val file = File(context.cacheDir, "streamplus_qr.png")
                                    FileOutputStream(file).use { fos -> bmp.compress(Bitmap.CompressFormat.PNG, 100, fos) }
                                    val shareUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        putExtra(Intent.EXTRA_TEXT, qrUrlInput)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Playlist QR"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share QR Image")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val url = qrUrlInput.trim()
                        if (url.isBlank()) {
                            Toast.makeText(context, "Please enter a URL", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        try {
                            val size = 512
                            val bitMatrix = MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, size, size)
                            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
                            for (x in 0 until size) {
                                for (y in 0 until size) {
                                    bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                                }
                            }
                            qrBitmap = bmp
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to generate QR: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Generate") }
            },
            dismissButton = {
                TextButton(onClick = { showQrDialog = false; qrBitmap = null }) { Text("Close") }
            }
        )
    }

    // ── Import & Export Dialog ─────────────────────────────────────────────────
    if (showExportImportDialog) {
        AlertDialog(
            onDismissRequest = { showExportImportDialog = false },
            title = { Text("Import & Export") },
            text = {
                Column {
                    Text(
                        "Export saves all your playlists to a JSON file.\nImport loads playlists from a previously exported file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            exportLauncher.launch("streamplus_playlists_backup.json")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Playlists")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { importLauncher.launch("application/json") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Playlists")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportImportDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Icon & Sort dialogs ────────────────────────────────────────────────────
    if (showIconDialog) {
        val options = listOf("List", "Grid", "Card", "Compact")
        AlertDialog(
            onDismissRequest = { showIconDialog = false },
            title = { Text("Default Playlist Icon") },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (option == settingsState.defaultPlaylistIcon),
                                    onClick = {
                                        viewModel.setDefaultPlaylistIcon(option)
                                        showIconDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == settingsState.defaultPlaylistIcon),
                                onClick = null
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSortDialog) {
        val options = listOf("Name", "Added Date", "Category", "Last Watched")
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort Order") },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (option == settingsState.playlistSortOrder),
                                    onClick = {
                                        viewModel.setPlaylistSortOrder(option)
                                        showSortDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == settingsState.playlistSortOrder),
                                onClick = null
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settingsState by viewModel.settings.collectAsState()
    var showDecoderDialog by remember { mutableStateOf(false) }

    com.codesrahul.streamplus.ui.components.PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("General", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                SettingsCard {
                    SettingsClickableItem(
                        title = "Decoder Backend",
                        subtitle = when(settingsState.decoderPriority) {
                            1 -> "Application (Software)"
                            2 -> "Hardware Only (Strict)"
                            else -> "System Default (Hardware)"
                        },
                        icon = Icons.Default.Memory,
                        onClick = { showDecoderDialog = true },
                        showDivider = true
                    )
                    
                    SettingsSwitchItem(
                        title = "Tunneled Playback",
                        subtitle = "Enable audio tunneling for supported 4K/HDR content",
                        icon = Icons.Default.VerticalAlignCenter,
                        checked = settingsState.enableTunneling,
                        onCheckedChange = { viewModel.setEnableTunneling(it) }
                    )
                    
                    SettingsSwitchItem(
                        title = "Dolby Vision Support",
                        subtitle = "Enable support for Dolby Vision Profile 7",
                        icon = Icons.Default.CastConnected,
                        checked = settingsState.dolbyVision,
                        onCheckedChange = { viewModel.setDolbyVision(it) },
                        showDivider = false
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDecoderDialog) {
        val options = listOf(
            0 to "System Default (Hardware)",
            1 to "Application (Software)",
            2 to "Hardware Only (Strict)"
        )
        AlertDialog(
            onDismissRequest = { showDecoderDialog = false },
            title = { Text("Decoder Backend") },
            text = {
                Column {
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
fun AdvanceVideoSettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settingsState by viewModel.settings.collectAsState()
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showBufferDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    var showOrientationDialog by remember { mutableStateOf(false) }

    com.codesrahul.streamplus.ui.components.PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Video", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                SettingsCard {
                    SettingsClickableItem(
                        title = "Video Quality",
                        subtitle = settingsState.videoQuality,
                        icon = Icons.Default.HighQuality,
                        onClick = { showQualityDialog = true },
                        showDivider = true
                    )
                    
                    SettingsClickableItem(
                        title = "Default Audio Track",
                        subtitle = settingsState.defaultAudioLanguage,
                        icon = Icons.Default.Audiotrack,
                        onClick = { showAudioDialog = true },
                        showDivider = true
                    )

                    SettingsClickableItem(
                        title = "Network Buffer",
                        subtitle = "${settingsState.networkBufferSize} seconds",
                        icon = Icons.Default.Waves,
                        onClick = { showBufferDialog = true },
                        showDivider = true
                    )
                    
                    val aspectOptions = listOf("Fit", "Fill", "Zoom")
                    SettingsClickableItem(
                        title = "Default Aspect Ratio",
                        subtitle = aspectOptions.getOrElse(settingsState.defaultAspectRatio) { "Fit" },
                        icon = Icons.Default.AspectRatio,
                        onClick = { showAspectRatioDialog = true },
                        showDivider = true
                    )
                    
                    SettingsSwitchItem(
                        title = "Auto Frame Rate Matching",
                        subtitle = "Switch display refresh rate to match video frame rate",
                        icon = Icons.Default.Speed,
                        checked = settingsState.autoFrameRate,
                        onCheckedChange = { viewModel.setAutoFrameRate(it) }
                    )
                    
                    SettingsSwitchItem(
                        title = "Auto Picture-in-Picture",
                        subtitle = "Switch to PiP when leaving the app",
                        icon = Icons.Default.PictureInPicture,
                        checked = settingsState.autoPip,
                        onCheckedChange = { viewModel.setAutoPip(it) }
                    )
                    
                    SettingsSwitchItem(
                        title = "Skip Silence",
                        subtitle = "Skip parts with silence on the audio stream",
                        icon = Icons.Default.SkipNext,
                        checked = settingsState.skipSilence,
                        onCheckedChange = { viewModel.setSkipSilence(it) }
                    )
                    
                    SettingsSwitchItem(
                        title = "Repeat Toggle",
                        subtitle = "Extra control to allow indefinitely loop video",
                        icon = Icons.Default.Repeat,
                        checked = settingsState.repeatMode,
                        onCheckedChange = { viewModel.setRepeatMode(it) },
                        showDivider = true
                    )

                    SettingsClickableItem(
                        title = "Default Player Orientation",
                        subtitle = settingsState.playerOrientation.name.lowercase().replaceFirstChar { it.uppercase() },
                        icon = Icons.Default.CropRotate,
                        onClick = { showOrientationDialog = true },
                        showDivider = true
                    )

                    SettingsSwitchItem(
                        title = "Force Widevine L3",
                        subtitle = "Useful for devices with broken L1 certification (fixes black screen)",
                        icon = Icons.Default.Security,
                        checked = settingsState.forceWidevineL3,
                        onCheckedChange = { viewModel.setForceWidevineL3(it) },
                        showDivider = false
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showQualityDialog) {
        val options = listOf("Auto (Recommended)", "High", "Medium", "Low", "Data Saver")
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Video Quality") },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (option == settingsState.videoQuality),
                                    onClick = {
                                        viewModel.setVideoQuality(option)
                                        showQualityDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == settingsState.videoQuality),
                                onClick = null
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAudioDialog) {
        val options = listOf("Auto", "English", "Hindi", "Spanish", "French", "German", "Russian", "Arabic", "Chinese", "Japanese", "Portuguese")
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            title = { Text("Default Audio Track") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (option == settingsState.defaultAudioLanguage),
                                    onClick = {
                                        viewModel.setDefaultAudioLanguage(option)
                                        showAudioDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == settingsState.defaultAudioLanguage),
                                onClick = null
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBufferDialog) {
        val options = listOf(10, 20, 30, 45, 60, 90, 120)
        AlertDialog(
            onDismissRequest = { showBufferDialog = false },
            title = { Text("Network Buffer Size") },
            text = {
                Column {
                    options.forEach { seconds ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (seconds == settingsState.networkBufferSize),
                                    onClick = {
                                        viewModel.setNetworkBufferSize(seconds)
                                        showBufferDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (seconds == settingsState.networkBufferSize),
                                onClick = null
                            )
                            Text(
                                text = "$seconds seconds",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBufferDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAspectRatioDialog) {
        val aspectOptions = listOf("Fit", "Fill", "Zoom")
        AlertDialog(
            onDismissRequest = { showAspectRatioDialog = false },
            title = { Text("Default Aspect Ratio") },
            text = {
                Column {
                    aspectOptions.forEachIndexed { index, option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (index == settingsState.defaultAspectRatio),
                                    onClick = {
                                        viewModel.setDefaultAspectRatio(index)
                                        showAspectRatioDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (index == settingsState.defaultAspectRatio),
                                onClick = null
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAspectRatioDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showOrientationDialog) {
        AlertDialog(
            onDismissRequest = { showOrientationDialog = false },
            title = { Text("Default Player Orientation", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    PlayerOrientation.entries.forEach { orientation ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (orientation == settingsState.playerOrientation),
                                    onClick = {
                                        viewModel.setPlayerOrientation(orientation)
                                        showOrientationDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (orientation == settingsState.playerOrientation),
                                onClick = null
                            )
                            Text(
                                text = orientation.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOrientationDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsSettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settingsState by viewModel.settings.collectAsState()
    val context = LocalContext.current

    com.codesrahul.streamplus.ui.components.PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Shortcuts", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                SettingsSectionHeader("Player Gestures")
                SettingsCard {
                    SettingsSwitchItem(
                        title = "Volume Gestures",
                        subtitle = "Swipe up/down on the right side to adjust volume",
                        icon = Icons.Default.VolumeUp,
                        checked = settingsState.volumeGestures,
                        onCheckedChange = { viewModel.setVolumeGestures(it) },
                        showDivider = true
                    )
                    SettingsSwitchItem(
                        title = "Brightness Gestures",
                        subtitle = "Swipe up/down on the left side to adjust brightness",
                        icon = Icons.Default.BrightnessMedium,
                        checked = settingsState.brightnessGestures,
                        onCheckedChange = { viewModel.setBrightnessGestures(it) },
                        showDivider = false
                    )
                }

                SettingsSectionHeader("System")
                SettingsCard {
                    SettingsClickableItem(
                        title = "Captioning Preferences",
                        subtitle = "System subtitle settings",
                        icon = Icons.Default.ClosedCaption,
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_CAPTIONING_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open captioning settings", Toast.LENGTH_SHORT).show()
                            }
                        },
                        showDivider = false
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

