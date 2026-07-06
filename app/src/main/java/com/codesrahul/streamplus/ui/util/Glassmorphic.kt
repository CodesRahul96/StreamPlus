package com.codesrahul.streamplus.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A custom modifier to apply a premium glassmorphic frosted-glass appearance.
 * To keep child texts and icons crisp and readable, it uses a translucent 
 * background fill and vertical gradient border highlights, rather than blurring the content layer.
 */
fun Modifier.glassmorphic(
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp
): Modifier = composed {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isPitchBlack = surfaceColor == Color(0xFF000000)
    val isDark = isPitchBlack || (surfaceColor.red + surfaceColor.green + surfaceColor.blue) < 1.2f
                 
    val backgroundColor = if (isPitchBlack) {
        Color(0xFF0B0B0C)
    } else if (isDark) {
        Color(0xFF161619).copy(alpha = 0.7f)
    } else {
        Color.White.copy(alpha = 0.8f)
    }
    
    val borderColorStart = if (isPitchBlack) {
        primaryColor.copy(alpha = 0.18f)
    } else if (isDark) {
        primaryColor.copy(alpha = 0.3f)
    } else {
        primaryColor.copy(alpha = 0.45f)
    }

    val borderColorEnd = if (isPitchBlack) {
        Color.White.copy(alpha = 0.03f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    Modifier
        .clip(RoundedCornerShape(cornerRadius))
        .background(backgroundColor, RoundedCornerShape(cornerRadius))
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColorStart,
                    borderColorEnd
                )
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
}
