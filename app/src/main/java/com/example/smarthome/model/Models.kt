package com.example.smarthome.model

import androidx.compose.ui.graphics.Color
import com.google.gson.annotations.SerializedName
import java.util.UUID

// ==========================================
// DATA MODELS FOR VERSION 3.1 (MQTT + FIREBASE HYBRID)
// ==========================================

/**
 * Represents a smart home room/device with all its states
 */
data class Room(
    val id: String = "",
    val name: String = "",
    val sensors: Sensors = Sensors(),
    val actuators: Actuators = Actuators(),
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val isConnected: Boolean = false,
    val lastUpdate: Long = 0L
)

/**
 * Sensor data received from ESP32 via MQTT topic: SmartHome/[deviceID]/data
 */
data class Sensors(
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val light: Int = 0
)

/**
 * Actuator states received from ESP32 via MQTT topic: SmartHome/[deviceID]/state
 */
data class Actuators(
    val light: Int = 0,  // 0 = OFF, 1 = ON
    val fan: Int = 0,    // 0 = OFF, 1 = ON
    val ac: Int = 0,     // 0 = OFF, 1 = ON
    val mode: Int = 0,   // 0 = System OFF, 1 = System ON
    val interval: Int = 5 // Sensor reading cycle in seconds
)

/**
 * Device information received from ESP32 via MQTT topic: SmartHome/[deviceID]/info
 */
data class DeviceInfo(
    val ip: String = "",
    val ssid: String = "",
    val firmware: String = "",
    val mac: String = ""
)

// ==========================================
// FIREBASE HISTORICAL DATA MODELS (V3.1)
// ==========================================

/**
 * Historical log entry from Firebase Realtime Database
 * Path: history/{deviceID}/{pushId}
 *
 * IMPORTANT: Firebase field mapping (verified from console):
 * - temp: Float (temperature in °C)
 * - humid: Float (humidity in %)
 * - light: Int (lux value)
 * - last_update: Long (timestamp in milliseconds)
 */
data class HistoryLog(
    val timestamp: Long = 0L,
    val temp: Float = 0f,
    val humid: Float = 0f,
    val lux: Int = 0,
    val pushId: String = ""
) {
    companion object {
        fun fromMap(pushId: String, map: Map<String, Any>): HistoryLog {
            return HistoryLog(
                pushId = pushId,
                timestamp = (map["last_update"] as? Long) ?: (map["last_update"] as? Double)?.toLong() ?: 0L,
                temp = (map["temp"] as? Number)?.toFloat() ?: 0f,
                humid = (map["humid"] as? Number)?.toFloat() ?: 0f,
                lux = (map["light"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

/**
 * Chart data for historical sensor readings (kept for compatibility)
 */
data class LogEntry(
    val temp: Float = 0f,
    val humidity: Float = 0f,
    val lux: Int = 0,
    val timestamp: Long = 0L
)

enum class ChartParameter(val label: String, val color: Color, val unit: String) {
    TEMPERATURE("Temperature", Color(0xFFFF5252), "°C"),
    HUMIDITY("Humidity", Color(0xFF007bff), "%"),
    LUX("Light", Color(0xFFFFAB00), "Lux")
}

enum class TimeRange(val label: String, val hours: Int) {
    DAY("24 Hours", 24),
    WEEK("7 Days", 168),
    MONTH("30 Days", 720),
    ALL_TIME("All Time", -1)  // -1 means no time limit
}

// ==========================================
// DATA HISTORY FILTER (FOR DATA HISTORY TAB)
// ==========================================

enum class DataHistoryFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

// ==========================================
// FIREBASE CONFIGURATION (V3.1)
// ==========================================

/**
 * Firebase Realtime Database configuration
 */
data class FirebaseConfig(
    val databaseUrl: String = "https://iot-smarthome-304c3-default-rtdb.asia-southeast1.firebasedatabase.app/",
    val apiKey: String = ""
)

// ==========================================
// MQTT COMMAND MODELS
// ==========================================

/**
 * Base structure for all MQTT commands sent to ESP32
 */
data class MqttCommand(
    val id: String = UUID.randomUUID().toString(),
    val command: String,
    val params: Any
)

/**
 * Command: set_device - Control single device (light, fan, or ac)
 */
data class SetDeviceParams(
    val device: String,  // "fan" | "light" | "ac"
    val state: Int       // 0 = OFF, 1 = ON
)

/**
 * Command: set_devices - Control multiple devices at once
 */
data class SetDevicesParams(
    val fan: Int,    // 0 = OFF, 1 = ON
    val light: Int,  // 0 = OFF, 1 = ON
    val ac: Int      // 0 = OFF, 1 = ON
)

/**
 * Command: set_mode - Toggle master room switch
 */
data class SetModeParams(
    val mode: Int  // 0 = System OFF, 1 = System ON
)

/**
 * Command: set_interval - Update sensor reading interval
 */
data class SetIntervalParams(
    val interval: Int  // Range: 5-3600 seconds
)

/**
 * Command: reboot - Reboot ESP32 system
 * This command has no parameters
 */
data class RebootParams(
    val dummy: String = ""  // Empty params object
)

// ==========================================
// MQTT BROKER CONFIGURATION
// ==========================================

/**
 * MQTT Broker connection settings
 */
data class MqttConfig(
    val broker: String = "6ceea111b6144c71a57b21faa3553fc6.s1.eu.hivemq.cloud",
    val port: Int = 8883,
    val username: String = "",
    val password: String = "",
    val clientId: String = "SmartHomeApp_${UUID.randomUUID().toString().substring(0, 8)}"
)

// ==========================================
// MQTT MESSAGE PARSING MODELS
// ==========================================

/**
 * Used for parsing incoming MQTT messages
 */
data class MqttStateMessage(
    @SerializedName("light") val light: Int = 0,
    @SerializedName("fan") val fan: Int = 0,
    @SerializedName("ac") val ac: Int = 0,
    @SerializedName("mode") val mode: Int = 0,
    @SerializedName("interval") val interval: Int = 5
)

data class MqttDataMessage(
    @SerializedName("temperature") val temperature: Float = 0f,
    @SerializedName("humidity") val humidity: Float = 0f,
    @SerializedName("light") val light: Int = 0
)

data class MqttInfoMessage(
    @SerializedName("ip") val ip: String = "",
    @SerializedName("ssid") val ssid: String = "",
    @SerializedName("firmware") val firmware: String = "",
    @SerializedName("mac") val mac: String = ""
)
