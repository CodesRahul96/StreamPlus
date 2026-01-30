package com.codesrahul.streamplus.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.codesrahul.streamplus.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onPlaylistClick: (Int) -> Unit,
    viewModel: PlaylistViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<com.codesrahul.streamplus.data.local.PlaylistEntity?>(null) }
    val playlists by viewModel.playlists.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlist") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri: android.net.Uri? ->
                        uri?.let { viewModel.importLocalPlaylist(context, it) }
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add URL") },
                            onClick = { 
                                showMenu = false 
                                showAddDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import Local M3U") },
                            onClick = { 
                                showMenu = false 
                                filePickerLauncher.launch("*/*") // Or application/octet-stream/vnd.apple.mpegurl
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No playlist found",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add a playlist to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Text("Add Playlist")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id) },
                        onEdit = { showEditDialog = playlist }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        PlaylistDialog(
            title = "Add Playlist URL",
            confirmButtonText = "Add",
            onConfirm = { name, url, userAgent ->
                viewModel.addPlaylist(name, url, userAgent)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    showEditDialog?.let { playlist ->
        PlaylistDialog(
            title = "Edit Playlist details",
            initialName = playlist.name,
            initialUrl = playlist.url,
            initialUserAgent = playlist.userAgent,
            confirmButtonText = "Update",
            onConfirm = { name, url, userAgent ->
                viewModel.updatePlaylist(playlist.copy(name = name, url = url, userAgent = userAgent))
                showEditDialog = null
            },
            onDismiss = { showEditDialog = null },
            onDelete = {
                viewModel.deletePlaylist(playlist)
                showEditDialog = null
            }
        )
    }
}

@Composable
fun PlaylistItem(
    playlist: com.codesrahul.streamplus.data.local.PlaylistEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val settingsRepository = hiltViewModel<SettingsViewModel>().settings.collectAsState()
    val hideUri = settingsRepository.value.hidePlaylistUri

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (!hideUri) {
                    Text(
                        text = playlist.url,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDialog(
    title: String,
    initialName: String = "",
    initialUrl: String = "",
    initialUserAgent: String = "Auto",
    confirmButtonText: String,
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val UA_MAP = mapOf(
        "Auto" to "Auto",
        "Default" to "ExoPlayerLib/2.19.1",
        "Chrome(Android)" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Chrome(PC)" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "IE(PC)" to "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko",
        "Firefox(PC)" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
        "iPhone" to "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1",
        "Nokia" to "Nokia5230/5.0 (SymbianOS/9.4; U; Series60/5.0 Mozilla/5.0; Profile/MIDP-2.1 Configuration/CLDC-1.1 ) AppleWebkit/525 (KHTML, like Gecko) Safari/525",
        "ExoPlayer" to "ExoPlayerLib/2.19.1"
    )

    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }
    // Store the display value for the dropdown, but we will pass the mapped value on confirm
    var userAgentDisplay by remember { mutableStateOf(
        when (initialUserAgent) {
            "Auto" -> "Auto (Inbuilt)"
            UA_MAP["Default"] -> "Default"
            else -> UA_MAP.entries.find { it.value == initialUserAgent }?.key ?: "Custom"
        }
    ) }
    var userAgentValue by remember { mutableStateOf(initialUserAgent.ifEmpty { UA_MAP["Default"]!! }) }
    var isUserAgentExpanded by remember { mutableStateOf(false) }

    val userAgentOptions = listOf(
        "Auto (Inbuilt)",
        "Default", 
        "Chrome(Android)", 
        "Chrome(PC)", 
        "IE(PC)", 
        "Firefox(PC)", 
        "iPhone", 
        "Nokia", 
        "Custom"
    )
    
    var showCustomUADialog by remember { mutableStateOf(false) }
    var customUAInput by remember { mutableStateOf("") }

    if (showCustomUADialog) {
        AlertDialog(
            onDismissRequest = { showCustomUADialog = false },
            title = { Text("Custom UserAgent") },
            text = {
                OutlinedTextField(
                    value = customUAInput,
                    onValueChange = { customUAInput = it },
                    label = { Text("Enter UserAgent string") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    userAgentValue = customUAInput
                    userAgentDisplay = "Custom"
                    showCustomUADialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomUADialog = false }) { Text("Cancel") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Enter playlist name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Enter playlist url") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(
                    expanded = isUserAgentExpanded,
                    onExpandedChange = { isUserAgentExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = userAgentDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("UserAgent") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUserAgentExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isUserAgentExpanded,
                        onDismissRequest = { isUserAgentExpanded = false }
                    ) {
                        userAgentOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    if (option == "Auto (Inbuilt)") {
                                        userAgentDisplay = "Auto (Inbuilt)"
                                        userAgentValue = "Auto"
                                    } else if (option == "Custom") {
                                        customUAInput = userAgentValue
                                        showCustomUADialog = true
                                    } else {
                                        userAgentDisplay = option
                                        userAgentValue = UA_MAP[option] ?: option
                                    }
                                    isUserAgentExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, url, userAgentValue) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete")
                    }
                }
            }
        }
    )
}
