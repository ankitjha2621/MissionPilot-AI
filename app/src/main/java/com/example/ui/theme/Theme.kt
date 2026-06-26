package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cosmic / Cyberpunk UI Palette
val CyberBlack = Color(0xFF0B0D19)
val CyberSlate = Color(0xFF13172E)
val CyberBorder = Color(0xFF1F2445)
val ElectricBlue = Color(0xFF00E5FF)
val NeonPurple = Color(0xFFD500F9)
val NeonPink = Color(0xFFFF2E93)
val CyberGreen = Color(0xFF00E676)
val CyberOrange = Color(0xFFFF9100)
val AeroBlue = Color(0xFF7FFFD4)
val TextPrimary = Color(0xFFF0F4FF)
val TextSecondary = Color(0xFF8B9BB4)
val TextMuted = Color(0xFF4C586F)

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = NeonPurple,
    tertiary = NeonPink,
    background = CyberBlack,
    surface = CyberSlate,
    onPrimary = CyberBlack,
    onSecondary = CyberBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    secondary = NeonPurple,
    tertiary = NeonPink,
    background = Color.White,
    surface = Color(0xFFF5F7FA),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun MissionPilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // Force dark theme for cyberpunk aesthetic

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
