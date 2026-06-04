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
    val isDark = (MaterialTheme.colorScheme.surface.red + 
                  MaterialTheme.colorScheme.surface.green + 
                  MaterialTheme.colorScheme.surface.blue) < 1.2f
                 
    val backgroundColor = if (isDark) {
        Color(0xFF18181B).copy(alpha = 0.65f)
    } else {
        Color.White.copy(alpha = 0.75f)
    }
    
    val borderColorStart = if (isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.White.copy(alpha = 0.45f)
    }

    val borderColorEnd = if (isDark) {
        Color.White.copy(alpha = 0.03f)
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
