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
private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimary.copy(alpha = 0.15f),
    onPrimaryContainer = BrandPrimaryDark,
    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = BrandSecondary.copy(alpha = 0.15f),
    onSecondaryContainer = BrandSecondary,
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

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryDark.copy(alpha = 0.2f),
    onPrimaryContainer = BrandPrimary,
    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = BrandSecondary.copy(alpha = 0.2f),
    onSecondaryContainer = BrandSecondary,
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

private val PitchBlackColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryDark.copy(alpha = 0.15f),
    onPrimaryContainer = BrandPrimary,
    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = BrandSecondary.copy(alpha = 0.15f),
    onSecondaryContainer = BrandSecondary,
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
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.PITCH_BLACK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        themeMode == ThemeMode.PITCH_BLACK -> PitchBlackColorScheme
        themeMode == ThemeMode.DARK -> DarkColorScheme
        themeMode == ThemeMode.LIGHT -> LightColorScheme
        else -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
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
