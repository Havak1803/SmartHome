package com.example.smarthome.utils

/**
 * AppConfig - Smart Home V3.3
 * Centralized configuration for Firebase and demo credentials
 */
object AppConfig {

    // Firebase Configuration (Asia-Southeast1 Region)
    const val FIREBASE_URL = "https://iot-smarthome-304c3-default-rtdb.asia-southeast1.firebasedatabase.app"
    const val FIREBASE_DB_SECRET = "q9IEaRyFj3MJpBUMr3LzDygpv0XNkqyKhip06dxF"

    // Demo User Credentials (for convenience)
    const val DEMO_EMAIL = "admin@smarthome.com"
    const val DEMO_PASSWORD = "123456"

    // MQTT Configuration
    const val MQTT_BROKER = "6ceea111b6144c71a57b21faa3553fc6.s1.eu.hivemq.cloud"
    const val MQTT_PORT = 8883

    // App Version
    const val APP_VERSION = "3.3"
}
