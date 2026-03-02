package com.codesrahul.streamplus.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val scrollState = rememberScrollState()
    
    com.codesrahul.streamplus.ui.components.PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { 
                TopAppBar(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    title = { 
                        Text(
                            "StreamPlus",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    actions = {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            IconButton(onClick = { onNavigate("settings") }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                ) 
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Welcome back,",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    "Your ultimate streaming companion.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                val cardItems = listOf(
                    Triple("My Playlists", "Browse and watch remote M3U lists", "playlist"),
                    Triple("Local Files", "Play videos from your device storage", "local"),
                    Triple("Direct Stream", "Stream directly from URL with headers", "direct_play"),
                    Triple("Watch History", "Resume recently played streams", "history")
                )

                cardItems.forEachIndexed { index, (title, sub, route) ->
                    val icon = when(route) {
                        "playlist" -> Icons.Default.PlayArrow
                        "local" -> Icons.Default.VideoLibrary
                        "direct_play" -> Icons.Default.Link
                        "history" -> Icons.Default.History
                        else -> Icons.Default.Folder
                    }
                    HomeDashboardCard(
                        title = title,
                        subtitle = sub,
                        icon = icon,
                        delay = index * 80,
                        onClick = { onNavigate(route) }
                    )
                }
                
                Spacer(modifier = Modifier.height(100.dp)) // Extra padding for floating nav bar
            }
        }
    }
}

@Composable
fun HomeDashboardCard(
    title: String, 
    subtitle: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    delay: Int,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { 50 })
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        title, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        subtitle, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

