package com.codesrahul.streamplus.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Modern curated Accent Colors for StreamPlus.
 * Ocean Blue is the default brand color.
 */
enum class AccentColor(
    val id: String,
    val title: String,
    val lightPrimary: Color,
    val darkPrimary: Color,
    val secondary: Color
) {
    BLUE(
        id = "blue",
        title = "Ocean Blue (Default)",
        lightPrimary = Color(0xFF007AFF),
        darkPrimary = Color(0xFF0A84FF),
        secondary = Color(0xFF5E5CE6)
    ),
    VIOLET(
        id = "violet",
        title = "Electric Violet",
        lightPrimary = Color(0xFF7C3AED),
        darkPrimary = Color(0xFF8B5CF6),
        secondary = Color(0xFFA855F7)
    ),
    EMERALD(
        id = "emerald",
        title = "Emerald Green",
        lightPrimary = Color(0xFF059669),
        darkPrimary = Color(0xFF10B981),
        secondary = Color(0xFF34D399)
    ),
    CRIMSON(
        id = "crimson",
        title = "Crimson Ruby",
        lightPrimary = Color(0xFFE11D48),
        darkPrimary = Color(0xFFF43F5E),
        secondary = Color(0xFFFB7185)
    ),
    AMBER(
        id = "amber",
        title = "Amber Sunset",
        lightPrimary = Color(0xFFD97706),
        darkPrimary = Color(0xFFF59E0B),
        secondary = Color(0xFFFBBF24)
    ),
    CYAN(
        id = "cyan",
        title = "Neon Cyan",
        lightPrimary = Color(0xFF0891B2),
        darkPrimary = Color(0xFF06B6D4),
        secondary = Color(0xFF22D3EE)
    ),
    ROSE(
        id = "rose",
        title = "Vibrant Pink",
        lightPrimary = Color(0xFFDB2777),
        darkPrimary = Color(0xFFEC4899),
        secondary = Color(0xFFF472B6)
    );

    companion object {
        fun fromId(id: String?): AccentColor {
            if (id == null) return BLUE
            return entries.find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) } ?: BLUE
        }
    }
}
