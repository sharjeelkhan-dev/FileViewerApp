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

// ==========================================
// MATERIAL DESIGN 3 COLOR SCHEMES
// ==========================================

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimary.copy(alpha = 0.2f),
    onPrimaryContainer = Color(0xFF7DD3FC),

    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = BrandSecondary.copy(alpha = 0.2f),
    onSecondaryContainer = Color(0xFF99F6E4),

    background = DarkBackground,
    onBackground = Color(0xFFF8FAFC),

    surface = DarkSurface,
    onSurface = Color(0xFFF8FAFC),

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),

    outline = Color(0xFF475569),
    error = Color(0xFFEF4444),
    onError = Color.White,

    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF0F172A),
    tertiaryContainer = Color(0xFF0C4A6E),
    onTertiaryContainer = Color(0xFFBAE6FD)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = BrandPrimary,

    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = BrandSecondary,

    background = LightBackground,
    onBackground = Color(0xFF0F172A),

    surface = LightSurface,
    onSurface = Color(0xFF0F172A),

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF64748B),

    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White,

    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF0369A1)
)

// ==========================================
// THEME COMPOSABLE FUNCTION
// ==========================================

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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Make sure typography variable is imported correctly
        content = content
    )
}