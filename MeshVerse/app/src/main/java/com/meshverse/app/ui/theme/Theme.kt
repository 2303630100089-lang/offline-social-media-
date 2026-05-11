package com.meshverse.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cyberpunk / dark color palette
val CyberCyan = Color(0xFF00F5FF)
val CyberPurple = Color(0xFF7B2FFF)
val CyberMagenta = Color(0xFFFF006E)
val CyberYellow = Color(0xFFFFBE0B)
val DarkBg = Color(0xFF0A0E1A)
val DarkSurface = Color(0xFF0F1423)
val DarkSurfaceMid = Color(0xFF1A2035)
val DarkCard = Color(0xFF1E2640)

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = DarkBg,
    primaryContainer = Color(0xFF003B4A),
    onPrimaryContainer = CyberCyan,
    secondary = CyberPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2D1060),
    onSecondaryContainer = Color(0xFFD0BAFF),
    tertiary = CyberMagenta,
    onTertiary = Color.White,
    background = DarkBg,
    onBackground = Color(0xFFE0E8FF),
    surface = DarkSurface,
    onSurface = Color(0xFFE0E8FF),
    surfaceVariant = DarkSurfaceMid,
    onSurfaceVariant = Color(0xFF9BA8BF),
    outline = Color(0xFF2A3450),
    error = Color(0xFFFF4444)
)

@Composable
fun MeshVerseTheme(
    darkTheme: Boolean = true, // Always dark (cyberpunk)
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
