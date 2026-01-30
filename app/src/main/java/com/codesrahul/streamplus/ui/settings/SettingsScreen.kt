package com.codesrahul.streamplus.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
                title = { Text("Settings") },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCategoryItem(
                title = "Appearance",
                subtitle = "Customize theme and style preferences",
                icon = Icons.Default.Brush,
                onClick = { onNavigate("appearance") }
            )
            SettingsCategoryItem(
                title = "Playlist and Channel",
                subtitle = "Control visibility of playlist URI and channel images",
                icon = Icons.Default.List,
                onClick = { onNavigate("playlist_channel") }
            )
            SettingsCategoryItem(
                title = "General",
                subtitle = "Toggle options for PiP mode, silence skipping, etc.",
                icon = Icons.Default.Settings,
                onClick = { onNavigate("general") }
            )
            SettingsCategoryItem(
                title = "Advance Video & render options",
                subtitle = "Configure decoder priority, tunneling, etc.",
                icon = Icons.Default.VideoSettings,
                onClick = { onNavigate("advance_video") }
            )
            SettingsCategoryItem(
                title = "Shortcuts",
                subtitle = "Access captioning settings",
                icon = Icons.Default.ShortText,
                onClick = { onNavigate("shortcuts") }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val context = androidx.compose.ui.platform.LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/CodesRahul96"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "Developed by CodesRahul",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) // Highlighted color to indicate link
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(
                    text = "v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsCategoryItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
