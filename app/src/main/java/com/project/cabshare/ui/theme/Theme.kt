package com.project.cabshare.ui.theme

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

// Create dark color scheme with our custom cab sharing colors
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary, // Lighter Turquoise
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary, // Lighter Orange
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary, // Amber/Light Orange
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkOnSurface,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

// Create light color scheme with our custom cab sharing colors
private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFFD1F3F4), // Light variant of PrimaryAccent
    onPrimaryContainer = Color(0xFF00565B), // Dark variant of PrimaryAccent
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = Color(0xFFFFDBD0), // Light variant of Secondary
    onSecondaryContainer = Color(0xFF5B1B00), // Dark variant of Secondary
    tertiary = SecondaryVariant,
    background = Background,
    surface = Surface,
    onSurface = OnSurface,
    error = Error
)

@Composable
fun CabShareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to use our custom colors by default
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
            try {
                val context = view.context
                if (context is Activity) {
                    val window = context.window
                    
                    // Use insetsController approach instead of deprecated direct property access
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme
                    
                    // Apply colors via the newer approach
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    
                    // Set the status bar and navigation bar colors
                    // This is still needed for backward compatibility
                    @Suppress("DEPRECATION")
                    window.statusBarColor = colorScheme.primary.toArgb()
                    
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = colorScheme.background.toArgb()
                }
            } catch (e: Exception) {
                // Just log the error but don't crash
                e.printStackTrace()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 