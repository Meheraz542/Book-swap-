package com.aistudio.bookset.market.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = NeumorphicSurface,
    onPrimaryContainer = PrimaryBlueVariant,
    secondary = AccentCoral,
    onSecondary = Color.White,
    background = NeumorphicBackground,
    onBackground = TextPrimary,
    surface = NeumorphicSurface,
    onSurface = TextPrimary,
    surfaceVariant = InputBackground,
    onSurfaceVariant = TextSecondary,
    outline = InputBorder
)

@Composable
fun BookSetMarketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
