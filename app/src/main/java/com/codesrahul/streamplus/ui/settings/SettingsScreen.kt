package com.codesrahul.streamplus.ui.settings

import com.codesrahul.streamplus.data.settings.ThemeMode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.codesrahul.streamplus.utils.AppUpdater

// Specific named color shades for icon containers (iOS-inspired)
private val GreenApple  = Color(0xFF34C759)
private val BlueSky     = Color(0xFF007AFF)
private val PurpleVibe  = Color(0xFF5E5CE6)
private val OrangeWarm  = Color(0xFFFF9500)
private val RedAlpha    = Color(0xFFFF3B30)
private val TealSea     = Color(0xFF5AC8FA)

@OptIn(ExperimentalMaterial3Api::class)
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
            AppearanceSettings(viewModel, onBack = { navController.popBackStack() })
        }
        composable("playlist_channel") {
            PlaylistChannelSettings(viewModel, onBack = { navController.popBackStack() })
        }
        composable("general") {
            GeneralSettings(viewModel, onBack = { navController.popBackStack() })
        }
        composable("advance_video") {
            AdvanceVideoSettings(viewModel, onBack = { navController.popBackStack() })
        }
        composable("shortcuts") {
            ShortcutsSettings(viewModel, onBack = { navController.popBackStack() })
        }
        composable("accessibility") {
            AccessibilitySettings(viewModel, onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainList(onNavigate: (String) -> Unit, onBack: () -> Unit) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    
    // Update States
    var isCheckingForUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdater.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    val downloadProgress by AppUpdater.downloadProgress.collectAsState()
    
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    com.codesrahul.streamplus.ui.components.PremiumBackground {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            "Settings", 
                            style = MaterialTheme.typography.headlineLarge, 
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Aesthetics ───────────────────────────────────────────
                SettingsSectionHeader("Aesthetics")
                SettingsCard {
                    SettingsClickableItem(
                        title = "Appearance",
                        subtitle = "Theme, colors and UI tweaks",
                        icon = Icons.Default.Palette,
                        iconColor = PurpleVibe,
                        onClick = { onNavigate("appearance") },
                        showDivider = false
                    )
                }

                // ── Core Features ────────────────────────────────────────
                SettingsSectionHeader("Playback")
                SettingsCard {
                    SettingsClickableItem(
                        title = "Video",
                        subtitle = "Quality, PiP and decoder",
                        icon = Icons.Default.VideoSettings,
                        iconColor = BlueSky,
                        onClick = { onNavigate("advance_video") },
                        showDivider = true
                    )
                    SettingsClickableItem(
                        title = "General",
                        subtitle = "Decoder priority and networking",
                        icon = Icons.Default.Sync,
                        iconColor = TealSea,
                        onClick = { onNavigate("general") },
                        showDivider = false
                    )
                }

                // ── Management ───────────────────────────────────────────
                SettingsSectionHeader("Management")
                SettingsCard {
                    SettingsClickableItem(
                        title = "Playlist",
                        subtitle = "Backups, security and organization",
                        icon = Icons.Default.PlaylistPlay,
                        iconColor = GreenApple,
                        onClick = { onNavigate("playlist_channel") },
                        showDivider = true
                    )
                    SettingsClickableItem(
                        title = "Shortcuts",
                        subtitle = "System captions and shortcuts",
                        icon = Icons.Default.TouchApp,
                        iconColor = OrangeWarm,
                        onClick = { onNavigate("shortcuts") },
                        showDivider = true
                    )
                    SettingsClickableItem(
                        title = "Accessibility",
                        subtitle = "Customize Accessibility settings",
                        icon = Icons.Default.Accessibility,
                        iconColor = BlueSky,
                        onClick = { onNavigate("accessibility") },
                        showDivider = false
                    )
                }

                // ── App ──────────────────────────────────────────────────
                SettingsSectionHeader("App")
                SettingsCard {
                    val version = remember {
                        try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.6"
                        } catch (e: Exception) { "1.0.6" }
                    }
                    SettingsClickableItem(
                        title = "App Version",
                        subtitle = version,
                        icon = Icons.Default.Info,
                        iconColor = TealSea,
                        onClick = { showAboutDialog = true },
                        showDivider = true
                    )
                    
                    if (isCheckingForUpdate) {
                        ListItem(
                            headlineContent = { Text("Checking for updates...") },
                            trailingContent = { CircularProgressIndicator(modifier = Modifier.size(24.dp)) },
                            leadingContent = {
                                Surface(shape = RoundedCornerShape(10.dp), color = TealSea, modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.SystemUpdate, null) }
                                }
                            }
                        )
                    } else if (isDownloading) {
                        ListItem(
                            headlineContent = { Text("Downloading Update...") },
                            trailingContent = { Text("${(downloadProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall) },
                            supportingContent = {
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                )
                            },
                            leadingContent = {
                                Surface(shape = RoundedCornerShape(10.dp), color = GreenApple, modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CloudDownload, null) }
                                }
                            }
                        )
                    } else {
                        SettingsClickableItem(
                            title = "Check for Updates",
                            subtitle = "Download the latest version",
                            icon = Icons.Default.SystemUpdate,
                            iconColor = TealSea,
                            onClick = { 
                                scope.launch {
                                    isCheckingForUpdate = true
                                    val info = AppUpdater.checkForUpdate(context)
                                    isCheckingForUpdate = false
                                    if (info != null && info.isUpdateAvailable) {
                                        updateInfo = info
                                        showUpdateDialog = true
                                    } else {
                                        android.widget.Toast.makeText(context, "You are on the latest version", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            showDivider = true
                        )
                    }

                    SettingsClickableItem(
                        title = "Share App",
                        subtitle = "Share StreamPlus with friends",
                        icon = Icons.Default.Share,
                        iconColor = BlueSky,
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "Check out StreamPlus for IPTV and local media: https://github.com/CodesRahul96/StreamPlus")
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, null))
                        },
                        showDivider = true
                    )
                    SettingsClickableItem(
                        title = "About",
                        subtitle = "Learn more about StreamPlus",
                        icon = Icons.Default.Description,
                        iconColor = BlueSky,
                        onClick = { showAboutDialog = true },
                        showDivider = false
                    )
                }

                // ── Support ──────────────────────────────────────────────
                SettingsSectionHeader("Support")
                SettingsCard {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    SettingsClickableItem(
                        title = "Help & FAQ",
                        subtitle = "Get help and find answers",
                        icon = Icons.Default.Help,
                        iconColor = BlueSky,
                        onClick = { showHelpDialog = true },
                        showDivider = true
                    )
                    SettingsClickableItem(
                        title = "Send Feedback",
                        subtitle = "Help us improve StreamPlus",
                        icon = Icons.Default.Feedback,
                        iconColor = TealSea,
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:")
                                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("codesrahul96@gmail.com"))
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "StreamPlus Feedback")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                        showDivider = true
                    )
                    SettingsClickableItem(
                        title = "Rate App",
                        subtitle = "Rate StreamPlus on the store",
                        icon = Icons.Default.Star,
                        iconColor = OrangeWarm,
                        onClick = {
                            try {
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${context.packageName}")))
                            } catch (e: Exception) {
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                            }
                        },
                        showDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                AppInfoFooter()
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
    if (showHelpDialog) {
        HelpFaqDialog(onDismiss = { showHelpDialog = false })
    }
    
    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Update Available: ${updateInfo!!.latestVersionName}") },
            text = { Text(updateInfo!!.content) },
            confirmButton = {
                TextButton(onClick = { 
                    showUpdateDialog = false
                    isDownloading = true
                    AppUpdater.downloadAndInstallUpdate(context, updateInfo!!.downloadUrl) {
                        isDownloading = true
                    }
                }) {
                    Text("Download & Install")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Later")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp),
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
fun AppInfoFooter() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.1"
        } catch (e: Exception) { "1.0.1" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App icon + name badge
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "StreamPlus",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            "v$version · Stable",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .clickable {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.codesrahul.in"))
                        context.startActivity(intent)
                    } catch (e: Exception) {}
                }
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.Code, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Crafted with ♥ by CodesRahul",
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
                shape = RoundedCornerShape(10.dp),
                color = iconContainerColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// ── Accessibility Settings Screen ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settingsState by viewModel.settings.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    com.codesrahul.streamplus.ui.components.PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Accessibility", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
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
                SettingsSectionHeader("Accessibility")
                SettingsCard {
                    SettingsSwitchItem(
                        title = "Embedded Styles",
                        subtitle = "Apply embedded subtitle styles",
                        icon = Icons.Default.Palette,
                        iconColor = BlueSky,
                        checked = settingsState.applyEmbeddedSubtitles,
                        onCheckedChange = { viewModel.setApplyEmbeddedSubtitles(it) },
                        showDivider = true
                    )
                    SettingsSwitchItem(
                        title = "Bold Style",
                        subtitle = "Use bold typeface as regular",
                        icon = Icons.Default.TextFormat,
                        iconColor = BlueSky,
                        checked = settingsState.useBoldTypeface,
                        onCheckedChange = { viewModel.setUseBoldTypeface(it) },
                        showDivider = true
                    )
                    SettingsClickableItem(
                        title = "Captioning Preferences",
                        subtitle = "System subtitle settings",
                        icon = Icons.Default.ClosedCaption,
                        iconColor = TealSea,
                        onClick = {
                            try {
                                val intent = android.content.Intent("android.settings.CAPTIONING_SETTINGS")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                } catch (ex: Exception) {}
                            }
                        },
                        showDivider = false
                    )
                }
            }
        }
    }
}

