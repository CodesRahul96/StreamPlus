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
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        ThemeStyle.IGNITE -> if (darkTheme) IgniteDark else IgniteLight
        ThemeStyle.FROSTY -> if (darkTheme) FrostyDark else FrostyLight
        ThemeStyle.GROVE -> if (darkTheme) GroveDark else GroveLight
        ThemeStyle.MIDNIGHT -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = StreamPlusBlue,
                    onPrimary = Color.White,
                    primaryContainer = StreamPlusNavy,
                    onPrimaryContainer = Color.White,
                    secondary = Color(0xFF03A9F4),
                    onSecondary = Color.White,
                    background = Color.Black,
                    onBackground = Color.White,
                    surface = Color.Black,
                    onSurface = Color.White,
                    surfaceVariant = Color(0xFF121212),
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
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
