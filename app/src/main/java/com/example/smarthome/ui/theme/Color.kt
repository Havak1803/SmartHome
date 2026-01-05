package com.example.smarthome.ui.theme

import androidx.compose.ui.graphics.Color

// ====================================================================
// Smart Home V3.3 - CLEAN LIGHT THEME
// Professional light theme optimized for readability
// ====================================================================

// Background Colors
val LightBackground = Color(0xFFFFFFFF)      // White background
val LightSurface = Color(0xFFF5F5F5)         // Light grey for cards/surfaces

// Primary Colors
val ElectricBlue = Color(0xFF3b82f6)        // Active state (ON) - Modern blue
val BlueGrey = Color(0xFF94a3b8)            // Inactive state (OFF)

// Text Colors
val TextDark = Color(0xFF1e293b)            // Primary text (dark for light background)
val TextGrey = Color(0xFF64748b)            // Secondary text/labels

// Status Colors
val SuccessGreen = Color(0xFF4CAF50)        // Connected/Success
val WarningAmber = Color(0xFFFF9800)        // Warning
val ErrorRed = Color(0xFFF44336)            // Error/Danger

// Sensor Colors (for Charts)
val TemperatureColor = Color(0xFFFF6B6B)    // Warm red
val HumidityColor = Color(0xFF4ECDC4)       // Cyan
val LightColor = Color(0xFFFFD93D)          // Yellow

// Overlay/Divider
val DividerColor = Color(0xFFe2e8f0)        // Subtle divider for light theme
val OverlayColor = Color(0x1A000000)        // 10% black overlay

// ====================================================================
// ALIASES FOR COMPATIBILITY (Required by MainScreen & SettingsScreen)
// ====================================================================
val BgColor = LightBackground                // Background alias
val SurfaceColor = LightSurface              // Surface alias
val TextPrimary = TextDark                   // Primary text alias
val PrimaryBlue = ElectricBlue               // Primary color alias

// ====================================================================
// BACKWARD COMPATIBILITY ALIASES (for old code)
// ====================================================================
val DeepDarkBlue = LightBackground           // Now maps to white
val DarkNavyBlue = LightSurface              // Now maps to light grey
val TextWhite = TextDark                     // Now maps to dark text for light theme

