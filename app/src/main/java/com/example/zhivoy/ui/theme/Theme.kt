package com.example.zhivoy.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Светлая цветовая схема
private val LightColorScheme = lightColorScheme(
    primary = FitnessPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = FitnessPrimaryLight,
    onPrimaryContainer = FitnessPrimaryDark,
    
    secondary = FitnessSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = FitnessSecondaryLight,
    onSecondaryContainer = FitnessSecondaryDark,
    
    tertiary = FitnessAccent,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = FitnessAccentLight,
    onTertiaryContainer = FitnessAccentDark,
    
    error = FitnessError,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFF410002),
    
    background = FitnessBackground,
    onBackground = FitnessTextPrimary,
    surface = FitnessSurface,
    onSurface = FitnessTextPrimary,
    surfaceVariant = FitnessSurfaceVariant,
    onSurfaceVariant = FitnessTextSecondary,
    
    outline = FitnessTextTertiary,
    outlineVariant = FitnessSurfaceVariant,
    
    scrim = androidx.compose.ui.graphics.Color.Black,
    inverseSurface = FitnessBackgroundDark,
    inverseOnSurface = FitnessTextPrimaryDark,
    inversePrimary = FitnessPrimaryLight,
    surfaceTint = FitnessPrimary
)

// Темная цветовая схема
private val DarkColorScheme = darkColorScheme(
    primary = FitnessPrimaryLight,
    onPrimary = FitnessPrimaryDark,
    primaryContainer = FitnessPrimary,
    onPrimaryContainer = FitnessPrimaryLight,
    
    secondary = FitnessSecondaryLight,
    onSecondary = FitnessSecondaryDark,
    secondaryContainer = FitnessSecondary,
    onSecondaryContainer = FitnessSecondaryLight,
    
    tertiary = FitnessAccentLight,
    onTertiary = FitnessAccentDark,
    tertiaryContainer = FitnessAccent,
    onTertiaryContainer = FitnessAccentLight,
    
    error = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
    onError = androidx.compose.ui.graphics.Color(0xFF690005),
    errorContainer = androidx.compose.ui.graphics.Color(0xFF93000A),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    
    background = FitnessBackgroundDark,
    onBackground = FitnessTextPrimaryDark,
    surface = FitnessSurfaceDark,
    onSurface = FitnessTextPrimaryDark,
    surfaceVariant = FitnessSurfaceVariantDark,
    onSurfaceVariant = FitnessTextSecondaryDark,
    
    outline = FitnessTextTertiaryDark,
    outlineVariant = FitnessSurfaceVariantDark,
    
    scrim = androidx.compose.ui.graphics.Color.Black,
    inverseSurface = FitnessBackground,
    inverseOnSurface = FitnessTextPrimary,
    inversePrimary = FitnessPrimary,
    surfaceTint = FitnessPrimaryLight
)

@Composable
fun ZhivoyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
