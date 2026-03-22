package com.codesrahul.streamplus.ui.util

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
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )

    this
        .scale(scale)
        .shadow(
            elevation = if (isFocused) 20.dp else 0.dp,
            shape = RoundedCornerShape(cornerRadius),
            ambientColor = borderColor.copy(alpha = 0.5f),
            spotColor = borderColor
        )
        .focusable(interactionSource = source)
        .then(
            if (isFocused) {
                Modifier.border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(cornerRadius)
                )
            } else {
                Modifier
            }
        )
}
