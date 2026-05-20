package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LipstickPrimary,
    onPrimary = Color.White,
    secondary = LipstickAccent,
    onSecondary = Color.White,
    background = Color(0xFF1E1115),
    surface = Color(0xFF28181D),
    onBackground = Color(0xFFFFECEF),
    onSurface = Color(0xFFFFECEF),
    outline = LipstickAccent
)

private val LightColorScheme = lightColorScheme(
    primary = LipstickPrimary,
    onPrimary = Color.White,
    secondary = LipstickAccent,
    onSecondary = Color.White,
    background = BeautyBackground,
    surface = BeautySurface,
    onBackground = BeautyOnSurface,
    onSurface = BeautyOnSurface,
    outline = BeautyGrayBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force brand colors for consistent business aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
