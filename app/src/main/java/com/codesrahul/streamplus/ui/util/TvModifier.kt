package com.codesrahul.streamplus.ui.util

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A custom modifier to add visual focus indication for Android TV (D-pad navigation).
 * Enhanced with scale animation and subtle glow.
 */
fun Modifier.tvFocusable(
    borderWidth: Dp = 3.dp,
    borderColor: Color = Color.White,
    cornerRadius: Dp = 16.dp, 
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by source.collectIsFocusedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "scale"
    )
    
    val shadowElevation by animateDpAsState(
        targetValue = if (isFocused) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "shadowElevation"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "borderAlpha"
    )

    this
        .scale(scale)
        .shadow(
            elevation = shadowElevation,
            shape = RoundedCornerShape(cornerRadius),
            ambientColor = borderColor.copy(alpha = 0.3f * borderAlpha),
            spotColor = borderColor.copy(alpha = borderAlpha)
        )
        .focusable(interactionSource = source)
        .border(
            width = borderWidth,
            color = borderColor.copy(alpha = borderAlpha),
            shape = RoundedCornerShape(cornerRadius)
        )
}