// ── About & Help Dialogs ──────────────────────────────────────────────────

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.6"
        } catch (e: Exception) { "1.0.6" }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1A1C1E), // Deep dark surface
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Header with App Icon and Name
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFF00B8D4))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "StreamPlus",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Version Chip
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Version $version",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Description
                Text(
                    "StreamPlus is a modern video player app with playlist management and beautiful UI.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Features Header
                Text(
                    "Features:",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Feature List
                val features = listOf(
                    "Playlist Management",
                    "Network Streaming",
                    "Import/Export",
                    "Customizable UI"
                )

                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            feature,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Close Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        "Close",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        }
    }
}

@Composable
fun HelpFaqDialog(onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text("Help & FAQ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FaqItem("How do I add a playlist?", "Go to the Playlists tab, click the '+' button, and enter your M3U URL or select a local file.")
                    FaqItem("What is DRM?", "Digital Rights Management. Some channels require a license URL and a specific scheme (like Widevine) to play.")
                    FaqItem("Why is my stream lagging?", "Check your internet connection or try changing the 'Network Buffer Size' in Settings > General.")
                    FaqItem("How to use Picture-in-Picture?", "Enable 'Auto PiP' in Settings > Video settings. The player will minimize when you leave the app.")
                    FaqItem("Can I play local videos?", "Yes, go to the Gallery tab to browse and play videos stored on your device.")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End), shape = RoundedCornerShape(12.dp)) {
                    Text("Got it")
                }
            }
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Column {
        Text(question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
