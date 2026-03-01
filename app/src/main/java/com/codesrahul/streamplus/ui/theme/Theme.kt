package com.codesrahul.streamplus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.codesrahul.streamplus.data.settings.ThemeMode
import com.codesrahul.streamplus.data.settings.ThemeStyle

private val DarkColorScheme = darkColorScheme(
    primary = iOSBlueDark,
    onPrimary = Color.White,
    secondary = iOSBlueDark,
    background = iOSBgDark,
    onBackground = Color.White,
    surface = iOSBgDark,
    onSurface = Color.White,
    surfaceVariant = iOSSurfaceDark,
    onSurfaceVariant = Color.White,
    outline = iOSDividerDark,
    error = iOSRedDark
)

private val LightColorScheme = lightColorScheme(
    primary = iOSBlueLight,
    onPrimary = Color.White,
    secondary = iOSBlueLight,
    background = iOSBgLight,
    onBackground = Color.Black,
    surface = iOSBgLight,
    onSurface = Color.Black,
    surfaceVariant = iOSSurfaceLight,
    onSurfaceVariant = Color.Black,
    outline = iOSDividerLight,
    error = iOSRedLight
)

// Custom Schemes
private val IgniteDark = darkColorScheme(primary = IgnitePrimary, secondary = IgniteSecondary, tertiary = IgniteTertiary)
private val IgniteLight = lightColorScheme(primary = IgnitePrimary, secondary = IgniteSecondary, tertiary = IgniteTertiary)

private val FrostyDark = darkColorScheme(primary = FrostyPrimary, secondary = FrostySecondary, tertiary = FrostyTertiary)
private val FrostyLight = lightColorScheme(primary = FrostyPrimary, secondary = FrostySecondary, tertiary = FrostyTertiary)

private val EarthDark = darkColorScheme(primary = EarthPrimary, secondary = EarthSecondary, tertiary = EarthTertiary)
private val EarthLight = lightColorScheme(primary = EarthPrimary, secondary = EarthSecondary, tertiary = EarthTertiary)

private val GroveDark = darkColorScheme(primary = GrovePrimary, secondary = GroveSecondary, tertiary = GroveTertiary)
private val GroveLight = lightColorScheme(primary = GrovePrimary, secondary = GroveSecondary, tertiary = GroveTertiary)

private val RoseDark = darkColorScheme(primary = RosePrimary, secondary = RoseSecondary, tertiary = RoseTertiary)
private val RoseLight = lightColorScheme(primary = RosePrimary, secondary = RoseSecondary, tertiary = RoseTertiary)

@Composable
fun StreamPlusTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeStyle: ThemeStyle = ThemeStyle.DYNAMIC,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when (themeStyle) {
        ThemeStyle.DYNAMIC -> {
            // For iOS style, we disable Android's Material You dynamic colors
            // so we can enforce the Apple system color palette.
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        ThemeStyle.IGNITE -> if (darkTheme) IgniteDark else IgniteLight
        ThemeStyle.FROSTY -> if (darkTheme) FrostyDark else FrostyLight
        ThemeStyle.GROVE -> if (darkTheme) GroveDark else GroveLight
        ThemeStyle.SUNSET -> {
            if (darkTheme) {
                darkColorScheme(primary = SunsetPrimary, secondary = SunsetSecondary, tertiary = SunsetTertiary)
            } else {
                lightColorScheme(primary = SunsetPrimary, secondary = SunsetSecondary, tertiary = SunsetTertiary)
            }
        }
        ThemeStyle.OCEAN -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = OceanSecondary,
                    onPrimary = Color.White,
                    primaryContainer = OceanPrimary,
                    onPrimaryContainer = Color.White,
                    secondary = OceanSecondary,
                    onSecondary = Color.White,
                    background = Color(0xFF001214),
                    onBackground = Color.White,
                    surface = Color(0xFF001214),
                    onSurface = Color.White,
                    surfaceVariant = Color(0xFF002226),
                    onSurfaceVariant = Color.LightGray
                )
            } else {
                lightColorScheme(
                    primary = OceanPrimary,
                    onPrimary = Color.White,
                    primaryContainer = OceanPrimary,
                    onPrimaryContainer = Color.White,
                    secondary = OceanSecondary,
                    onSecondary = Color.White,
                    background = Color(0xFFE0F7FA),
                    onBackground = Color(0xFF002226),
                    surface = Color.White,
                    onSurface = Color(0xFF002226),
                    surfaceVariant = Color(0xFFB2EBF2),
                    onSurfaceVariant = Color.DarkGray
                )
            }
        }
        ThemeStyle.CARBON -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color.White,
                    onPrimary = Color.Black,
                    primaryContainer = Color.DarkGray,
                    onPrimaryContainer = Color.White,
                    secondary = Color.Gray,
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF1A1A1A)
                )
            } else {
                lightColorScheme(
                    primary = CarbonPrimary,
                    onPrimary = Color.White,
                    secondary = CarbonSecondary,
                    background = Color(0xFFF5F5F5),
                    surface = Color.White,
                    surfaceVariant = Color(0xFFE0E0E0)
                )
            }
        }
        ThemeStyle.MIDNIGHT -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = StreamPlusBlue,
                    onPrimary = Color.White,
                    primaryContainer = StreamPlusNavy,
                    onPrimaryContainer = Color.White,
                    secondary = Color(0xFF03A9F4),
                    onSecondary = Color.White,
                    background = Color(0xFF0A0A0A),
                    onBackground = Color.White,
                    surface = Color(0xFF0A0A0A),
                    onSurface = Color.White,
                    surfaceVariant = Color(0xFF161616),
                    onSurfaceVariant = Color.LightGray,
                    outline = Color.DarkGray
                )
            } else {
                lightColorScheme(
                    primary = StreamPlusNavy,
                    onPrimary = Color.White,
                    primaryContainer = StreamPlusBlue,
                    onPrimaryContainer = Color.White,
                    secondary = Color(0xFF0288D1),
                    onSecondary = Color.White,
                    background = Color.White,
                    onBackground = Color.Black,
                    surface = Color.White,
                    onSurface = Color.Black,
                    surfaceVariant = Color(0xFFF5F5F5),
                    onSurfaceVariant = Color.DarkGray,
                    outline = Color.Gray
                )
            }
        }
        ThemeStyle.AMOLED -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color.White,
                    onPrimary = Color.Black,
                    primaryContainer = Color(0xFF121212),
                    onPrimaryContainer = Color.White,
                    secondary = Color.LightGray,
                    onSecondary = Color.Black,
                    background = Color.Black,
                    onBackground = Color.White,
                    surface = Color.Black,
                    onSurface = Color.White,
                    surfaceVariant = Color(0xFF080808),
                    onSurfaceVariant = Color.LightGray,
                    outline = Color.Gray
                )
            } else {
                // AMOLED Lite (high contrast)
                lightColorScheme(
                    primary = Color.Black,
                    onPrimary = Color.White,
                    primaryContainer = Color.LightGray,
                    onPrimaryContainer = Color.Black,
                    secondary = Color.Gray,
                    onSecondary = Color.White,
                    background = Color.White,
                    onBackground = Color.Black,
                    surface = Color.White,
                    onSurface = Color.Black,
                    surfaceVariant = Color(0xFFF0F0F0),
                    onSurfaceVariant = Color.Black
                )
            }
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            val controller = WindowCompat.getInsetsController(window, view)
            controller?.isAppearanceLightStatusBars = !darkTheme
            controller?.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
