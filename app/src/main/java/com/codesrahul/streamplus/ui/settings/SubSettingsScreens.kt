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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.unit.sp
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
import com.codesrahul.streamplus.ui.util.glassmorphic
import com.codesrahul.streamplus.ui.util.tvFocusable
import com.codesrahul.streamplus.ui.theme.NeonCyan
import com.codesrahul.streamplus.ui.theme.ElectricIndigo

// Specific named color shades for icon containers (iOS-inspired)
private val GreenApple  = Color(0xFF34C759)
private val BlueSky     = Color(0xFF007AFF)
private val PurpleVibe  = Color(0xFF5E5CE6)
private val OrangeWarm  = Color(0xFFFF9500)
private val RedAlpha    = Color(0xFFFF3B30)
private val TealSea     = Color(0xFF5AC8FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settingsState by viewModel.settings.collectAsState()
    
    com.codesrahul.streamplus.ui.components.PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(com.codesrahul.streamplus.R.string.appearance_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    windowInsets = WindowInsets(0.dp)
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
                        title = stringResource(com.codesrahul.streamplus.R.string.theme_mode_title),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.theme_mode_desc),
                        icon = Icons.Default.BrightnessMedium,
                        iconColor = PurpleVibe
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ThemeCard(
                                mode = ThemeMode.SYSTEM,
                                title = stringResource(com.codesrahul.streamplus.R.string.theme_system),
                                desc = stringResource(com.codesrahul.streamplus.R.string.theme_system_desc),
                                selected = settingsState.themeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeCard(
                                mode = ThemeMode.LIGHT,
                                title = stringResource(com.codesrahul.streamplus.R.string.theme_light),
                                desc = stringResource(com.codesrahul.streamplus.R.string.theme_light_desc),
                                selected = settingsState.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ThemeCard(
                                mode = ThemeMode.DARK,
                                title = stringResource(com.codesrahul.streamplus.R.string.theme_dark),
                                desc = stringResource(com.codesrahul.streamplus.R.string.theme_dark_desc),
                                selected = settingsState.themeMode == ThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeCard(
                                mode = ThemeMode.PITCH_BLACK,
                                title = stringResource(com.codesrahul.streamplus.R.string.theme_black),
                                desc = stringResource(com.codesrahul.streamplus.R.string.theme_black_desc),
                                selected = settingsState.themeMode == ThemeMode.PITCH_BLACK,
                                onClick = { viewModel.setThemeMode(ThemeMode.PITCH_BLACK) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // UI Customization Section
                SettingsCard {
                    SettingsItemHeader(
                        title = stringResource(com.codesrahul.streamplus.R.string.nav_pref_title),
                        icon = Icons.Default.LabelOff,
                        iconColor = TealSea
                    )
                    
                    SettingsSwitchItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.hide_nav_labels),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.hide_nav_labels_desc),
                        checked = settingsState.hideNavLabels,
                        onCheckedChange = { viewModel.setHideNavLabels(it) },
                        showDivider = false
                    )
                }

                // Default Tab Section
                SettingsCard {
                    SettingsItemHeader(
                        title = stringResource(com.codesrahul.streamplus.R.string.default_tab_title),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.default_tab_desc),
                        icon = Icons.Default.Home,
                        iconColor = BlueSky
                    )
                    
                    val tabs = listOf(
                        "playlist" to com.codesrahul.streamplus.R.string.nav_playlist, 
                        "local" to com.codesrahul.streamplus.R.string.nav_gallery, 
                        "direct_play" to com.codesrahul.streamplus.R.string.nav_stream
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        tabs.forEach { (route, labelRes) ->
                            val selected = route == settingsState.defaultTab
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clickable { viewModel.setDefaultTab(route) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                     else if (MaterialTheme.colorScheme.surface == Color(0xFF000000)) MaterialTheme.colorScheme.surface
                                                     else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = when(route) {
                                            "playlist" -> Icons.Default.PlaylistPlay
                                            "local" -> Icons.Default.VideoLibrary
                                            "direct_play" -> Icons.Default.Podcasts
                                            else -> Icons.Default.Home
                                        },
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(labelRes),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // App Language Section
                var showLanguageDialog by remember { mutableStateOf(false) }
                val currentLanguageName = if (settingsState.languageCode == "hi") "हिन्दी" else "English"
                
                SettingsCard {
                    SettingsItemHeader(
                        title = stringResource(com.codesrahul.streamplus.R.string.app_lang_title),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.app_lang_desc),
                        icon = Icons.Default.Language,
                        iconColor = TealSea
                    )
                    
                    SettingsClickableItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.app_lang_title),
                        subtitle = currentLanguageName,
                        icon = Icons.Default.Translate,
                        iconColor = BlueSky,
                        onClick = { showLanguageDialog = true },
                        showDivider = false
                    )
                }

                if (showLanguageDialog) {
                    val languages = listOf("en", "hi")
                    PremiumSelectionDialog(
                        title = stringResource(com.codesrahul.streamplus.R.string.app_lang_title),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.app_lang_desc),
                        options = languages,
                        selectedOption = settingsState.languageCode,
                        onOptionSelected = { code ->
                            viewModel.setLanguageCode(code)
                            showLanguageDialog = false
                        },
                        onDismiss = { showLanguageDialog = false },
                        labelProvider = { code ->
                            if (code == "hi") "हिन्दी" else "English"
                        },
                        descriptionProvider = { code ->
                            if (code == "hi") "हिन्दी इंटरफ़ेस और अनुवाद" else "Default English interface"
                        },
                        iconProvider = { code ->
                            Icon(
                                imageVector = if (code == "hi") Icons.Default.Translate else Icons.Default.Language,
                                contentDescription = null,
                                tint = if (code == settingsState.languageCode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
private fun ThemeCard(
    mode: ThemeMode,
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) 
                             else if (MaterialTheme.colorScheme.surface == Color(0xFF000000)) MaterialTheme.colorScheme.surface
                             else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (mode) {
                            ThemeMode.SYSTEM -> Brush.horizontalGradient(listOf(Color(0xFFE5E5EA), Color(0xFF1C1C1E)))
                            ThemeMode.LIGHT -> Brush.verticalGradient(listOf(Color(0xFFF2F2F7), Color(0xFFE5E5EA)))
                            ThemeMode.DARK -> Brush.verticalGradient(listOf(Color(0xFF1C1C1E), Color(0xFF2C2C2E)))
                            ThemeMode.PITCH_BLACK -> Brush.verticalGradient(listOf(Color.Black, Color(0xFF1C1C1E)))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when(mode) {
                            ThemeMode.SYSTEM -> Icons.Default.SettingsSuggest
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.PITCH_BLACK -> Icons.Default.NightsStay
                        },
                        contentDescription = null,
                        tint = when(mode) {
                            ThemeMode.SYSTEM -> Color.Gray
                            ThemeMode.LIGHT -> Color(0xFFFF9500)
                            ThemeMode.DARK -> Color(0xFF5E5CE6)
                            ThemeMode.PITCH_BLACK -> Color(0xFF30D158)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ThemeModeDialog(currentMode: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .glassmorphic(cornerRadius = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        if (MaterialTheme.colorScheme.surface == Color(0xFF000000)) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    )
                    .padding(24.dp)
            ) {
                Text(
                    "Theme Mode", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    ThemeMode.values().forEach { mode ->
                        val isSelected = mode == currentMode
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelect(mode)
                                    onDismiss()
                                }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = mode.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " "),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
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

    var qrUrlInput by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

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
                    title = { Text(stringResource(com.codesrahul.streamplus.R.string.playlist_settings_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    windowInsets = WindowInsets(0.dp)
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
                        title = stringResource(com.codesrahul.streamplus.R.string.default_playlist_icon),
                        subtitle = settingsState.defaultPlaylistIcon,
                        icon = Icons.Default.List,
                        iconColor = BlueSky,
                        onClick = { showIconDialog = true },
                        showDivider = true
                    )
                    
                    SettingsClickableItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.sort_order),
                        subtitle = settingsState.playlistSortOrder,
                        icon = Icons.Default.SortByAlpha,
                        iconColor = OrangeWarm,
                        onClick = { showSortDialog = true },
                        showDivider = true
                    )
                    
                    SettingsSwitchItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.mask_playlist_urls),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.mask_playlist_urls_desc),
                        icon = Icons.Default.VpnKey,
                        iconColor = RedAlpha,
                        checked = settingsState.hidePlaylistUri,
                        onCheckedChange = { viewModel.setHidePlaylistUri(it) },
                        showDivider = true
                    )
                    
                    SettingsClickableItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.qr_share),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.qr_share_desc),
                        icon = Icons.Default.QrCode,
                        iconColor = PurpleVibe,
                        onClick = { showQrDialog = true },
                        showDivider = true
                    )
                    
                    SettingsClickableItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.import_export),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.import_export_desc),
                        icon = Icons.Default.Backup,
                        iconColor = GreenApple,
                        onClick = { showExportImportDialog = true },
                        showDivider = false
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showQrDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showQrDialog = false; qrBitmap = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassmorphic(cornerRadius = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            if (MaterialTheme.colorScheme.surface == Color(0xFF000000)) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        )
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("QR Share Generator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = qrUrlInput,
                        onValueChange = { qrUrlInput = it; qrBitmap = null },
                        label = { Text("Playlist M3U URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    qrBitmap?.let { bmp ->
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Image(
                                painter = androidx.compose.ui.graphics.painter.BitmapPainter(bmp.asImageBitmap()),
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
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
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share QR Code", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showQrDialog = false; qrBitmap = null }) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Generate", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showExportImportDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showExportImportDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassmorphic(cornerRadius = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            if (MaterialTheme.colorScheme.surface == Color(0xFF000000)) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        )
                        .padding(24.dp)
                ) {
                    Text("Backup & Restore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Export saves your loaded playlist directories into a clean JSON file. Import reconstructs them locally.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { exportLauncher.launch("streamplus_playlists_backup.json") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Backup JSON", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { importLauncher.launch("application/json") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Backup JSON", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showExportImportDialog = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showIconDialog) {
        PremiumSelectionDialog(
            title = stringResource(com.codesrahul.streamplus.R.string.default_playlist_icon),
            subtitle = stringResource(com.codesrahul.streamplus.R.string.default_playlist_icon_desc),
            options = listOf("List", "Grid", "Card", "Compact"),
            selectedOption = settingsState.defaultPlaylistIcon,
            onOptionSelected = { viewModel.setDefaultPlaylistIcon(it) },
            onDismiss = { showIconDialog = false },
            labelProvider = { it },
            iconProvider = { option ->
                Icon(
                    imageVector = when(option) {
                        "Grid" -> Icons.Default.GridView
                        "Card" -> Icons.Default.FeaturedPlayList
                        "Compact" -> Icons.Default.DensityMedium
                        else -> Icons.Default.List
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }

    if (showSortDialog) {
        PremiumSelectionDialog(
            title = stringResource(com.codesrahul.streamplus.R.string.sort_order),
            subtitle = stringResource(com.codesrahul.streamplus.R.string.sort_order_desc),
            options = listOf("Name", "Added Date", "Category", "Last Watched"),
            selectedOption = settingsState.playlistSortOrder,
            onOptionSelected = { viewModel.setPlaylistSortOrder(it) },
            onDismiss = { showSortDialog = false },
            labelProvider = { it },
            iconProvider = { option ->
                Icon(
                    imageVector = when(option) {
                        "Name" -> Icons.Default.SortByAlpha
                        "Added Date" -> Icons.Default.CalendarToday
                        "Category" -> Icons.Default.Category
                        else -> Icons.Default.History
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
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
                    title = { Text(stringResource(com.codesrahul.streamplus.R.string.general_settings_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    windowInsets = WindowInsets(0.dp)
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
                        title = stringResource(com.codesrahul.streamplus.R.string.decoder_backend),
                        subtitle = when(settingsState.decoderPriority) {
                            1 -> "Application (Software)"
                            2 -> "Hardware Only (Strict)"
                            else -> "System Default (Hardware)"
                        },
                        icon = Icons.Default.Memory,
                        iconColor = BlueSky,
                        onClick = { showDecoderDialog = true },
                        showDivider = true
                    )
                    
                    SettingsSwitchItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.tunneled_playback),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.tunneled_playback_desc),
                        icon = Icons.Default.VerticalAlignCenter,
                        iconColor = PurpleVibe,
                        checked = settingsState.enableTunneling,
                        onCheckedChange = { viewModel.setEnableTunneling(it) },
                        showDivider = true
                    )
                    
                    SettingsSwitchItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.dolby_vision_support),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.dolby_vision_support_desc),
                        icon = Icons.Default.CastConnected,
                        iconColor = OrangeWarm,
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
        val selectedOption = options.find { it.first == settingsState.decoderPriority } ?: options[0]
        PremiumSelectionDialog(
            title = stringResource(com.codesrahul.streamplus.R.string.decoder_backend),
            subtitle = stringResource(com.codesrahul.streamplus.R.string.decoder_backend_desc),
            options = options,
            selectedOption = selectedOption,
            onOptionSelected = { viewModel.setDecoderPriority(it.first) },
            onDismiss = { showDecoderDialog = false },
            labelProvider = { it.second },
            descriptionProvider = { option ->
                when(option.first) {
                    0 -> "Recommended for most devices. Balance of performance & power."
                    1 -> "CPU-based rendering. Best compatibility, higher battery use."
                    2 -> "Strict HW only. Smooth playback, may fail on unsupported codecs."
                    else -> ""
                }
            },
            iconProvider = { option ->
                Icon(
                    imageVector = when(option.first) {
                        0 -> Icons.Default.Settings
                        1 -> Icons.Default.Code
                        else -> Icons.Default.Memory
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
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
    var showCodecDialog by remember { mutableStateOf(false) }
    var showSubtitleTranscodeDialog by remember { mutableStateOf(false) }
    var showAiHealingDialog by remember { mutableStateOf(false) }

    com.codesrahul.streamplus.ui.components.PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(com.codesrahul.streamplus.R.string.video_settings_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    windowInsets = WindowInsets(0.dp)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Universal Codec & Stream Intelligence ───────────────────
                SettingsSectionHeader("Universal Engine & Intelligence")
                SettingsCard {
                    SettingsClickableItem(
                        title = "Universal Codec & Container Suite",
                        subtitle = when (settingsState.decoderPriority) {
                            1 -> "Active: Software Decoder (CPU Fallback)"
                            2 -> "Active: Hardware Only (Strict H.264/H.265)"
                            else -> "Active: Hardware Accelerated (H.264, H.265/HEVC, VP9, AV1)"
                        },
                        icon = Icons.Default.Memory,
                        iconColor = NeonCyan,
                        onClick = { showCodecDialog = true },
                        showDivider = true
                    )
                    SettingsClickableItem(
                        title = "Universal Subtitle Transcoding",
                        subtitle = "SubRip (.srt), ASS/SSA, and TTML cues transcoding active",
                        icon = Icons.Default.Subtitles,
                        iconColor = TealSea,
                        onClick = { showSubtitleTranscodeDialog = true },
                        showDivider = true
                    )
                    SettingsClickableItem(
                        title = "Smart Stream Inspector & Auto-Recovery",
                        subtitle = "Anti-hotlink spoofing, automatic HTTP 403 bypass & live recovery",
                        icon = Icons.Default.AutoAwesome,
                        iconColor = ElectricIndigo,
                        onClick = { showAiHealingDialog = true },
                        showDivider = false
                    )
                }

                SettingsSectionHeader("Playback Preferences")
                SettingsCard {
                    SettingsClickableItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.video_quality),
                        subtitle = settingsState.videoQuality,
                        icon = Icons.Default.HighQuality,
                        iconColor = BlueSky,
                        onClick = { showQualityDialog = true },
                        showDivider = true
                    )
                    
                    SettingsClickableItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.default_audio_track),
                        subtitle = settingsState.defaultAudioLanguage,
                        icon = Icons.Default.Audiotrack,
                        iconColor = TealSea,
                        onClick = { showAudioDialog = true },
                        showDivider = true
                    )

                    SettingsClickableItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.network_buffer),
                        subtitle = "${settingsState.networkBufferSize} seconds",
                        icon = Icons.Default.Waves,
                        iconColor = PurpleVibe,
                        onClick = { showBufferDialog = true },
                        showDivider = true
                    )
                    
                    val aspectOptions = listOf("Auto", "Fit", "Fill", "Zoom")
                    SettingsClickableItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.default_aspect_ratio),
                        subtitle = aspectOptions.getOrElse(settingsState.defaultAspectRatio) { "Auto" },
                        icon = Icons.Default.AspectRatio,
                        iconColor = OrangeWarm,
                        onClick = { showAspectRatioDialog = true },
                        showDivider = true
                    )
                    
                    SettingsSwitchItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.frame_rate_matching),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.frame_rate_matching_desc),
                        icon = Icons.Default.Speed,
                        iconColor = GreenApple,
                        checked = settingsState.autoFrameRate,
                        onCheckedChange = { viewModel.setAutoFrameRate(it) },
                        showDivider = true
                    )
                    
                    SettingsSwitchItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.auto_pip),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.auto_pip_desc),
                        icon = Icons.Default.PictureInPicture,
                        iconColor = BlueSky,
                        checked = settingsState.autoPip,
                        onCheckedChange = { viewModel.setAutoPip(it) },
                        showDivider = true
                    )
                    
                    SettingsSwitchItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.skip_silence),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.skip_silence_desc),
                        icon = Icons.Default.SkipNext,
                        iconColor = RedAlpha,
                        checked = settingsState.skipSilence,
                        onCheckedChange = { viewModel.setSkipSilence(it) },
                        showDivider = true
                    )
                    
                    SettingsSwitchItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.repeat_mode),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.repeat_mode_desc),
                        icon = Icons.Default.Repeat,
                        iconColor = TealSea,
                        checked = settingsState.repeatMode,
                        onCheckedChange = { viewModel.setRepeatMode(it) },
                        showDivider = true
                    )

                    SettingsClickableItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.screen_rotation),
                        subtitle = settingsState.playerOrientation.name.lowercase().replaceFirstChar { it.uppercase() },
                        icon = Icons.Default.CropRotate,
                        iconColor = OrangeWarm,
                        onClick = { showOrientationDialog = true },
                        showDivider = true
                    )

                    SettingsSwitchItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.software_decoder),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.software_decoder_desc),
                        icon = Icons.Default.Security,
                        iconColor = GreenApple,
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
        PremiumSelectionDialog(
            title = stringResource(com.codesrahul.streamplus.R.string.video_quality),
            subtitle = stringResource(com.codesrahul.streamplus.R.string.video_quality_desc),
            options = options,
            selectedOption = settingsState.videoQuality,
            onOptionSelected = { viewModel.setVideoQuality(it) },
            onDismiss = { showQualityDialog = false },
            labelProvider = { it },
            descriptionProvider = { option ->
                when(option) {
                    "Auto (Recommended)" -> "Automatically select best quality based on connection speed."
                    "High" -> "Use highest available quality stream (up to 4K/1080p)."
                    "Medium" -> "Balanced quality for standard definition viewing (720p)."
                    "Low" -> "Standard definition video quality (480p)."
                    "Data Saver" -> "Minimize cellular data usage. Limits stream quality."
                    else -> ""
                }
            },
            iconProvider = { option ->
                Icon(
                    imageVector = when(option) {
                        "Auto (Recommended)" -> Icons.Default.AutoMode
                        "High" -> Icons.Default.HighQuality
                        "Data Saver" -> Icons.Default.DataUsage
                        else -> Icons.Default.VideoSettings
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }

    if (showAudioDialog) {
        val options = listOf(
            "Auto", "English", "Hindi", "Bengali", "Telugu", "Marathi", "Tamil", "Urdu", 
            "Gujarati", "Kannada", "Odia", "Malayalam", "Punjabi", "Sanskrit", 
            "Assamese", "Maithili", "Santali", "Kashmiri", "Nepali", "Konkani", 
            "Sindhi", "Dogri", "Manipuri", "Bodo", "Spanish", "French", 
            "German", "Russian", "Arabic", "Chinese", "Japanese", "Portuguese"
        )
        PremiumSelectionDialog(
            title = stringResource(com.codesrahul.streamplus.R.string.default_audio_track),
            subtitle = stringResource(com.codesrahul.streamplus.R.string.default_audio_track_desc),
            options = options,
            selectedOption = settingsState.defaultAudioLanguage,
            onOptionSelected = { viewModel.setDefaultAudioLanguage(it) },
            onDismiss = { showAudioDialog = false },
            labelProvider = { it },
            iconProvider = { option ->
                Icon(
                    imageVector = if (option == "Auto") Icons.Default.Language else Icons.Default.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }

    if (showBufferDialog) {
        val options = listOf(10, 20, 30, 45, 60, 90, 120)
        PremiumSelectionDialog(
            title = stringResource(com.codesrahul.streamplus.R.string.network_buffer),
            subtitle = stringResource(com.codesrahul.streamplus.R.string.network_buffer_desc),
            options = options,
            selectedOption = settingsState.networkBufferSize,
            onOptionSelected = { viewModel.setNetworkBufferSize(it) },
            onDismiss = { showBufferDialog = false },
            labelProvider = { "$it seconds" },
            descriptionProvider = { seconds ->
                when(seconds) {
                    10 -> "Ultra low latency, might buffer on slower networks."
                    20 -> "Balanced buffering for standard connections."
                    30 -> "Recommended. Good for most network speeds."
                    45 -> "Sufficient pre-caching for minor network drops."
                    60 -> "High buffer, ideal for highly unstable connections."
                    90 -> "Very high buffer, requires more device memory."
                    120 -> "Maximum pre-caching buffer, best for extreme environments."
                    else -> ""
                }
            },
            iconProvider = { option ->
                Icon(
                    imageVector = when {
                        option <= 20 -> Icons.Default.OfflineBolt
                        option <= 45 -> Icons.Default.Speed
                        else -> Icons.Default.CloudQueue
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }

    if (showAspectRatioDialog) {
        val aspectOptions = listOf("Auto", "Fit", "Fill", "Zoom")
        val selectedOption = aspectOptions.getOrNull(settingsState.defaultAspectRatio) ?: "Auto"
        PremiumSelectionDialog(
            title = stringResource(com.codesrahul.streamplus.R.string.default_aspect_ratio),
            subtitle = stringResource(com.codesrahul.streamplus.R.string.default_aspect_ratio_desc),
            options = aspectOptions,
            selectedOption = selectedOption,
            onOptionSelected = { option ->
                viewModel.setDefaultAspectRatio(aspectOptions.indexOf(option).coerceAtLeast(0))
            },
            onDismiss = { showAspectRatioDialog = false },
            labelProvider = { it },
            descriptionProvider = { option ->
                when(option) {
                    "Auto" -> "Smart detection — adapts dynamically to content and screen dimensions."
                    "Fit" -> "Preserve original aspect ratio. May show black bars."
                    "Fill" -> "Stretch video to cover full screen dimensions."
                    "Zoom" -> "Crop video slightly to fill screen without stretching."
                    else -> ""
                }
            },
            iconProvider = { option ->
                Icon(
                    imageVector = when(option) {
                        "Auto" -> Icons.Default.AutoAwesome
                        "Fit" -> Icons.Default.CenterFocusStrong
                        "Fill" -> Icons.Default.AspectRatio
                        else -> Icons.Default.ZoomOutMap
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }

    if (showOrientationDialog) {
        PremiumSelectionDialog(
            title = stringResource(com.codesrahul.streamplus.R.string.screen_rotation),
            subtitle = stringResource(com.codesrahul.streamplus.R.string.screen_rotation_desc),
            options = PlayerOrientation.entries,
            selectedOption = settingsState.playerOrientation,
            onOptionSelected = { viewModel.setPlayerOrientation(it) },
            onDismiss = { showOrientationDialog = false },
            labelProvider = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
            descriptionProvider = { option ->
                when(option) {
                    PlayerOrientation.AUTO -> "Matches physical device orientation dynamically."
                    PlayerOrientation.LANDSCAPE -> "Locks player to horizontal widescreen orientation."
                    PlayerOrientation.PORTRAIT -> "Locks player to vertical portrait orientation."
                }
            },
            iconProvider = { option ->
                Icon(
                    imageVector = when(option) {
                        PlayerOrientation.AUTO -> Icons.Default.ScreenRotation
                        PlayerOrientation.LANDSCAPE -> Icons.Default.StayCurrentLandscape
                        PlayerOrientation.PORTRAIT -> Icons.Default.StayCurrentPortrait
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }

    if (showCodecDialog) {
        val options = listOf(
            0 to "Hardware Accelerated (Auto Fallback)",
            1 to "Application (Software Decoder)",
            2 to "Hardware Only (Strict HW)"
        )
        val selectedOption = options.find { it.first == settingsState.decoderPriority } ?: options[0]
        PremiumSelectionDialog(
            title = "Universal Codec & Container Suite",
            subtitle = "Configures hardware and software decoder priority for H.264, H.265 (HEVC), VP9, and AV1 across TS, MP4, and MKV containers.",
            options = options,
            selectedOption = selectedOption,
            onOptionSelected = { viewModel.setDecoderPriority(it.first) },
            onDismiss = { showCodecDialog = false },
            labelProvider = { it.second },
            descriptionProvider = { option ->
                when (option.first) {
                    0 -> "Recommended. Decodes H.264/H.265 using device GPU/DSP chips, with automatic software fallback if a profile is unsupported."
                    1 -> "CPU-based decoding. Ensures playback compatibility for rare or broken H.265/HEVC profiles at the cost of higher battery usage."
                    2 -> "Strict hardware-only decoding for maximum efficiency. Fails fast if hardware doesn't support the codec."
                    else -> ""
                }
            },
            iconProvider = { option ->
                Icon(
                    imageVector = when (option.first) {
                        0 -> Icons.Default.Memory
                        1 -> Icons.Default.Code
                        else -> Icons.Default.SettingsSuggest
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }

    if (showSubtitleTranscodeDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSubtitleTranscodeDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassmorphic(cornerRadius = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            if (MaterialTheme.colorScheme.surface == Color(0xFF000000)) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        )
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Universal Subtitle Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Real-time Text Cue Transcoding",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "• SubRip (.srt): Parsed into native UTF-8 timed cues.\n• ASS/SSA: Strips complex styling into readable subtitles.\n• TTML / WebVTT: Full support for multi-track stream captions.\n• Live Transcoding: Active and enforced on all media sources.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showSubtitleTranscodeDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAiHealingDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAiHealingDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassmorphic(cornerRadius = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            if (MaterialTheme.colorScheme.surface == Color(0xFF000000)) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        )
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricIndigo.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricIndigo)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Smart Stream Inspector",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Autonomous Self-Healing Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "• HTTP 403 Forbidden Bypass: Auto-rotates User-Agent & Referer headers to Smart TV signatures upon access denial.\n• Decoder Crash Auto-Recovery: Drops to software fallback without terminating stream.\n• Live Edge Alignment: Automatically seeks to live window if stream clock slips.\n• Zero-Buffer Reconnect: Resumes interrupted live streams with exponential backoff.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showAiHealingDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Active & Monitored", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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
                    title = { Text(stringResource(com.codesrahul.streamplus.R.string.gesture_accessibility_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    windowInsets = WindowInsets(0.dp)
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
                        title = stringResource(com.codesrahul.streamplus.R.string.volume_gesture),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.volume_gesture_desc),
                        icon = Icons.Default.VolumeUp,
                        iconColor = BlueSky,
                        checked = settingsState.volumeGestures,
                        onCheckedChange = { viewModel.setVolumeGestures(it) },
                        showDivider = true
                    )
                    SettingsSwitchItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.brightness_gesture),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.brightness_gesture_desc),
                        icon = Icons.Default.BrightnessMedium,
                        iconColor = PurpleVibe,
                        checked = settingsState.brightnessGestures,
                        onCheckedChange = { viewModel.setBrightnessGestures(it) },
                        showDivider = false
                    )
                }

                SettingsSectionHeader("System Shortcuts")
                SettingsCard {
                    SettingsClickableItem(
                        title = stringResource(com.codesrahul.streamplus.R.string.system_captions),
                        subtitle = stringResource(com.codesrahul.streamplus.R.string.system_captions_desc),
                        icon = Icons.Default.ClosedCaption,
                        iconColor = TealSea,
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
