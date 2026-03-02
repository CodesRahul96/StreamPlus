package com.codesrahul.streamplus.ui.home

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.codesrahul.streamplus.data.local.HistoryEntity
import com.codesrahul.streamplus.ui.components.PremiumBackground
import com.codesrahul.streamplus.ui.player.PlayerActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyItems by viewModel.history.collectAsState(initial = emptyList())
    val context = LocalContext.current

    PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Watch History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (historyItems.isNotEmpty()) {
                                Text(
                                    "${historyItems.size} items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (historyItems.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearAllHistory() }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (historyItems.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "Nothing watched yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Your stream history will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyItems, key = { it.url + it.timestamp }) { item ->
                        HistoryItem(
                            item = item,
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
                            onDelete = { viewModel.deleteHistory(item) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItem(
    item: HistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (item.duration > 0 && item.lastPosition > 0) {
        (item.lastPosition.toFloat() / item.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val posMin = item.lastPosition / 60000
    val posSec = (item.lastPosition % 60000) / 1000
    val durMin = item.duration / 60000
    val durSec = (item.duration % 60000) / 1000

    val timeLabel = if (item.duration > 0)
        "%02d:%02d / %02d:%02d".format(posMin, posSec, durMin, durSec)
    else "Live"

    val isLive = item.duration <= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onDelete),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = if (progress > 0) 8.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play icon container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isLive) Icons.Default.RadioButtonChecked else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name.ifBlank { "Unknown Stream" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "● LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Progress bar — only for non-live streams with actual progress
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
