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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Schemat kolorów dla trybu jasnego
private val LightColorScheme = lightColorScheme(
    primary = Pink80,
    onPrimary = Color.White,
    primaryContainer = Pink90,
    onPrimaryContainer = Pink10,
    secondary = Purple80,
    onSecondary = Color.White,
    secondaryContainer = Purple90,
    onSecondaryContainer = Purple10,
    tertiary = Red80,  // Kolor kurtyny
    onTertiary = Color.White,
    tertiaryContainer = Red90,
    onTertiaryContainer = Red10,
    error = ErrorRed,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color(0xFFFAFAFA),
    onSurface = Color.Black
)

// Schemat kolorów dla trybu ciemnego
private val DarkColorScheme = darkColorScheme(
    primary = Pink40,
    onPrimary = Pink100,
    primaryContainer = Pink30,
    onPrimaryContainer = Pink90,
    secondary = Purple40,
    onSecondary = Purple100,
    secondaryContainer = Purple30,
    onSecondaryContainer = Purple90,
    tertiary = Red40,  // Kolor kurtyny
    onTertiary = Red100,
    tertiaryContainer = Red30,
    onTertiaryContainer = Red90,
    error = ErrorLight,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White
)

@Composable
fun GiftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamiczne kolory są dostępne od Androida 12 (API 31)
    dynamicColor: Boolean = true,
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

            // Ustawienie jasnego lub ciemnego paska statusu
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