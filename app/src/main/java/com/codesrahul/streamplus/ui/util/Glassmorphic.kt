package com.codesrahul.streamplus.ui.util

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A custom modifier to apply a premium glassmorphic frosted-glass effect.
 * Uses real RenderEffect backdrop blur on Android 12+ (API 31+) and translucent overlay fallback.
 */
fun Modifier.glassmorphic(
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    blurRadius: Float = 20f
): Modifier = composed {
    val isDark = (MaterialTheme.colorScheme.surface.red + 
                  MaterialTheme.colorScheme.surface.green + 
                  MaterialTheme.colorScheme.surface.blue) < 1.2f
                 
    val backgroundColor = if (isDark) {
        Color(0xFF1E1E1E).copy(alpha = 0.35f)
    } else {
        Color.White.copy(alpha = 0.55f)
    }
    
    val borderColorStart = if (isDark) {
        Color.White.copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.40f)
    }

    val borderColorEnd = if (isDark) {
        Color.White.copy(alpha = 0.02f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }

    val baseModifier = Modifier
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

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        baseModifier.then(
            Modifier.graphicsLayer {
                clip = true
                shape = RoundedCornerShape(cornerRadius)
                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                    blurRadius, blurRadius,
                    android.graphics.Shader.TileMode.CLAMP
                ).asComposeRenderEffect()
            }
        )
    } else {
        baseModifier
    }
}
