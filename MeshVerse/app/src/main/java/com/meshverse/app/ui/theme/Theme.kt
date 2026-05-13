package com.meshverse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonBlue = Color(0xFF38B6FF)
val NeonPurple = Color(0xFF8E58FF)
val NeonViolet = Color(0xFFB055FF)
val NeonAqua = Color(0xFF2EE6D6)
val MeshBackground = Color(0xFF040A16)
val MeshSurface = Color(0xFF0B1527)
val MeshSurfaceSoft = Color(0xFF121F36)
val MeshOutline = Color(0xFF2A3650)

private val MeshDarkColors = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF28184E),
    onPrimaryContainer = Color(0xFFE7DBFF),
    secondary = NeonBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF102A4A),
    onSecondaryContainer = Color(0xFFD8E9FF),
    tertiary = NeonAqua,
    onTertiary = Color(0xFF03201D),
    background = MeshBackground,
    onBackground = Color(0xFFE5EDFF),
    surface = MeshSurface,
    onSurface = Color(0xFFE5EDFF),
    surfaceVariant = MeshSurfaceSoft,
    onSurfaceVariant = Color(0xFFA7B4CF),
    outline = MeshOutline,
    error = Color(0xFFFF5A7A)
)

@Composable
fun MeshVerseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MeshDarkColors,
        typography = Typography(),
        content = content
    )
}
