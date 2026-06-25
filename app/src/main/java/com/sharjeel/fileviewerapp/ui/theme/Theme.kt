package com.sharjeel.fileviewerapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonSecondary,
    onPrimary = Color.Black,
    primaryContainer = NeonSecondary.copy(alpha = 0.2f),
    onPrimaryContainer = Color.White,

    secondary = NeonPrimary,
    onSecondary = Color.White,
    secondaryContainer = NeonPrimary.copy(alpha = 0.2f),
    onSecondaryContainer = Color.White,

    background = GlassBackground,
    onBackground = Color.White,

    surface = GlassSurface,
    onSurface = Color.White,

    surfaceVariant = GlassSurfaceVariant,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),

    outline = Color.White.copy(alpha = 0.12f),
    error = Color(0xFFCF6679),
    onError = Color.Black,

    tertiary = Color(0xFF80DEEA),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF006064).copy(alpha = 0.3f),
    onTertiaryContainer = Color(0xFF80DEEA)
)

private val LightColorScheme = lightColorScheme(
    primary = NeonPrimary,
    onPrimary = Color.White,
    primaryContainer = NeonPrimary.copy(alpha = 0.1f),
    onPrimaryContainer = NeonPrimary,

    secondary = NeonSecondary,
    onSecondary = Color.Black,
    secondaryContainer = NeonSecondary.copy(alpha = 0.1f),
    onSecondaryContainer = NeonSecondary,

    background = LightBackground,
    onBackground = Color(0xFF1A1C1E),

    surface = LightSurface,
    onSurface = Color(0xFF1A1C1E),

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF44474E),

    outline = Color(0xFF74777F),
    error = Color(0xFFBA1A1A),
    onError = Color.White,

    tertiary = Color(0xFF00ACC1),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F7FA),
    onTertiaryContainer = Color(0xFF00ACC1)
)

@Composable
fun FileViewerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    // FIXED: Window architecture level par edge-to-edge enforcement aur icon alignment
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Framework ko instruction dena ke content window system bars ke piche layout draw kare
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Icons visibility control (Light background par dark icons, Dark background par light icons)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}