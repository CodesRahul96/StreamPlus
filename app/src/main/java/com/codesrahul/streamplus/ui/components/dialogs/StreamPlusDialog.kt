package com.codesrahul.streamplus.ui.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Standard enterprise-grade Dialog component for StreamPlus.
 * Guarantees solid opaque background (no see-through bleeding),
 * elegant rounded borders, shadow elevation, and responsive dark/light theme styling.
 */
@Composable
fun StreamPlusDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 24.dp,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isPitchBlack = surfaceColor == Color(0xFF000000)
    val isDark = isPitchBlack || (surfaceColor.red + surfaceColor.green + surfaceColor.blue) < 1.2f

    val dialogBg = when {
        isPitchBlack -> Color(0xFF0D0E15)
        isDark -> Color(0xFF161B26)
        else -> surfaceColor
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Surface(
            shape = RoundedCornerShape(cornerRadius),
            color = dialogBg,
            shadowElevation = elevation,
            border = BorderStroke(1.dp, borderColor),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                content = content
            )
        }
    }
}
