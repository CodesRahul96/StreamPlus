 package com.codesrahul.streamplus.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import com.codesrahul.streamplus.ui.playlist.PlaylistScreen
import com.codesrahul.streamplus.ui.settings.SettingsScreen
import com.codesrahul.streamplus.ui.settings.SettingsViewModel
import com.codesrahul.streamplus.ui.local.LocalScreen

data class NavigationItem(
    val titleRes: Int,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainScreen(
    initialTab: String? = null,
    playlistViewModel: com.codesrahul.streamplus.ui.playlist.PlaylistViewModel = hiltViewModel()
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val navController = rememberNavController()
    val settingsState by settingsViewModel.settings.collectAsState(initial = com.codesrahul.streamplus.data.settings.AppSettings())
    val hideLabels = settingsState.hideNavLabels

    val context = androidx.compose.ui.platform.LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    androidx.activity.compose.BackHandler(enabled = true) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        val isAtRoot = currentRoute == null || currentRoute == "playlist"

        if (!isAtRoot) {
            val popped = navController.popBackStack()
            if (!popped) {
                navController.navigate("playlist") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                var currentContext = context
                while (currentContext is android.content.ContextWrapper) {
                    if (currentContext is android.app.Activity) {
                        currentContext.finish()
                        break
                    }
                    currentContext = currentContext.baseContext
                }
            } else {
                lastBackPressTime = currentTime
                android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val items = listOf(
        NavigationItem(com.codesrahul.streamplus.R.string.nav_playlist, "playlist",    Icons.AutoMirrored.Filled.List,      Icons.AutoMirrored.Outlined.List),
        NavigationItem(com.codesrahul.streamplus.R.string.nav_gallery,  "local",        Icons.Default.VideoLibrary,          Icons.Outlined.VideoLibrary),
        NavigationItem(com.codesrahul.streamplus.R.string.nav_stream,   "direct_play",  Icons.Default.Podcasts,              Icons.Outlined.Podcasts),
        NavigationItem(com.codesrahul.streamplus.R.string.nav_settings, "settings",     Icons.Default.Settings,              Icons.Outlined.Settings)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
            val isPitchBlack = !isLight && (MaterialTheme.colorScheme.surface == Color(0xFF000000) || MaterialTheme.colorScheme.background == Color(0xFF000000))

            val navBarColor = when {
                isLight -> MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                isPitchBlack -> Color(0xF2050508)
                else -> Color(0xF212121A)
            }

            val navBorderBrush = if (isLight) {
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        Color.Black.copy(alpha = 0.08f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = if (isPitchBlack) 0.40f else 0.35f),
                        Color.White.copy(alpha = 0.06f)
                    )
                )
            }

            val navItemActiveColor = if (isLight) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            }

            val navUnselectedTint = if (isLight) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = navBarColor,
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, navBorderBrush),
                    shadowElevation = if (isLight) 10.dp else 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            val titleText = androidx.compose.ui.res.stringResource(item.titleRes)

                            val iconScale by animateFloatAsState(
                                targetValue = if (selected) 1.15f else 1.0f,
                                animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy),
                                label = "navIconScale"
                            )

                            Surface(
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) navItemActiveColor else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .padding(horizontal = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = titleText,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else navUnselectedTint,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                                    )
                                    AnimatedVisibility(
                                        visible = selected && !hideLabels,
                                        enter = fadeIn() + expandHorizontally(),
                                        exit = fadeOut() + shrinkHorizontally()
                                    ) {
                                        Row {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = titleText,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        // Draw background behind the translucent bar
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        @OptIn(ExperimentalSharedTransitionApi::class)
        SharedTransitionLayout {
            val topLevelRoutes = remember { setOf("playlist", "local", "direct_play", "settings") }
            NavHost(
                navController = navController,
                startDestination = initialTab ?: when (settingsState.defaultTab) {
                    "playlist", "local", "direct_play" -> settingsState.defaultTab
                    else -> "playlist"
                },
                modifier = Modifier.padding(innerPadding),
                enterTransition = { 
                    val initialRoute = initialState.destination.route
                    val targetRoute = targetState.destination.route
                    if (initialRoute in topLevelRoutes && targetRoute in topLevelRoutes) {
                        fadeIn(animationSpec = tween(150))
                    } else {
                        fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 8 },
                            animationSpec = tween(300)
                        )
                    }
                },
                exitTransition = { 
                    val initialRoute = initialState.destination.route
                    val targetRoute = targetState.destination.route
                    if (initialRoute in topLevelRoutes && targetRoute in topLevelRoutes) {
                        fadeOut(animationSpec = tween(150))
                    } else {
                        fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 8 },
                            animationSpec = tween(300)
                        )
                    }
                }
            ) {
                composable("playlist") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    PlaylistScreen(
                        viewModel = playlistViewModel,
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
                                putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_IS_VOD, channel.isVod)
                                putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_TYPE, channel.streamType ?: "")
                                channel.licenseUrl?.let { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_LICENSE_URL, it) }
                                channel.drmScheme?.let { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_SCHEME, it) }
                                if (channel.drmHeaders.isNotEmpty()) {
                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_HEADERS, java.util.HashMap(channel.drmHeaders))
                                }
                            }
                            context.startActivity(intent)
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
                composable("local") { LocalScreen() }
                composable("direct_play") {
                    com.codesrahul.streamplus.ui.home.DirectPlayScreen(
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate("playlist") {
                                    popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onNavigateToHistory = { navController.navigate("history") }
                    )
                }
                composable("history") {
                    com.codesrahul.streamplus.ui.home.HistoryScreen(onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate("playlist") {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    })
                }
                composable(
                    route = "playlist_detail/{playlistId}",
                    arguments = listOf(androidx.navigation.navArgument("playlistId") { type = androidx.navigation.NavType.StringType })
                ) {
                    com.codesrahul.streamplus.ui.playlist.PlaylistDetailScreen(
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate("playlist") {
                                    popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onNavigate = { route -> navController.navigate(route) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
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
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate("playlist") {
                                    popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onNavigate = { route -> navController.navigate(route) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
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
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate("playlist") {
                                    popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
                composable("settings") {
                    SettingsScreen(onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate("playlist") {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    })
                }
            }
        }
    }
}
