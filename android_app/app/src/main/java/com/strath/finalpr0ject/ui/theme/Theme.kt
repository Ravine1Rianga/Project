package com.strath.finalpr0ject.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CalmGreenPrimary,
    onPrimary = CalmGreenOnPrimary,
    primaryContainer = CalmGreenContainer,
    onPrimaryContainer = CalmGreenOnContainer,
    secondary = ParentCoral,
    onSecondary = Color.White,
    secondaryContainer = ParentCoralContainer,
    onSecondaryContainer = ParentCoralOnContainer,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondaryLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    outline = CardBorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF139771),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF00382B),
    onPrimaryContainer = Color(0xFF88F2CE),
    secondary = ParentCoral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5D1A16),
    onSecondaryContainer = ParentCoralContainer,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDarkSurface,
    onSurfaceVariant = TextSecondaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    outline = CardBorderDark
)

@Composable
fun AfyaEarlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
