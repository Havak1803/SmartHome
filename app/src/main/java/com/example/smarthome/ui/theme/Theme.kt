package com.example.smarthome.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = TextWhite,
    secondary = BlueGrey,
    onSecondary = TextWhite,
    background = DeepDarkBlue,
    onBackground = TextWhite,
    surface = DarkNavyBlue,
    onSurface = TextWhite,
    error = ErrorRed,
    onError = TextWhite
)

@Composable
fun SmartHomeTheme(
    darkTheme: Boolean = true, // Always use dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepDarkBlue.toArgb()
            window.navigationBarColor = DeepDarkBlue.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
