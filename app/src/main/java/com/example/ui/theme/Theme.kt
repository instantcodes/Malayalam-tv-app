package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkMalayalamColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color.Black,
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = OnAmberContainerDark,
    secondary = AmberSecondary,
    onSecondary = Color.Black,
    tertiary = AmberTertiary,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkCardBorder,
    error = LiveBroadcastRed
)

val LightMalayalamColorScheme = lightColorScheme(
    primary = AmberPrimary,
    onPrimary = Color.Black,
    secondary = AmberSecondary,
    onSecondary = Color.Black,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = LiveBroadcastRed
)

@Composable
fun MalayalamTvTheme(
    darkTheme: Boolean = true, // Default to cinematic dark theme as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkMalayalamColorScheme else LightMalayalamColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
