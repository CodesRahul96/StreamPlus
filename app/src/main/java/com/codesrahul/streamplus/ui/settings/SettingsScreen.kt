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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.codesrahul.streamplus.ui.components.PremiumBackground
import com.codesrahul.streamplus.ui.util.glassmorphic
import com.codesrahul.streamplus.ui.util.tvFocusable

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
    
    val context = androidx.compose.ui.platform.LocalContext.current

    com.codesrahul.streamplus.ui.components.PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Settings", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
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
                // Editorial Header Banner
                SettingsHeaderBanner()

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
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
                        } catch (e: Exception) { "1.0.0" }
                    }
                    SettingsClickableItem(
                        title = "App Version",
                        subtitle = version,
                        icon = Icons.Default.Info,
                        iconColor = TealSea,
                        onClick = { showAboutDialog = true },
                        showDivider = true
                    )
                    
                    SettingsClickableItem(
                        title = "Share App",
                        subtitle = "Share StreamPlus with friends",
                        icon = Icons.Default.Share,
                        iconColor = BlueSky,
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "Check out StreamPlus for IPTV and local media: https://play.google.com/store/apps/details?id=${context.packageName}")
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
                                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("support@streamplus.app"))
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "StreamPlus Feedback")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "No email client found. Send feedback to support@streamplus.app", android.widget.Toast.LENGTH_LONG).show()
                            }
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
}

@Composable
fun SettingsHeaderBanner() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .glassmorphic(cornerRadius = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        radius = 400f
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(18.dp))
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
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    Column {
                        Text(
                            text = "StreamPlus",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "v$version · Stable Release",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusBadge(
                        label = "Decoder",
                        value = "System Default",
                        icon = Icons.Default.Memory,
                        color = BlueSky
                    )
                    StatusBadge(
                        label = "Engine",
                        value = "Media3 / Exo",
                        icon = Icons.Default.Bolt,
                        color = GreenApple
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.StatusBadge(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        modifier = Modifier.weight(1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .glassmorphic(cornerRadius = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
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
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "StreamPlus",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Version $version",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "StreamPlus is a premium video player application with advanced playlist management, network stream support, and modern design semantics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Core Capabilities",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                val features = listOf(
                    "Fluid Shared Element Transitions",
                    "Advanced Playlist Management & Security",
                    "Real-time Hardware / Software Decoding",
                    "Modern, Glassmorphic Design Aesthetics"
                )

                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GreenApple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        "Dismiss",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun HelpFaqDialog(onDismiss: () -> Unit) {
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
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "Help & FAQ", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                Button(
                    onClick = onDismiss, 
                    modifier = Modifier.align(Alignment.End), 
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold)
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
