package com.example.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Map old hardcoded colors to dynamic material colors
val MidnightBackground: Color
    @Composable get() = MaterialTheme.colorScheme.background

val MidnightSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val MidnightSurfaceCard: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val CyberCyan: Color
    @Composable get() = MaterialTheme.colorScheme.primary

val CyberPurple: Color
    @Composable get() = MaterialTheme.colorScheme.secondary

val SecureGreen: Color
    @Composable get() = Color(0xFF00C853)

val SoftGrey: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val CleanWhite: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground


@Composable
fun CyberBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
