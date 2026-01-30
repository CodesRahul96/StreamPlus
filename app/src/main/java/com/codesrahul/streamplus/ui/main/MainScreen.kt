package com.codesrahul.streamplus.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.codesrahul.streamplus.ui.home.HomeScreen
import com.codesrahul.streamplus.ui.local.LocalScreen
import com.codesrahul.streamplus.ui.playlist.PlaylistScreen
import com.codesrahul.streamplus.ui.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Local : Screen("local", "Local", Icons.Filled.VideoLibrary)
    object Playlist : Screen("playlist", "Playlist", Icons.Filled.List)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Home,
        Screen.Local,
        Screen.Playlist,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { dest -> dest.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { 
                HomeScreen(onNavigate = { route -> navController.navigate(route) }) 
            }
            composable("direct_play") {
                com.codesrahul.streamplus.ui.home.DirectPlayScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Local.route) { LocalScreen() }
            composable(Screen.Playlist.route) { 
                PlaylistScreen(
                    onPlaylistClick = { playlistId ->
                        navController.navigate("playlist_detail/$playlistId")
                    }
                ) 
            }
            composable(
                route = "playlist_detail/{playlistId}",
                arguments = listOf(androidx.navigation.navArgument("playlistId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                com.codesrahul.streamplus.ui.playlist.PlaylistDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(
                route = "playlist_url_detail/{playlistId}/{name}/{userAgent}",
                arguments = listOf(
                    androidx.navigation.navArgument("playlistId") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("name") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("userAgent") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                com.codesrahul.streamplus.ui.playlist.PlaylistDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(Screen.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
