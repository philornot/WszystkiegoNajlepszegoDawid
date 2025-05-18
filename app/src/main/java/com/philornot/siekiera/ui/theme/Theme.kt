package com.philornot.siekiera.ui.theme

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
import androidx.core.view.WindowCompat

// Light color scheme with lavender focus
private val LightColorScheme = lightColorScheme(
    primary = LavenderPrimary,
    onPrimary = White,
    primaryContainer = LavenderPastel,
    onPrimaryContainer = LavenderDark,
    secondary = AccentMauve,
    onSecondary = White,
    secondaryContainer = CurtainSecondary,
    onSecondaryContainer = LavenderDark,
    tertiary = AccentPeriwinkle,
    onTertiary = White,
    tertiaryContainer = LightGray,
    onTertiaryContainer = DarkGray,
    error = ErrorRed,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = LavenderPastel.copy(alpha = 0.4f),
    onSurfaceVariant = LavenderDark
)

// Dark color scheme with lavender focus
private val DarkColorScheme = darkColorScheme(
    primary = LavenderLight,
    onPrimary = Black,
    primaryContainer = LavenderDark,
    onPrimaryContainer = LavenderPastel,
    secondary = AccentMauve,
    onSecondary = Black,
    secondaryContainer = LavenderDark,
    onSecondaryContainer = LavenderPastel,
    tertiary = AccentPeriwinkle,
    onTertiary = Black,
    tertiaryContainer = DarkGray,
    onTertiaryContainer = LightGray,
    error = ErrorRed,
    background = Black,
    onBackground = White,
    surface = DarkGray,
    onSurface = White,
    surfaceVariant = LavenderDark.copy(alpha = 0.6f),
    onSurfaceVariant = LavenderPastel
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic colors available on Android 12+
    dynamicColor: Boolean = false, // Disabled by default to maintain lavender theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
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
        shapes = Shapes,
        content = content
    )
}