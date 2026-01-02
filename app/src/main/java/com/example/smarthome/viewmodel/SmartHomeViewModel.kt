package com.example.smarthome.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthome.R
import com.example.smarthome.model.*
import com.example.smarthome.network.MQTTHelper
import com.example.smarthome.utils.AppConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.UUID

/**
 * SmartHomeViewModel - Version 3.5
 * FIXED: Firebase Asia-Southeast1 region + Type safety + Threshold Notifications
 */
class SmartHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "SmartHomeViewModel"
    private val gson = Gson()
    private var mqttHelper: MQTTHelper? = null
    private val roomPrefs = application.getSharedPreferences("room_names", android.content.Context.MODE_PRIVATE)
    private val thresholdPrefs = application.getSharedPreferences("threshold_prefs", android.content.Context.MODE_PRIVATE)
    private var commandCounter = 0

    // Notification channel
    private val CHANNEL_ID = "smart_home_alerts"
    private val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Firebase Database instance with Asia region URL
    private val firebaseDatabase: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance(AppConfig.FIREBASE_URL)
    }

    // ==========================================
    // STATE FLOWS
    // ==========================================

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _mqttConfig = MutableStateFlow(
        MqttConfig(
            broker = AppConfig.MQTT_BROKER,
            port = AppConfig.MQTT_PORT,
            username = "",
            password = ""
        )
    )
    val mqttConfig: StateFlow<MqttConfig> = _mqttConfig.asStateFlow()

    private val _chartData = MutableStateFlow<List<HistoryLog>>(emptyList())
    val chartData: StateFlow<List<HistoryLog>> = _chartData.asStateFlow()

    private val _firebaseConfig = MutableStateFlow(
        FirebaseConfig(
            databaseUrl = AppConfig.FIREBASE_URL,
            apiKey = AppConfig.FIREBASE_DB_SECRET
        )
    )
    val firebaseConfig: StateFlow<FirebaseConfig> = _firebaseConfig.asStateFlow()

    private val _isLoadingChart = MutableStateFlow(false)
    val isLoadingChart: StateFlow<Boolean> = _isLoadingChart.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isNotificationEnabled = MutableStateFlow(false)
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled.asStateFlow()

    init {
        Log.d(TAG, "ViewModel v3.5 initialized with Firebase URL: ${AppConfig.FIREBASE_URL}")
        loadMqttConfig()
        loadFirebaseConfig()
        loadNotificationEnabled()
        createNotificationChannel()
    }

    // ==========================================
    // THRESHOLD SETTINGS
    // ==========================================

    data class ThresholdSettings(
        val tempThreshold: Float = 30f,
        val humidThreshold: Float = 70f,
        val luxThreshold: Int = 500
    )

    fun getThresholdSettings(): ThresholdSettings {
        return ThresholdSettings(
            tempThreshold = thresholdPrefs.getFloat("temp_threshold", 30f),
            humidThreshold = thresholdPrefs.getFloat("humid_threshold", 70f),
            luxThreshold = thresholdPrefs.getInt("lux_threshold", 500)
        )
    }

    fun saveThresholdSettings(tempThreshold: Float, humidThreshold: Float, luxThreshold: Int) {
        thresholdPrefs.edit()
            .putFloat("temp_threshold", tempThreshold)
            .putFloat("humid_threshold", humidThreshold)
            .putInt("lux_threshold", luxThreshold)
            .apply()
        Log.d(TAG, "✅ Thresholds saved: Temp=$tempThreshold°C, Humid=$humidThreshold%, Lux=$luxThreshold")
    }

    // ==========================================
    // NOTIFICATION SETTINGS
    // ==========================================

    fun setNotificationEnabled(enabled: Boolean) {
        _isNotificationEnabled.value = enabled
        thresholdPrefs.edit().putBoolean("notification_enabled", enabled).apply()
        Log.d(TAG, if (enabled) "✅ Notifications ENABLED" else "⚠️ Notifications DISABLED")
    }

    private fun loadNotificationEnabled() {
        _isNotificationEnabled.value = thresholdPrefs.getBoolean("notification_enabled", false)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Home Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sensor threshold alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(deviceName: String, sensorType: String, value: String, threshold: String) {
        if (!_isNotificationEnabled.value) return

        val notification = NotificationCompat.Builder(getApplication(), CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Alert: $deviceName")
            .setContentText("$sensorType: $value (threshold: $threshold)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Log.d(TAG, "🔔 Notification sent: $deviceName - $sensorType = $value")
    }

    // ==========================================
    // ROOM RENAMING
    // ==========================================

    fun saveRoomName(deviceId: String, customName: String) {
        roomPrefs.edit().putString(deviceId, customName).apply()

        val currentRooms = _rooms.value.toMutableList()
        val index = currentRooms.indexOfFirst { it.id == deviceId }
        if (index != -1) {
            currentRooms[index] = currentRooms[index].copy(name = customName)
            _rooms.value = currentRooms
        }
    }

    fun getRoomName(deviceId: String): String {
        return roomPrefs.getString(deviceId, null) ?: deviceId.uppercase().replace("_", " ")
    }

    // ==========================================
    // MQTT CONNECTION
    // ==========================================

    fun connectToMqtt() {
        viewModelScope.launch {
            try {
                val config = _mqttConfig.value

                if (config.username.isEmpty() || config.password.isEmpty()) {
                    _errorMessage.value = "Configure MQTT credentials"
                    return@launch
                }

                mqttHelper = MQTTHelper(
                    context = getApplication(),
                    config = config,
                    onMessageReceived = { topic, message ->
                        handleIncomingMessage(topic, message)
                    },
                    onConnectionStatusChanged = { connected ->
                        _isConnected.value = connected
                        if (connected) {
                            _errorMessage.value = null
                        }
                    }
                )

                mqttHelper?.connect()

            } catch (e: Exception) {
                Log.e(TAG, "MQTT error", e)
                _errorMessage.value = "Connection failed"
            }
        }
    }

    fun disconnectFromMqtt() {
        mqttHelper?.disconnect()
        _isConnected.value = false
    }

    // ==========================================
    // MQTT MESSAGE HANDLING
    // ==========================================

    private fun handleIncomingMessage(topic: String, message: String) {
        try {
            val parts = topic.split("/")
            if (parts.size < 3 || parts[0] != "SmartHome") return

            val deviceId = parts[1]
            val messageType = parts[2]

            // Filter out "app" - it's the Android app status, not an IoT device
            if (deviceId == "app") {
                Log.d(TAG, "Ignoring app status message")
                return
            }

            val currentRooms = _rooms.value.toMutableList()
            var room = currentRooms.find { it.id == deviceId }

            if (room == null) {
                room = Room(
                    id = deviceId,
                    name = getRoomName(deviceId),
                    isConnected = true,
                    lastUpdate = System.currentTimeMillis()
                )
                currentRooms.add(room)
            }

            val updatedRoom = when (messageType) {
                "state" -> parseStateMessage(room, message)
                "data" -> parseDataMessage(room, message)
                "info" -> parseInfoMessage(room, message)
                else -> room
            }

            val index = currentRooms.indexOfFirst { it.id == deviceId }
            if (index != -1) {
                currentRooms[index] = updatedRoom.copy(
                    isConnected = true,
                    lastUpdate = System.currentTimeMillis()
                )
            }

            _rooms.value = currentRooms

        } catch (e: Exception) {
            Log.e(TAG, "Message parse error", e)
        }
    }

    private fun parseStateMessage(room: Room, message: String): Room {
        return try {
            val stateMsg = gson.fromJson(message, MqttStateMessage::class.java)
            room.copy(
                actuators = Actuators(
                    light = stateMsg.light,
                    fan = stateMsg.fan,
                    ac = stateMsg.ac,
                    mode = stateMsg.mode,
                    interval = stateMsg.interval
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "State parse error", e)
            room
        }
    }

    private fun parseDataMessage(room: Room, message: String): Room {
        return try {
            val dataMsg = gson.fromJson(message, MqttDataMessage::class.java)

            // Check thresholds and send notifications if needed
            val thresholds = getThresholdSettings()
            val deviceName = room.name

            // Temperature check
            if (dataMsg.temperature > thresholds.tempThreshold) {
                sendNotification(
                    deviceName,
                    "Temperature",
                    "${dataMsg.temperature}°C",
                    "${thresholds.tempThreshold}°C"
                )
            }

            // Humidity check
            if (dataMsg.humidity > thresholds.humidThreshold) {
                sendNotification(
                    deviceName,
                    "Humidity",
                    "${dataMsg.humidity}%",
                    "${thresholds.humidThreshold}%"
                )
            }

            // Light check
            if (dataMsg.light > thresholds.luxThreshold) {
                sendNotification(
                    deviceName,
                    "Light",
                    "${dataMsg.light} lux",
                    "${thresholds.luxThreshold} lux"
                )
            }

            room.copy(
                sensors = Sensors(
                    temperature = dataMsg.temperature,
                    humidity = dataMsg.humidity,
                    light = dataMsg.light
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Data parse error", e)
            room
        }
    }

    private fun parseInfoMessage(room: Room, message: String): Room {
        return try {
            val infoMsg = gson.fromJson(message, MqttInfoMessage::class.java)
            room.copy(
                deviceInfo = DeviceInfo(
                    ip = infoMsg.ip,
                    ssid = infoMsg.ssid,
                    firmware = infoMsg.firmware,
                    mac = infoMsg.mac
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Info parse error", e)
            room
        }
    }

    // ==========================================
    // MQTT COMMAND SENDING
    // ==========================================

    fun controlDevice(deviceId: String, device: String, state: Int) {
        sendCommand(deviceId, "set_device", SetDeviceParams(device, state))
    }

    fun controlAllDevices(deviceId: String, fan: Int, light: Int, ac: Int) {
        sendCommand(deviceId, "set_devices", SetDevicesParams(fan, light, ac))
    }

    fun setSystemMode(deviceId: String, mode: Int) {
        sendCommand(deviceId, "set_mode", SetModeParams(mode))
    }

    fun setSensorInterval(deviceId: String, interval: Int) {
        sendCommand(deviceId, "set_interval", SetIntervalParams(interval.coerceIn(5, 3600)))
    }

    fun rebootDevice(deviceId: String) {
        sendCommand(deviceId, "reboot", mapOf<String, Any>())
    }

    fun sendCommand(deviceId: String, command: String, params: Any) {
        try {
            if (!_isConnected.value) {
                _errorMessage.value = "Not connected"
                Log.w(TAG, "❌ Cannot send command: MQTT not connected")
                return
            }

            commandCounter++
            val commandId = "app_${String.format("%03d", commandCounter)}"

            val mqttCommand = MqttCommand(
                id = commandId,
                command = command,
                params = params
            )

            val jsonPayload = gson.toJson(mqttCommand)
            val topic = "SmartHome/$deviceId/command"

            Log.d(TAG, "📤 MQTT Command → Topic: $topic")
            Log.d(TAG, "📤 Payload: $jsonPayload")

            mqttHelper?.publish(topic, jsonPayload, qos = 1, retained = false)

        } catch (e: Exception) {
            Log.e(TAG, "Send error", e)
            _errorMessage.value = "Command failed"
        }
    }

    // ==========================================
    // MQTT CONFIGURATION
    // ==========================================

    fun updateMqttConfig(config: MqttConfig) {
        _mqttConfig.value = config
        saveMqttConfig(config)
        disconnectFromMqtt()
        connectToMqtt()
    }

    private fun saveMqttConfig(config: MqttConfig) {
        val prefs = getApplication<Application>().getSharedPreferences("mqtt_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("broker", config.broker)
            .putInt("port", config.port)
            .putString("username", config.username)
            .putString("password", config.password)
            .apply()
    }

    private fun loadMqttConfig() {
        val prefs = getApplication<Application>().getSharedPreferences("mqtt_prefs", android.content.Context.MODE_PRIVATE)
        _mqttConfig.value = MqttConfig(
            broker = prefs.getString("broker", AppConfig.MQTT_BROKER) ?: AppConfig.MQTT_BROKER,
            port = prefs.getInt("port", AppConfig.MQTT_PORT),
            username = prefs.getString("username", "") ?: "",
            password = prefs.getString("password", "") ?: ""
        )
    }

    // ==========================================
    // FIREBASE HISTORY (ASIA REGION + TYPE SAFE)
    // ==========================================

    fun fetchHistory(deviceId: String, timeRange: TimeRange = TimeRange.DAY) {
        viewModelScope.launch {
            try {
                _isLoadingChart.value = true

                val config = _firebaseConfig.value
                val currentTime = System.currentTimeMillis()

                // Use Firebase REST API with Asia region URL
                val historyData = fetchFirebaseHistory(config.databaseUrl, deviceId, 200)

                Log.d(TAG, "Raw data count: ${historyData.size}")
                Log.d(TAG, "TimeRange: ${timeRange.label}, hours: ${timeRange.hours}")

                // Filter data based on time range
                val filteredData = when {
                    timeRange.hours < 0 -> {
                        // ALL_TIME: No filtering, return all data
                        Log.d(TAG, "ALL_TIME selected - no filtering")
                        historyData.sortedBy { it.timestamp }
                    }
                    else -> {
                        // Filter by time range from current time
                        val timeThreshold = currentTime - (timeRange.hours * 3600 * 1000L)
                        Log.d(TAG, "Filtering from ${Date(timeThreshold)} to ${Date(currentTime)}")
                        historyData
                            .filter { it.timestamp >= timeThreshold }
                            .sortedBy { it.timestamp }
                    }
                }

                _chartData.value = filteredData
                Log.d(TAG, "Loaded ${filteredData.size} entries for ${timeRange.label}")

            } catch (e: Exception) {
                Log.e(TAG, "Fetch error", e)
                _errorMessage.value = "Load failed"
                _chartData.value = emptyList()
            } finally {
                _isLoadingChart.value = false
            }
        }
    }

    /**
     * FIXED: Type-safe Firebase parsing with .toFloat() + Auth token support
     */
    private suspend fun fetchFirebaseHistory(
        databaseUrl: String,
        deviceId: String,
        limitToLast: Int
    ): List<HistoryLog> = withContext(Dispatchers.IO) {
        try {
            // Get Firebase Auth token (works with Anonymous Auth too)
            val authToken = try {
                FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            } catch (e: Exception) {
                Log.w(TAG, "Auth token not available: ${e.message}")
                null
            }

            // Build URL with auth token if available
            val baseUrl = "${databaseUrl.trimEnd('/')}/history/$deviceId.json"
            val url = if (authToken != null) {
                "$baseUrl?auth=$authToken&orderBy=\"\$key\"&limitToLast=$limitToLast"
            } else {
                "$baseUrl?orderBy=\"\$key\"&limitToLast=$limitToLast"
            }

            Log.d(TAG, "Fetching from Asia region: $url")
            Log.d(TAG, "Auth token: ${if (authToken != null) "✅ Available" else "❌ Missing"}")

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 8000  // Reduced from 15s
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
                } catch (e: Exception) {
                    "Cannot read error"
                }
                Log.e(TAG, "Firebase HTTP ${connection.responseCode}: $errorBody")
                return@withContext emptyList()
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            Log.d(TAG, "Response preview: ${response.take(200)}...")

            val historyMap = try {
                gson.fromJson(response, Map::class.java) as? Map<String, Any> ?: emptyMap()
            } catch (e: Exception) {
                Log.e(TAG, "JSON parse error", e)
                emptyMap<String, Any>()
            }

            val historyList = mutableListOf<HistoryLog>()
            historyMap.forEach { (pushId, data) ->
                if (data is Map<*, *>) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val dataMap = data as Map<String, Any>

                        // FIXED: Map to actual Firebase fields from screenshot
                        // Firebase uses: temp, humid, lux, last_update (confirmed from logcat)
                        val timestamp = (dataMap["last_update"] as? Number)?.toLong() ?: 0L
                        val temp = (dataMap["temp"] as? Number)?.toFloat() ?: 0f
                        val humid = (dataMap["humid"] as? Number)?.toFloat() ?: 0f
                        val lux = (dataMap["lux"] as? Number)?.toInt() ?: 0  // Firebase uses "lux" not "light"

                        if (timestamp > 0) {
                            historyList.add(
                                HistoryLog(
                                    timestamp = timestamp,
                                    temp = temp,
                                    humid = humid,
                                    lux = lux,
                                    pushId = pushId
                                )
                            )
                        } else {
                            Log.w(TAG, "Skip entry with invalid timestamp: $pushId")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Skip entry: $pushId", e)
                    }
                }
            }

            Log.d(TAG, "Parsed ${historyList.size} entries")
            historyList

        } catch (e: Exception) {
            Log.e(TAG, "Firebase error", e)
            emptyList()
        }
    }

    // ==========================================
    // FIREBASE CONFIGURATION
    // ==========================================

    fun updateFirebaseConfig(config: FirebaseConfig) {
        _firebaseConfig.value = config
        saveFirebaseConfig(config)
    }

    private fun saveFirebaseConfig(config: FirebaseConfig) {
        val prefs = getApplication<Application>().getSharedPreferences("firebase_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("databaseUrl", config.databaseUrl)
            .putString("apiKey", config.apiKey)
            .apply()
    }

    private fun loadFirebaseConfig() {
        val prefs = getApplication<Application>().getSharedPreferences("firebase_prefs", android.content.Context.MODE_PRIVATE)
        _firebaseConfig.value = FirebaseConfig(
            databaseUrl = prefs.getString("databaseUrl", AppConfig.FIREBASE_URL) ?: AppConfig.FIREBASE_URL,
            apiKey = prefs.getString("apiKey", AppConfig.FIREBASE_DB_SECRET) ?: AppConfig.FIREBASE_DB_SECRET
        )
    }

    // ==========================================
    // UTILITY
    // ==========================================

    fun clearChartData() {
        _chartData.value = emptyList()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        mqttHelper?.cleanup()
    }
}
