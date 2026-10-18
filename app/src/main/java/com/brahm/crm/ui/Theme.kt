package com.brahm.crm.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
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

private val BrandPurple = Color(0xFF5B4BD6)
private val BrandPurpleDark = Color(0xFF8B7DF0)

private val LightColors = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E2FB),
    onPrimaryContainer = Color(0xFF1C1348),
    secondary = Color(0xFF5A5B7A),
    tertiary = Color(0xFF2E9E6B),
    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1B20),
    surfaceVariant = Color(0xFFEDECF4),
    onSurfaceVariant = Color(0xFF494A57),
    outline = Color(0xFFC6C5D2),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = BrandPurpleDark,
    onPrimary = Color(0xFF17123A),
    primaryContainer = Color(0xFF3A2F7A),
    onPrimaryContainer = Color(0xFFE6E2FB),
    secondary = Color(0xFFC3C2E0),
    tertiary = Color(0xFF7FD9AB),
    background = Color(0xFF121218),
    onBackground = Color(0xFFE5E4EC),
    surface = Color(0xFF1C1C24),
    onSurface = Color(0xFFE5E4EC),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C5D2),
    outline = Color(0xFF8E8D9A),
    error = Color(0xFFFFB4AB)
)

@Composable
fun BrahmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
