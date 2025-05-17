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

// Schemat kolorów dla trybu jasnego
private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = White,
    primaryContainer = PurplePastel,
    onPrimaryContainer = PurpleDark,
    secondary = AccentPink,
    onSecondary = White,
    secondaryContainer = CurtainAccent,
    onSecondaryContainer = CurtainPrimary,
    tertiary = AccentBlue,
    onTertiary = White,
    tertiaryContainer = LightGray,
    onTertiaryContainer = DarkGray,
    error = ErrorRed,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black
)

// Schemat kolorów dla trybu ciemnego
private val DarkColorScheme = darkColorScheme(
    primary = PurpleLight,
    onPrimary = Black,
    primaryContainer = PurpleDark,
    onPrimaryContainer = PurplePastel,
    secondary = AccentPink,
    onSecondary = Black,
    secondaryContainer = CurtainPrimary,
    onSecondaryContainer = CurtainAccent,
    tertiary = AccentBlue,
    onTertiary = Black,
    tertiaryContainer = DarkGray,
    onTertiaryContainer = LightGray,
    error = ErrorRed,
    background = Black,
    onBackground = White,
    surface = DarkGray,
    onSurface = White
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamiczne kolory są dostępne od Androida 12 (API 31)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
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
        colorScheme = colorScheme, typography = Typography, shapes = Shapes, content = content
    )
}