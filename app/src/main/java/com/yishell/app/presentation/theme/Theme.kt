package com.yishell.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Theme — 按设计规范 3.x / 13.x
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = Cyan700,
    tertiary = Cyan500,
    background = LightBackground,       // #F7F9FC
    surface = LightSurface,             // #FFFFFF
    surfaceVariant = LightSurfaceVariant,
    onPrimary = LightOnPrimary,
    onSecondary = LightOnSecondary,
    onTertiary = LightOnTertiary,
    onBackground = LightOnBackground,   // #1F2937
    onSurface = LightOnSurface,         // #1F2937
    onSurfaceVariant = LightOnSurfaceVariant, // #6B7280
    error = LightError,
    onError = LightOnPrimary
)

// Dark Theme — 按设计规范 14.x
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,              // #4D8DFF
    secondary = GlowBlue,               // #6AA8FF
    tertiary = Cyan500,
    background = DarkBackground,        // #0F1115
    surface = DarkSurface,              // #1A1D24
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = DarkOnBackground,    // #E6EDF3
    onSurface = DarkOnSurface,          // #E6EDF3
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = Danger,
    onError = Color.White
)

@Composable
fun YiShellTheme(
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
