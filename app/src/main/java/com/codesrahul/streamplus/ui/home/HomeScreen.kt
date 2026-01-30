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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { 
                TopAppBar(
                    title = { 
                        Text(
                            "StreamPlus",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                    Triple("Local Media", "Play videos from your device", "local"),
                    Triple("Direct Play", "Play URL with custom headers", "direct_play")
                )

                cardItems.forEachIndexed { index, (title, sub, route) ->
                    val icon = when(route) {
                        "playlist" -> Icons.Default.PlayArrow
                        "local" -> Icons.Default.VideoLibrary
                        "direct_play" -> Icons.Default.Link
                        else -> Icons.Default.Folder
                    }
                    HomeDashboardCard(
                        title = title,
                        subtitle = sub,
                        icon = icon,
                        delay = index * 100,
                        onClick = { onNavigate(route) }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
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
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
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
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        title, 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        subtitle, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectPlayScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var streamUrl by remember { mutableStateOf("") }
    var cookie by remember { mutableStateOf("") }
    var referer by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf("") }
    var drmLicenseUrl by remember { mutableStateOf("") }
    var userAgent by remember { mutableStateOf("Default") }
    var drmScheme by remember { mutableStateOf("clearkey") }
    var isUserAgentExpanded by remember { mutableStateOf(false) }
    var isDrmExpanded by remember { mutableStateOf(false) }

    val UA_MAP = mapOf(
        "Default" to "ExoPlayerLib/2.19.1",
        "Chrome(Android)" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Chrome(PC)" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "iPhone" to "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Direct Play") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        streamUrl = ""
                        cookie = ""
                        referer = ""
                        origin = ""
                        drmLicenseUrl = ""
                        userAgent = "Default"
                        drmScheme = "clearkey"
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (streamUrl.isNotBlank()) {
                        val finalUA = UA_MAP[userAgent] ?: userAgent
                        val intent = android.content.Intent(context, com.codesrahul.streamplus.ui.player.PlayerActivity::class.java).apply {
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_URL, streamUrl)
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_USER_AGENT, finalUA)
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_REFERER, referer)
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_COOKIE, cookie)
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_ORIGIN, origin)
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_LICENSE_URL, drmLicenseUrl)
                            putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_SCHEME, drmScheme)
                        }
                        context.startActivity(intent)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = streamUrl,
                onValueChange = { streamUrl = it },
                label = { Text("Media Stream URL") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://example.com/stream.m3u8") },
                trailingIcon = {
                    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                    IconButton(onClick = {
                        val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                        if (clip != null) streamUrl = clip
                    }) {
                        Icon(androidx.compose.material.icons.Icons.Default.ContentPaste, contentDescription = "Paste")
                    }
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = cookie,
                onValueChange = { cookie = it },
                label = { Text("Cookie Value") },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = referer,
                onValueChange = { referer = it },
                label = { Text("Referer Value") },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = origin,
                onValueChange = { origin = it },
                label = { Text("Origin Value") },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = drmLicenseUrl,
                onValueChange = { drmLicenseUrl = it },
                label = { Text("DRM License URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                placeholder = { Text("License URL (Optional)") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Agent Dropdown
                ExposedDropdownMenuBox(
                    expanded = isUserAgentExpanded,
                    onExpandedChange = { isUserAgentExpanded = !isUserAgentExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = userAgent,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("UserAgent") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUserAgentExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isUserAgentExpanded,
                        onDismissRequest = { isUserAgentExpanded = false }
                    ) {
                        listOf("Default", "Chrome(Android)", "Chrome(PC)", "iPhone", "Custom").forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    userAgent = item
                                    isUserAgentExpanded = false
                                }
                            )
                        }
                    }
                }

                // DRM Scheme Dropdown
                ExposedDropdownMenuBox(
                    expanded = isDrmExpanded,
                    onExpandedChange = { isDrmExpanded = !isDrmExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = drmScheme,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("DrmScheme") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDrmExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isDrmExpanded,
                        onDismissRequest = { isDrmExpanded = false }
                    ) {
                        listOf("clearkey", "widevine", "playready").forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    drmScheme = item
                                    isDrmExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp)) // Clearance for FAB
        }
    }
}
