package com.example.bookmymovie.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PremiumDarkScheme = darkColorScheme(
    primary = PrimaryAccent,
    secondary = SecondaryAccent,
    tertiary = WarmHighlight,
    background = DeepCharcoal,
    surface = SecondaryBackground,
    surfaceVariant = CardBackground,
    onPrimary = Color(0xFF1A1207),
    onSecondary = Color(0xFF1A1207),
    onTertiary = Color(0xFF1A1207),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    outlineVariant = DividerColor,
    inverseSurface = TextPrimary,
    inverseOnSurface = DeepCharcoal,
    surfaceTint = Color.Transparent,
    error = ErrorRose,
    onError = TextPrimary
)

@Composable
fun BookmyMovieTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = PremiumDarkScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepCharcoal.toArgb()
            window.navigationBarColor = DeepCharcoal.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
