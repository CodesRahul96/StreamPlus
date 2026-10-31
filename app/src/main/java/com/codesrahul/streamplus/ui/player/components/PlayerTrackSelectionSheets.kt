package com.codesrahul.streamplus.ui.player.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoQualityBottomSheet(
    exoPlayer: ExoPlayer,
    onDismiss: () -> Unit
) {
    val videoTrackGroups = remember(exoPlayer.currentTracks) {
        exoPlayer.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
    }
    val currentOverrides = exoPlayer.trackSelectionParameters.overrides
    val isAuto = currentOverrides.none { it.value.type == C.TRACK_TYPE_VIDEO }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141E),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HighQuality, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Video Quality", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 8.dp))

            // Auto Option
            Surface(
                onClick = {
                    val newParams = exoPlayer.trackSelectionParameters.buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                        .build()
                    exoPlayer.trackSelectionParameters = newParams
                    onDismiss()
                },
                color = if (isAuto) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                border = if (isAuto) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Auto", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (isAuto) MaterialTheme.colorScheme.primary else Color.White)
                        Text("Adaptive bitrate streaming", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    if (isAuto) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Available Video Tracks
            videoTrackGroups.forEach { group ->
                for (i in 0 until group.length) {
                    val isSelected = group.isTrackSelected(i) && !isAuto
                    val format = group.getTrackFormat(i)
                    val resolution = if (format.width > 0 && format.height > 0) "${format.width}x${format.height}" else "Unknown"
                    val bitrateMb = if (format.bitrate > 0) String.format("%.1f Mbps", format.bitrate / 1_000_000f) else ""
                    val fps = if (format.frameRate > 0) "${format.frameRate.toInt()} fps" else ""
                    val label = listOfNotNull(resolution, fps, bitrateMb.takeIf { it.isNotEmpty() }).joinToString(" • ")

                    Surface(
                        onClick = {
                            val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(i))
                            val newParams = exoPlayer.trackSelectionParameters.buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                .addOverride(override)
                                .build()
                            exoPlayer.trackSelectionParameters = newParams
                            onDismiss()
                        },
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White)
                                format.codecs?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrackBottomSheet(
    exoPlayer: ExoPlayer,
    onDismiss: () -> Unit
) {
    val audioTrackGroups = remember(exoPlayer.currentTracks) {
        exoPlayer.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141E),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Audio Tracks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 8.dp))

            if (audioTrackGroups.isEmpty()) {
                Text(
                    "No additional audio tracks found",
                    color = Color.Gray,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                audioTrackGroups.forEach { group ->
                    for (i in 0 until group.length) {
                        val isSelected = group.isTrackSelected(i)
                        val format = group.getTrackFormat(i)
                        val lang = format.language?.uppercase() ?: "Unknown"
                        val label = format.label ?: lang

                        Surface(
                            onClick = {
                                val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(i))
                                val newParams = exoPlayer.trackSelectionParameters.buildUpon()
                                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                    .addOverride(override)
                                    .build()
                                exoPlayer.trackSelectionParameters = newParams
                                onDismiss()
                            },
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White)
                                    val channels = if (format.channelCount > 0) "${format.channelCount} Channels" else ""
                                    val sampleRate = if (format.sampleRate > 0) "${format.sampleRate / 1000} kHz" else ""
                                    Text(listOfNotNull(channels, sampleRate).joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
