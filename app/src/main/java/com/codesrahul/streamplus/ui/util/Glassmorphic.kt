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

import androidx.compose.runtime.Composable

/**
 * A custom modifier to apply a premium glassmorphic frosted-glass appearance.
 * To keep child texts and icons crisp and readable, it uses a translucent 
 * background fill and vertical gradient border highlights, rather than blurring the content layer.
 */
@Composable
fun Modifier.glassmorphic(
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    backgroundAlpha: Float? = null,
    accentGlowColor: Color? = null
): Modifier {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isPitchBlack = surfaceColor == Color(0xFF000000)
    val isDark = isPitchBlack || (surfaceColor.red + surfaceColor.green + surfaceColor.blue) < 1.2f
                 
    val defaultAlpha = backgroundAlpha ?: if (isPitchBlack) 0.85f else if (isDark) 0.72f else 0.85f

    val backgroundColor = if (isPitchBlack) {
        Color(0xFF0C0C12).copy(alpha = defaultAlpha)
    } else if (isDark) {
        Color(0xFF14141E).copy(alpha = defaultAlpha)
    } else {
        Color.White.copy(alpha = defaultAlpha)
    }
    
    val borderColorStart = accentGlowColor ?: if (isPitchBlack) {
        primaryColor.copy(alpha = 0.28f)
    } else if (isDark) {
        primaryColor.copy(alpha = 0.38f)
    } else {
        primaryColor.copy(alpha = 0.5f)
    }

    val borderColorEnd = if (isPitchBlack) {
        Color.White.copy(alpha = 0.04f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.07f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    return this
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
