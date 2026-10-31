package com.codesrahul.streamplus.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies a frosted-glass / glassmorphism style overlay to a composable.
 * Uses a translucent white/dark fill + rounded corners to simulate the effect.
 */
fun Modifier.glassy(
    cornerRadius: Dp = 16.dp,
    fillColor: Color = Color.White.copy(alpha = 0.10f),
    strokeColor: Color = Color.White.copy(alpha = 0.18f)
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(fillColor)
    .border(1.dp, strokeColor, RoundedCornerShape(cornerRadius))
