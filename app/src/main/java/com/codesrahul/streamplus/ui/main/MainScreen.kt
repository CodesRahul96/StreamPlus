 package com.codesrahul.streamplus.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.codesrahul.streamplus.ui.playlist.PlaylistScreen
import com.codesrahul.streamplus.ui.settings.SettingsScreen
import com.codesrahul.streamplus.ui.settings.SettingsViewModel
import com.codesrahul.streamplus.ui.local.LocalScreen

data class NavigationItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainScreen(initialTab: String? = null) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val navController = rememberNavController()
    val settingsState by settingsViewModel.settings.collectAsState(initial = com.codesrahul.streamplus.data.settings.AppSettings())
    val hideLabels = settingsState.hideNavLabels

    val context = androidx.compose.ui.platform.LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    androidx.activity.compose.BackHandler(enabled = true) {
        val popped = navController.popBackStack()
        if (!popped) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                (context as? android.app.Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val items = listOf(
        NavigationItem("Playlist", "playlist",    Icons.Default.List,          Icons.Outlined.List),
        NavigationItem("Gallery",  "local",        Icons.Default.VideoLibrary,   Icons.Outlined.VideoLibrary),
        NavigationItem("Stream",   "direct_play",  Icons.Default.Podcasts,       Icons.Outlined.Podcasts),
        NavigationItem("Settings", "settings",     Icons.Default.Settings,       Icons.Outlined.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = if (!hideLabels) {
                            {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        } else null,
                        selected = selected,
                        alwaysShowLabel = !hideLabels,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        // Draw background behind the translucent bar
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = initialTab ?: when (settingsState.defaultTab) {
                "playlist", "local", "direct_play" -> settingsState.defaultTab
                else -> "playlist"
            },
            modifier = Modifier.padding(innerPadding),
            enterTransition = { 
                fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth / 8 },
                    animationSpec = tween(300)
                ) 
            },
            exitTransition = { 
                fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth / 8 },
                    animationSpec = tween(300)
                )
            }
        ) {
            composable("playlist") {
                val context = androidx.compose.ui.platform.LocalContext.current
                PlaylistScreen(
                    onPlaylistClick = { playlistId ->
                        navController.navigate("playlist_detail/$playlistId")
                    },
                    onChannelClick = { channel ->
                        val intent = android.content.Intent(
                            context,
                            com.codesrahul.streamplus.ui.player.PlayerActivity::class.java
                        ).apply {
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_URL, channel.url)
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_TITLE, channel.name)
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_CHANNEL_LOGO, channel.logoUrl)
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_USER_AGENT, channel.userAgent)
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_PLAYLIST_ID, channel.playlistId)
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_GROUP, channel.group)
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_HEADERS, java.util.HashMap(channel.headers))
                            channel.licenseUrl?.let { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_LICENSE_URL, it) }
                            channel.drmScheme?.let { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_SCHEME, it) }
                            if (channel.drmHeaders.isNotEmpty()) {
                                putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_HEADERS, java.util.HashMap(channel.drmHeaders))
                            }
                        }
                        context.startActivity(intent)
                    }
                )
            }
            composable("local") { LocalScreen() }
            composable("direct_play") {
                com.codesrahul.streamplus.ui.home.DirectPlayScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate("history") }
                )
            }
            composable("history") {
                com.codesrahul.streamplus.ui.home.HistoryScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "playlist_detail/{playlistId}",
                arguments = listOf(androidx.navigation.navArgument("playlistId") { type = androidx.navigation.NavType.StringType })
            ) {
                com.codesrahul.streamplus.ui.playlist.PlaylistDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(
                route = "playlist_url_detail/{playlistId}/{name}/{userAgent}",
                arguments = listOf(
                    androidx.navigation.navArgument("playlistId") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("name")       { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("userAgent")  { type = androidx.navigation.NavType.StringType }
                )
            ) {
                com.codesrahul.streamplus.ui.playlist.PlaylistDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(
                route = "series_detail/{playlistId}/{seriesId}/{name}",
                arguments = listOf(
                    androidx.navigation.navArgument("playlistId") { type = androidx.navigation.NavType.IntType },
                    androidx.navigation.navArgument("seriesId")   { type = androidx.navigation.NavType.IntType },
                    androidx.navigation.navArgument("name")       { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val pid = backStackEntry.arguments?.getInt("playlistId") ?: -1
                val sid = backStackEntry.arguments?.getInt("seriesId") ?: -1
                val name = backStackEntry.arguments?.getString("name") ?: "Series"
                com.codesrahul.streamplus.ui.playlist.SeriesDetailScreen(
                    playlistId = pid,
                    seriesId = sid,
                    seriesName = java.net.URLDecoder.decode(name, "UTF-8"),
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
