package com.example.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

// Custom Cyber-Midnight palette
val MidnightBackground = Color(0xFF090A0E)
val MidnightSurface = Color(0xFF14161F)
val MidnightSurfaceCard = Color(0xFF1E212D)
val CyberCyan = Color(0xFF00E5FF)
val CyberPurple = Color(0xFFD500F9)
val SecureGreen = Color(0xFF00E676)
val SoftGrey = Color(0xFF90A4AE)
val CleanWhite = Color(0xFFF5F7FA)

private val CyanDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    secondary = CyberPurple,
    onSecondary = Color.White,
    background = MidnightBackground,
    onBackground = CleanWhite,
    surface = MidnightSurface,
    onSurface = CleanWhite
)

@Composable
fun CyberBrowserTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyanDarkColorScheme,
        content = content
    )
}
