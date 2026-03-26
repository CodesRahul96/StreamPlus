@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.codesrahul.streamplus.ui.home

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.codesrahul.streamplus.ui.components.PremiumBackground
import com.codesrahul.streamplus.ui.player.PlayerActivity
import com.codesrahul.streamplus.util.UserAgentManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectPlayScreen(
    onBack: () -> Unit,
    onNavigateToHistory: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Core fields
    var streamUrl by remember { mutableStateOf("") }
    var streamTitle by remember { mutableStateOf("") }

    // Header fields
    var cookie    by remember { mutableStateOf("") }
    var referer   by remember { mutableStateOf("") }
    var origin    by remember { mutableStateOf("") }

    // UA
    var selectedUAName    by remember { mutableStateOf("Chrome") }
    var customUserAgent   by remember { mutableStateOf("") }

    // DRM
    var drmScheme     by remember { mutableStateOf("None") }
    var drmLicenseUrl by remember { mutableStateOf("") }

    // UI state
    var showUADialog      by remember { mutableStateOf(false) }
    var showDrmDialog     by remember { mutableStateOf(false) }
    var showAdvanced      by remember { mutableStateOf(false) }
    var urlError          by remember { mutableStateOf(false) }

    val advancedRotation by animateFloatAsState(
        targetValue = if (showAdvanced) 180f else 0f,
        animationSpec = tween(300), label = "chevron"
    )

    // The final resolved UA string
    val resolvedUA = UserAgentManager.getFullUserAgent(selectedUAName, customUserAgent)

    fun handlePaste(raw: String) {
        val (url, hdrs) = com.codesrahul.streamplus.util.UrlParser.parsePipedUrl(raw)
        streamUrl = url
        hdrs["User-Agent"]?.let { ua -> customUserAgent = ua; selectedUAName = "Custom" }
        hdrs["Referer"]?.let  { v  -> referer = v }
        hdrs["Cookie"]?.let   { v  -> cookie  = v }
        hdrs["Origin"]?.let   { v  -> origin  = v }
        hdrs["DRM-Scheme"]?.let { v ->
            drmScheme = when {
                v.contains("widevine", ignoreCase = true) -> "Widevine"
                v.contains("clearkey", ignoreCase = true) -> "ClearKey"
                v.contains("playready", ignoreCase = true) -> "PlayReady"
                else -> drmScheme
            }
        }
        hdrs["DRM-LicenseUrl"]?.let { v -> drmLicenseUrl = v }
        if (hdrs.any { (k, _) -> k != "User-Agent" }) showAdvanced = true
    }

    fun handlePlay() {
        if (streamUrl.isBlank()) { urlError = true; return }
        urlError = false
        val derivedTitle = streamTitle.takeIf { it.isNotBlank() } ?: try {
            java.net.URI(streamUrl).host?.removePrefix("www.") ?: streamUrl
        } catch (e: Exception) { streamUrl }

        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_STREAM_URL,   streamUrl)
            putExtra(PlayerActivity.EXTRA_STREAM_TITLE, derivedTitle)
            putExtra(PlayerActivity.EXTRA_USER_AGENT,   resolvedUA)
            if (referer.isNotBlank()) putExtra(PlayerActivity.EXTRA_REFERER, referer)
            if (cookie.isNotBlank())  putExtra(PlayerActivity.EXTRA_COOKIE,  cookie)
            if (origin.isNotBlank())  putExtra(PlayerActivity.EXTRA_ORIGIN,  origin)
            if (drmScheme != "None") {
                putExtra(PlayerActivity.EXTRA_DRM_SCHEME, drmScheme.lowercase())
                if (drmLicenseUrl.isNotBlank()) putExtra(PlayerActivity.EXTRA_DRM_LICENSE_URL, drmLicenseUrl)
            }
        }
        context.startActivity(intent)
    }

    PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Direct Stream",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    actions = {
                        IconButton(onClick = { onNavigateToHistory?.invoke() }) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = ::handlePlay,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor  = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(24.dp)) },
                    text = { Text("Play Stream", fontWeight = FontWeight.Bold) }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // ── Stream URL Card ──────────────────────────────────────────
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            "Source Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clip.primaryClip?.getItemAt(0)?.text?.toString()?.let { handlePaste(it) }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { raw ->
                            val (url, hdrs) = com.codesrahul.streamplus.util.UrlParser.parsePipedUrl(raw)
                            if (url != raw) {
                                streamUrl = url
                                hdrs["User-Agent"]?.let { ua -> customUserAgent = ua; selectedUAName = "Custom" }
                                hdrs["Referer"]?.let { v -> referer = v; showAdvanced = true }
                                hdrs["Cookie"]?.let  { v -> cookie  = v; showAdvanced = true }
                                hdrs["Origin"]?.let  { v -> origin  = v; showAdvanced = true }
                                hdrs["DRM-Scheme"]?.let { v ->
                                    drmScheme = when {
                                        v.contains("widevine", ignoreCase = true) -> "Widevine"
                                        v.contains("clearkey", ignoreCase = true) -> "ClearKey"
                                        v.contains("playready", ignoreCase = true) -> "PlayReady"
                                        else -> drmScheme
                                    }
                                }
                                hdrs["DRM-LicenseUrl"]?.let { v -> drmLicenseUrl = v; showAdvanced = true }
                            } else {
                                streamUrl = raw
                            }
                            if (urlError && raw.isNotBlank()) urlError = false
                        },
                        label = { Text("Stream URL") },
                        placeholder = { Text("https://example.com/playlist.m3u8", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)) },
                        isError = urlError,
                        supportingText = if (urlError) { { Text("Please enter a valid URL") } } else null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = streamTitle,
                        onValueChange = { streamTitle = it },
                        label = { Text("Display Title") },
                        placeholder = { Text("My Awesome Stream (Optional)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)) },
                        leadingIcon = { Icon(Icons.Default.Title, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary.copy(0.6f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                }

                // ── User Agent Card ──────────────────────────────────────────
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Devices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("User Agent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Compatibility mode: $selectedUAName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // UA Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf("Chrome", "Firefox", "Safari", "Mobile Chrome", "Smart TV").forEach { name ->
                            FilterChip(
                                selected = selectedUAName == name,
                                onClick = { selectedUAName = name },
                                label = { Text(name) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        ElevatedFilterChip(
                            selected = selectedUAName == "Custom",
                            onClick = { showUADialog = true },
                            label = { Text("Options…") },
                            leadingIcon = { Icon(Icons.Default.Tune, null, modifier = Modifier.size(14.dp)) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    
                    AnimatedVisibility(visible = selectedUAName == "Custom") {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = customUserAgent,
                                onValueChange = { customUserAgent = it },
                                label = { Text("Custom User Agent String") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }

                // ── Advanced Options ─────────────────────────────────────────
                SectionCard(onClick = { showAdvanced = !showAdvanced }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Advanced Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val summary = listOfNotNull(
                                "Referer".takeIf { referer.isNotBlank() },
                                "Cookie".takeIf  { cookie.isNotBlank()  },
                                "Origin".takeIf  { origin.isNotBlank()  },
                                "DRM: $drmScheme".takeIf { drmScheme != "None" }
                            ).joinToString(" · ").ifBlank { "Headers, DRM settings" }
                            Text(summary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.rotate(advancedRotation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = showAdvanced,
                        enter = expandVertically() + fadeIn(),
                        exit  = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(modifier = Modifier.alpha(0.3f))
                            Spacer(modifier = Modifier.height(4.dp))

                            AdvancedTextField(value = referer, onValueChange = { referer = it }, label = "Referer", icon = Icons.Default.OpenInNew)
                            AdvancedTextField(value = cookie, onValueChange = { cookie = it }, label = "Cookie", icon = Icons.Default.Cookie)
                            AdvancedTextField(value = origin, onValueChange = { origin = it }, label = "Origin", icon = Icons.Default.Public)

                            Divider(modifier = Modifier.alpha(0.3f).padding(vertical = 4.dp))

                            // DRM Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.2f))
                                    .clickable { showDrmDialog = true }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DRM Protection", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(drmScheme, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Icon(Icons.Default.ArrowForwardIos, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                            }

                            AnimatedVisibility(visible = drmScheme != "None") {
                                AdvancedTextField(
                                    value = drmLicenseUrl,
                                    onValueChange = { drmLicenseUrl = it },
                                    label = "DRM License URL",
                                    icon = Icons.Default.VpnKey
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────────────────
    if (showUADialog) {
        UserAgentSelectionDialog(
            currentSelection = selectedUAName,
            onDismiss = { showUADialog = false },
            onSelect  = { selectedUAName = it; showUADialog = false }
        )
    }
    if (showDrmDialog) {
        DrmSelectionDialog(
            currentSelection = drmScheme,
            onDismiss = { showDrmDialog = false },
            onSelect  = { drmScheme = it; showDrmDialog = false }
        )
    }
}

@Composable
fun SectionCard(
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = borderStroke()
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
)

@Composable
fun AdvancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)) },
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        singleLine = true,
        textStyle  = MaterialTheme.typography.bodySmall,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    )
}

@Composable
fun UserAgentSelectionDialog(
    currentSelection: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = borderStroke()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "Select Device Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .verticalScroll(rememberScrollState(0))
                        .weight(1f, fill = false)
                ) {
                    val allOptions = com.codesrahul.streamplus.util.UserAgentManager.presets.map { it.name } + "Custom"
                    allOptions.forEach { option ->
                        val preset = com.codesrahul.streamplus.util.UserAgentManager.presets.find { it.name == option }
                        val desc   = preset?.description ?: "Manually enter a custom UA string"
                        val isSelected = option == currentSelection
                        
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .selectable(
                                    selected = isSelected,
                                    onClick  = { onSelect(option) },
                                    role     = Role.RadioButton
                                )
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(0.3f) else Color.Transparent)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(option, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                Text(desc,   style = MaterialTheme.typography.labelSmall,  color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun DrmSelectionDialog(
    currentSelection: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val options = listOf("None", "Widevine", "ClearKey", "PlayReady")
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = borderStroke()
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text(
                    "Digital Rights (DRM)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                options.forEach { option ->
                    val isSelected = option == currentSelection
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = isSelected,
                                onClick  = { onSelect(option) },
                                role     = Role.RadioButton
                            )
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(0.3f) else Color.Transparent)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(option, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                            Text(
                                text = when (option) {
                                    "None"      -> "No protection required"
                                    "Widevine"  -> "DASH/HLS standards"
                                    "ClearKey"  -> "AES key exchange"
                                    "PlayReady" -> "Microsoft standard"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
