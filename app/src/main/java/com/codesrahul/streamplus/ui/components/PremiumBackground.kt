package com.codesrahul.streamplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

@Composable
fun PremiumBackground(
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val secondary = MaterialTheme.colorScheme.secondary

    // Three-stop diagonal gradient for a premium, alive feel
    val gradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to primary.copy(alpha = if (isDark) 0.18f else 0.10f),
            0.45f to surface,
            1.0f to secondary.copy(alpha = if (isDark) 0.12f else 0.06f),
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surface)          // solid base keeps contrast
            .background(gradient)         // gradient sits on top
    ) {
        content()
    }
}
