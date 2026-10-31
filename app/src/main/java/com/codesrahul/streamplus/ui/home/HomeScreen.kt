package com.codesrahul.streamplus.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codesrahul.streamplus.ui.components.PremiumBackground
import com.codesrahul.streamplus.ui.util.tvFocusable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val scrollState = rememberScrollState()
    
    PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { 
                TopAppBar(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    title = { 
                        Column {
                            Text(
                                "StreamPlus",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Premium IPTV & Media Player",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    actions = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .tvFocusable(cornerRadius = 50.dp)
                        ) {
                            IconButton(onClick = { onNavigate("settings") }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.primary
                                )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    "Your ultimate streaming companion.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val cardItems = listOf(
                    Triple("My Playlists", "Browse and watch remote M3U lists", "playlist"),
                    Triple("Local Files", "Play videos from your device storage", "local"),
                    Triple("Direct Stream", "Stream directly from URL with headers", "direct_play"),
                    Triple("Watch History", "Resume recently played streams", "history")
                )

                cardItems.forEachIndexed { index, (title, sub, route) ->
                    val (icon, accentColor) = when(route) {
                        "playlist" -> Pair(Icons.Default.PlayArrow, Color(0xFF0284C7))
                        "local" -> Pair(Icons.Default.VideoLibrary, Color(0xFF10B981))
                        "direct_play" -> Pair(Icons.Default.Link, Color(0xFF8B5CF6))
                        "history" -> Pair(Icons.Default.History, Color(0xFFF59E0B))
                        else -> Pair(Icons.Default.Folder, Color(0xFF0284C7))
                    }
                    HomeDashboardCard(
                        title = title,
                        subtitle = sub,
                        icon = icon,
                        accentColor = accentColor,
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
    accentColor: Color,
    delay: Int,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }

    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val cardBg = if (isLight) Color(0xF5FFFFFF) else Color(0xF0121624)
    val borderBrush = if (isLight) {
        Brush.verticalGradient(
            listOf(
                accentColor.copy(alpha = 0.35f),
                Color.Black.copy(alpha = 0.06f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                accentColor.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .tvFocusable(cornerRadius = 20.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderBrush)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored icon podium
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.25f),
                                    accentColor.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = accentColor
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        subtitle, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Trailing action indicator
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLight) 0.06f else 0.10f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLight) 0.6f else 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
