package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GemmaAccent,
    onPrimary = Color.White,
    primaryContainer = GemmaAccentHover,
    onPrimaryContainer = Color.White,
    secondary = DarkBgSecondary,
    onSecondary = DarkTextPrimary,
    secondaryContainer = DarkBgTertiary,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = GemmaAccentGlow,
    onTertiary = Color.White,
    background = DarkBgPrimary,
    onBackground = DarkTextPrimary,
    surface = DarkBgCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBgInput,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorderLight,
    error = StatusDanger,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = GemmaAccentHover,
    onPrimary = Color.White,
    primaryContainer = GemmaAccent,
    onPrimaryContainer = Color.White,
    secondary = LightBgSecondary,
    onSecondary = LightTextPrimary,
    secondaryContainer = LightBgTertiary,
    onSecondaryContainer = LightTextPrimary,
    tertiary = GemmaAccent,
    onTertiary = Color.White,
    background = LightBgPrimary,
    onBackground = LightTextPrimary,
    surface = LightBgCard,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBgInput,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorderLight,
    error = StatusDanger,
    onError = Color.White
)

@Composable
fun GemmaAgentTheme(
    themeMode: String = "dark", // "dark", "light", "system"
    textSize: String = "medium", // "small", "medium", "large"
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val scale = when (textSize) {
        "small" -> 0.88f
        "large" -> 1.15f
        else -> 1.0f
    }

    val colors = if (isDark) DarkColorScheme else LightColorScheme
    val typography = getTypographyForScale(scale)

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        content = content
    )
}
