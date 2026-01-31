package com.codesrahul.streamplus.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp 
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "settings_main") {
        composable("settings_main") {
            SettingsMainList(
                onNavigate = { route -> navController.navigate(route) },
                onBack = onBack
            )
        }
        composable("appearance") {
            AppearanceSettings(
                viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("playlist_channel") {
            PlaylistChannelSettings(
                viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("general") {
            GeneralSettings(
                viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("advance_video") {
            AdvanceVideoSettings(
                viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("shortcuts") {
            ShortcutsSettings(
                viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainList(onNavigate: (String) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSectionHeader("Appearance & UI")
            SettingsCategoryItem(
                title = "Appearance",
                subtitle = "Theme mode, style, and accent colors",
                icon = Icons.Default.Brush,
                onClick = { onNavigate("appearance") },
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
            SettingsCategoryItem(
                title = "Playlist and Channel",
                subtitle = "Manage visibility and privacy",
                icon = Icons.Default.List,
                onClick = { onNavigate("playlist_channel") },
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer
            )

            SettingsSectionHeader("Playback & Tech")
            SettingsCategoryItem(
                title = "General",
                subtitle = "PiP, silent skipping, and gestures",
                icon = Icons.Default.Settings,
                onClick = { onNavigate("general") },
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
            SettingsCategoryItem(
                title = "Playback & Rendering",
                subtitle = "Decoders, tunneling, and HDR",
                icon = Icons.Default.VideoSettings,
                onClick = { onNavigate("advance_video") },
                iconContainerColor = MaterialTheme.colorScheme.errorContainer
            )

            SettingsSectionHeader("Accessibility")
            SettingsCategoryItem(
                title = "Shortcuts",
                subtitle = "System captioning and shortcuts",
                icon = Icons.Default.ShortText,
                onClick = { onNavigate("shortcuts") },
                iconContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            AppInfoFooter()
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun AppInfoFooter() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.52"
        } catch (e: Exception) {
            "1.0.52"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Divider(
            modifier = Modifier.width(48.dp).padding(bottom = 24.dp),
            thickness = 3.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        
        Text(
            text = "StreamPlus",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "Version $version (Stable)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/CodesRahul96"))
                        context.startActivity(intent)
                    } catch (e: Exception) {}
                }
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Crafted with ♥ by CodesRahul",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SettingsCategoryItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { 
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = iconContainerColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
        },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
