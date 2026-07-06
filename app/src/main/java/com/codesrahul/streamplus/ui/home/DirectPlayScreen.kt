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
import com.codesrahul.streamplus.ui.util.tvFocusable
import androidx.compose.ui.res.stringResource
import com.codesrahul.streamplus.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectPlayScreen(
    onBack: () -> Unit,
    onNavigateToHistory: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val historyItems by historyViewModel.history.collectAsState(initial = emptyList())

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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            streamUrl = it.toString()
            streamTitle = it.lastPathSegment?.substringAfterLast('/') ?: "Local File"
        }
    }

    val directHistory = remember(historyItems) {
        historyItems.filter { it.playlistId == -1 }.sortedByDescending { it.timestamp }
    }

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
        var url = streamUrl.trim()
        if (!url.startsWith("http://", ignoreCase = true) && 
            !url.startsWith("https://", ignoreCase = true) && 
            !url.startsWith("rtmp://", ignoreCase = true) && 
            !url.startsWith("rtsp://", ignoreCase = true) && 
            !url.startsWith("file://", ignoreCase = true) && 
            !url.startsWith("content://", ignoreCase = true)) {
            url = "http://$url"
        }
        
        var finalDrmLicenseUrl = drmLicenseUrl.trim()
        if (finalDrmLicenseUrl.isNotEmpty() &&
            !finalDrmLicenseUrl.startsWith("http://", ignoreCase = true) && 
            !finalDrmLicenseUrl.startsWith("https://", ignoreCase = true)) {
            finalDrmLicenseUrl = "http://$finalDrmLicenseUrl"
        }

        urlError = false
        val derivedTitle = streamTitle.takeIf { it.isNotBlank() } ?: try {
            java.net.URI(url).host?.removePrefix("www.") ?: url
        } catch (e: Exception) { url }

        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_STREAM_URL,   url)
            putExtra(PlayerActivity.EXTRA_STREAM_TITLE, derivedTitle)
            putExtra(PlayerActivity.EXTRA_USER_AGENT,   resolvedUA)
            if (referer.isNotBlank()) putExtra(PlayerActivity.EXTRA_REFERER, referer)
            if (cookie.isNotBlank())  putExtra(PlayerActivity.EXTRA_COOKIE,  cookie)
            if (origin.isNotBlank())  putExtra(PlayerActivity.EXTRA_ORIGIN,  origin)
            if (drmScheme != "None") {
                putExtra(PlayerActivity.EXTRA_DRM_SCHEME, drmScheme.lowercase())
                if (finalDrmLicenseUrl.isNotBlank()) putExtra(PlayerActivity.EXTRA_DRM_LICENSE_URL, finalDrmLicenseUrl)
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
                            stringResource(R.string.direct_stream_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    windowInsets = WindowInsets(0.dp),
                    actions = {
                        IconButton(onClick = { onNavigateToHistory?.invoke() }) {
                            Icon(Icons.Default.History, contentDescription = stringResource(R.string.history_btn))
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
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play_stream_btn), modifier = Modifier.size(24.dp)) },
                    text = { Text(stringResource(R.string.play_stream_btn), fontWeight = FontWeight.Bold) }
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
                            stringResource(R.string.source_details_sec),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Paste Button
                            IconButton(
                                onClick = {
                                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clip.primaryClip?.getItemAt(0)?.text?.toString()?.let { handlePaste(it) }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            // File Picker Button
                            IconButton(
                                onClick = {
                                    filePickerLauncher.launch("video/*")
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Choose File", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            // Clear Form Button
                            IconButton(
                                onClick = {
                                    streamUrl = ""
                                    streamTitle = ""
                                    cookie = ""
                                    referer = ""
                                    origin = ""
                                    selectedUAName = "Chrome"
                                    customUserAgent = ""
                                    drmScheme = "None"
                                    drmLicenseUrl = ""
                                    showAdvanced = false
                                    urlError = false
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Form", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            }
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
                        label = { Text(stringResource(R.string.stream_url_label)) },
                        placeholder = { Text(stringResource(R.string.stream_url_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)) },
                        isError = urlError,
                        supportingText = if (urlError) { { Text(stringResource(R.string.invalid_url_error)) } } else null,
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
                        label = { Text(stringResource(R.string.display_title_label)) },
                        placeholder = { Text(stringResource(R.string.display_title_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)) },
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
                            Text(stringResource(R.string.user_agent_sec), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = stringResource(R.string.compatibility_mode_desc, selectedUAName),
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
                            label = { Text(stringResource(R.string.options_btn)) },
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
                                label = { Text(stringResource(R.string.custom_ua_label)) },
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
                                Text(stringResource(R.string.advanced_options_sec), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                val summary = listOfNotNull(
                                    stringResource(R.string.referer_label).takeIf { referer.isNotBlank() },
                                    stringResource(R.string.cookie_label).takeIf  { cookie.isNotBlank()  },
                                    stringResource(R.string.origin_label).takeIf  { origin.isNotBlank()  },
                                    "DRM: $drmScheme".takeIf { drmScheme != "None" }
                                ).joinToString(" · ").ifBlank { stringResource(R.string.advanced_options_default_desc) }
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
                            HorizontalDivider(modifier = Modifier.alpha(0.3f))
                            Spacer(modifier = Modifier.height(4.dp))

                            AdvancedTextField(value = referer, onValueChange = { referer = it }, label = "Referer", icon = Icons.Default.OpenInNew)
                            AdvancedTextField(value = cookie, onValueChange = { cookie = it }, label = "Cookie", icon = Icons.Default.Cookie)
                            AdvancedTextField(value = origin, onValueChange = { origin = it }, label = "Origin", icon = Icons.Default.Public)

                            HorizontalDivider(modifier = Modifier.alpha(0.3f).padding(vertical = 4.dp))

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
                                    Text(stringResource(R.string.drm_protection_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(drmScheme, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Icon(Icons.Default.ArrowForwardIos, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                            }

                            AnimatedVisibility(visible = drmScheme != "None") {
                                AdvancedTextField(
                                    value = drmLicenseUrl,
                                    onValueChange = { drmLicenseUrl = it },
                                    label = stringResource(R.string.drm_license_url_label),
                                    icon = Icons.Default.VpnKey
                                )
                            }
                        }
                    }
                }

                // ── Recent Streams Card ──────────────────────────────────────
                if (directHistory.isNotEmpty()) {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                "Recent Streams",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            directHistory.take(5).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                                        .clickable {
                                            // POPULATE ALL EDITABLE FIELDS
                                            streamUrl = item.url
                                            streamTitle = item.name
                                            cookie = item.cookie ?: ""
                                            referer = item.referer ?: ""
                                            origin = item.origin ?: ""
                                            drmScheme = item.drmScheme?.let { scheme ->
                                                when {
                                                    scheme.contains("widevine", ignoreCase = true) -> "Widevine"
                                                    scheme.contains("clearkey", ignoreCase = true) -> "ClearKey"
                                                    scheme.contains("playready", ignoreCase = true) -> "PlayReady"
                                                    else -> "None"
                                                }
                                            } ?: "None"
                                            drmLicenseUrl = item.licenseUrl ?: ""
                                            
                                            val ua = item.userAgent ?: ""
                                            if (ua.isNotEmpty()) {
                                                val preset = com.codesrahul.streamplus.util.UserAgentManager.presets.find { 
                                                    com.codesrahul.streamplus.util.UserAgentManager.getFullUserAgent(it.name) == ua 
                                                }
                                                if (preset != null) {
                                                    selectedUAName = preset.name
                                                    customUserAgent = ""
                                                } else {
                                                    selectedUAName = "Custom"
                                                    customUserAgent = ua
                                                }
                                            } else {
                                                selectedUAName = "Chrome"
                                                customUserAgent = ""
                                            }
                                            
                                            if (cookie.isNotBlank() || referer.isNotBlank() || origin.isNotBlank() || drmScheme != "None") {
                                                showAdvanced = true
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.url,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    // Play icon to play immediately
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(context, PlayerActivity::class.java).apply {
                                                putExtra(PlayerActivity.EXTRA_STREAM_URL,   item.url)
                                                putExtra(PlayerActivity.EXTRA_STREAM_TITLE, item.name.ifBlank { "Unknown Stream" })
                                                item.logoUrl?.let   { putExtra(PlayerActivity.EXTRA_CHANNEL_LOGO, it) }
                                                item.userAgent?.let { putExtra(PlayerActivity.EXTRA_USER_AGENT,   it) }
                                                item.cookie?.let    { putExtra(PlayerActivity.EXTRA_COOKIE,       it) }
                                                item.referer?.let   { putExtra(PlayerActivity.EXTRA_REFERER,      it) }
                                                item.origin?.let    { putExtra(PlayerActivity.EXTRA_ORIGIN,       it) }
                                                item.licenseUrl?.let{ putExtra(PlayerActivity.EXTRA_DRM_LICENSE_URL, it) }
                                                item.drmScheme?.let { putExtra(PlayerActivity.EXTRA_DRM_SCHEME,   it) }
                                                putExtra(PlayerActivity.EXTRA_PLAYLIST_ID, item.playlistId)
                                                putExtra(PlayerActivity.EXTRA_STREAM_GROUP, item.group)
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    
                                    // Delete icon
                                    IconButton(
                                        onClick = {
                                            historyViewModel.deleteHistory(item)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
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
    val isPitchBlack = MaterialTheme.colorScheme.surface == Color(0xFF000000)
    Card(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (isPitchBlack) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
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
    val isPitchBlack = MaterialTheme.colorScheme.surface == Color(0xFF000000)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = if (isPitchBlack) 0.dp else 6.dp,
            border = borderStroke()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.select_device_profile_title),
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
                        val desc   = preset?.description ?: stringResource(R.string.custom_ua_dialog_desc)
                        val isSelected = option == currentSelection
                        
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .tvFocusable(cornerRadius = 12.dp)
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
                        Text(stringResource(R.string.cancel_btn), fontWeight = FontWeight.SemiBold)
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
    val isPitchBlack = MaterialTheme.colorScheme.surface == Color(0xFF000000)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = if (isPitchBlack) 0.dp else 6.dp,
            border = borderStroke()
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text(
                    stringResource(R.string.drm_title),
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
                            .tvFocusable(cornerRadius = 12.dp)
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
                                    "None"      -> stringResource(R.string.drm_desc_none)
                                    "Widevine"  -> stringResource(R.string.drm_desc_widevine)
                                    "ClearKey"  -> stringResource(R.string.drm_desc_clearkey)
                                    "PlayReady" -> stringResource(R.string.drm_desc_playready)
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
                    Text(stringResource(R.string.close_btn), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
