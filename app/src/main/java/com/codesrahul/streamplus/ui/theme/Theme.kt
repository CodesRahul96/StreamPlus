package com.codesrahul.streamplus.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.codesrahul.streamplus.data.settings.ThemeMode
fun getLightColorScheme(accent: AccentColor = AccentColor.BLUE) = lightColorScheme(
    primary = accent.lightPrimary,
    onPrimary = Color.White,
    primaryContainer = accent.lightPrimary.copy(alpha = 0.15f),
    onPrimaryContainer = accent.darkPrimary,
    secondary = accent.secondary,
    onSecondary = Color.White,
    secondaryContainer = accent.secondary.copy(alpha = 0.15f),
    onSecondaryContainer = accent.secondary,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.5f),
    error = ErrorLight,
    onError = Color.White
)

fun getDarkColorScheme(accent: AccentColor = AccentColor.BLUE) = darkColorScheme(
    primary = accent.darkPrimary,
    onPrimary = Color.White,
    primaryContainer = accent.darkPrimary.copy(alpha = 0.2f),
    onPrimaryContainer = accent.lightPrimary,
    secondary = accent.secondary,
    onSecondary = Color.White,
    secondaryContainer = accent.secondary.copy(alpha = 0.2f),
    onSecondaryContainer = accent.secondary,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.5f),
    error = ErrorDark,
    onError = Color.White
)

fun getPitchBlackColorScheme(accent: AccentColor = AccentColor.BLUE) = darkColorScheme(
    primary = accent.darkPrimary,
    onPrimary = Color.White,
    primaryContainer = accent.darkPrimary.copy(alpha = 0.15f),
    onPrimaryContainer = accent.lightPrimary,
    secondary = accent.secondary,
    onSecondary = Color.White,
    secondaryContainer = accent.secondary.copy(alpha = 0.15f),
    onSecondaryContainer = accent.secondary,
    background = PitchBlackBackground,
    onBackground = PitchBlackOnSurface,
    surface = PitchBlackSurface,
    onSurface = PitchBlackOnSurface,
    surfaceVariant = PitchBlackSurfaceVariant,
    onSurfaceVariant = PitchBlackOnSurfaceVariant,
    outline = PitchBlackOutline,
    outlineVariant = PitchBlackOutline.copy(alpha = 0.5f),
    error = ErrorDark,
    onError = Color.White
)

@Composable
fun StreamPlusTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: String = "blue",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.PITCH_BLACK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val accent = AccentColor.fromId(accentColor)
    val colorScheme = when {
        themeMode == ThemeMode.PITCH_BLACK -> getPitchBlackColorScheme(accent)
        themeMode == ThemeMode.DARK -> getDarkColorScheme(accent)
        themeMode == ThemeMode.LIGHT -> getLightColorScheme(accent)
        else -> if (isSystemInDarkTheme()) getDarkColorScheme(accent) else getLightColorScheme(accent)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
