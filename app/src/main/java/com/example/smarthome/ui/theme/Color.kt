package com.example.smarthome.ui.theme

import androidx.compose.ui.graphics.Color

// ====================================================================
// Smart Home V3.3 - MIDNIGHT DEEP BLUE THEME
// Professional dark theme optimized for night use
// ====================================================================

// Background Colors
val DeepDarkBlue = Color(0xFF020810)        // Almost black, very deep blue
val DarkNavyBlue = Color(0xFF0F1E2E)        // Dark Navy for cards/surfaces

// Primary Colors
val ElectricBlue = Color(0xFF007BFF)        // Active state (ON) - Changed to 0x007BFF per request
val BlueGrey = Color(0xFF455A64)            // Inactive state (OFF)

// Text Colors
val TextWhite = Color.White                  // Primary text
val TextGrey = Color(0xFF90A4AE)            // Secondary text/labels

// Status Colors
val SuccessGreen = Color(0xFF4CAF50)        // Connected/Success
val WarningAmber = Color(0xFFFF9800)        // Warning
val ErrorRed = Color(0xFFF44336)            // Error/Danger

// Sensor Colors (for Charts)
val TemperatureColor = Color(0xFFFF6B6B)    // Warm red
val HumidityColor = Color(0xFF4ECDC4)       // Cyan
val LightColor = Color(0xFFFFD93D)          // Yellow

// Overlay/Divider
val DividerColor = Color(0xFF263238)        // Subtle divider
val OverlayColor = Color(0x33000000)        // 20% black overlay

// ====================================================================
// ALIASES FOR COMPATIBILITY (Required by MainScreen & SettingsScreen)
// ====================================================================
val BgColor = DeepDarkBlue                   // Background alias
val SurfaceColor = DarkNavyBlue              // Surface alias
val TextPrimary = TextWhite                  // Primary text alias
val PrimaryBlue = ElectricBlue               // Primary color alias

