package com.codesrahul.streamplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codesrahul.streamplus.ui.theme.ElectricIndigo
import com.codesrahul.streamplus.ui.theme.NeonCyan
import com.codesrahul.streamplus.ui.util.glassmorphic
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionBox(
    permissionState: PermissionState,
    description: String,
    content: @Composable () -> Unit
) {
    if (permissionState.status.isGranted) {
        content()
    } else {
        PermissionLayout(
            title = "Device Storage Access",
            description = description,
            onRequest = { permissionState.launchPermissionRequest() }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionBox(
    permissionState: MultiplePermissionsState,
    description: String,
    content: @Composable () -> Unit
) {
    val anyGranted = permissionState.permissions.any { it.status.isGranted }
    val allGranted = permissionState.permissions.all { it.status.isGranted }
    
    // If either video/audio is granted on newer Android, allow browsing available media
    if (allGranted || anyGranted) {
        content()
    } else {
        PermissionLayout(
            title = "Media Storage Access",
            description = description,
            onRequest = { permissionState.launchMultiplePermissionRequest() }
        )
    }
}

@Composable
private fun PermissionLayout(
    title: String,
    description: String,
    onRequest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .glassmorphic(cornerRadius = 28.dp, accentGlowColor = NeonCyan.copy(alpha = 0.2f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.5f), ElectricIndigo.copy(alpha = 0.3f))
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glowing Icon Badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NeonCyan.copy(alpha = 0.25f), ElectricIndigo.copy(alpha = 0.25f))
                            )
                        )
                        .border(1.5.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PermMedia,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = description.ifBlank {
                        "Grant permission to allow StreamPlus Studio to index and play your high-definition offline videos and lossless audio."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(ElectricIndigo, NeonCyan)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Grant Storage Access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
