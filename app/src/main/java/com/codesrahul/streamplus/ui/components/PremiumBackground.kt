package com.codesrahul.streamplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun PremiumBackground(
    isSplash: Boolean = false,
    content: @Composable () -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background
    
    // Create a very subtle, elegant gradient based on the exact theme background
    // This allows the Glassmorphism surfaces to float perfectly over it
    val gradientColors = listOf(
        bgColor,
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
        bgColor
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .background(Brush.verticalGradient(gradientColors))
    ) {
        content()
    }
}
