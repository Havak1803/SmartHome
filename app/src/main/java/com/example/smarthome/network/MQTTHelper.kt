package com.example.smarthome.network

import android.content.Context
import android.util.Log
import com.example.smarthome.model.MqttConfig
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * MQTT Helper Class for HiveMQ Cloud with SSL/TLS Support
 * Version 3.0 - Complete rewrite for MQTT protocol
 */
class MQTTHelper(
    private val context: Context,
    private val config: MqttConfig,
    private val onMessageReceived: (topic: String, message: String) -> Unit,
    private val onConnectionStatusChanged: (Boolean) -> Unit
) {
    private var mqttClient: MqttAsyncClient? = null
    private val TAG = "MQTTHelper"

    // Track connection state
    private var isConnected = false

    /**
     * Initialize and connect to MQTT broker with SSL/TLS
     */
    fun connect() {
        try {
            val serverUri = "ssl://${config.broker}:${config.port}"
            Log.d(TAG, "Connecting to MQTT Broker: $serverUri")

            // Initialize MQTT client with memory persistence
            mqttClient = MqttAsyncClient(serverUri, config.clientId, MemoryPersistence())

            val options = MqttConnectOptions().apply {
                userName = config.username
                password = config.password.toCharArray()
                isCleanSession = true
                connectionTimeout = 30
                keepAliveInterval = 60
                isAutomaticReconnect = true

                // CRITICAL: SSL/TLS Socket Factory for HiveMQ Cloud
                socketFactory = getSocketFactory()

                // Set Last Will and Testament (optional but recommended)
                setWill(
                    "SmartHome/app/status",
                    "offline".toByteArray(),
                    1,
                    false
                )
            }

            // Set callback before connecting
            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e(TAG, "Connection lost: ${cause?.message}")
                    isConnected = false
                    onConnectionStatusChanged(false)

                    // Auto-reconnect is enabled, but we can handle additional logic here
                    if (cause != null) {
                        Log.e(TAG, "Attempting to reconnect...", cause)
                    }
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    val payload = String(message.payload)
                    Log.d(TAG, "Message arrived - Topic: $topic, Payload: $payload")

                    try {
                        onMessageReceived(topic, payload)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing message", e)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Message delivery complete")
                }
            })

            // Connect asynchronously
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connected to MQTT Broker successfully!")
                    isConnected = true
                    onConnectionStatusChanged(true)

                    // Subscribe to all SmartHome topics after successful connection
                    subscribeToTopics()

                    // Publish online status
                    publish("SmartHome/app/status", "online", qos = 1, retained = false)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Failed to connect to MQTT Broker", exception)
                    isConnected = false
                    onConnectionStatusChanged(false)
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "Error during MQTT initialization", e)
            onConnectionStatusChanged(false)
        }
    }

    /**
     * Subscribe to SmartHome topics (wildcard subscription)
     */
    private fun subscribeToTopics() {
        try {
            val topics = arrayOf(
                "SmartHome/#"  // Subscribe to all SmartHome topics
            )
            val qos = intArrayOf(1)  // QoS 1 - At least once delivery

            mqttClient?.subscribe(topics, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Successfully subscribed to: ${topics.joinToString(", ")}")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Failed to subscribe to topics", exception)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to topics", e)
        }
    }

    /**
     * Publish a message to a specific topic
     *
     * @param topic The MQTT topic to publish to
     * @param message The message payload (usually JSON string)
     * @param qos Quality of Service (0, 1, or 2)
     * @param retained Whether the message should be retained by broker
     */
    fun publish(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
        try {
            if (!isConnected) {
                Log.w(TAG, "Cannot publish - not connected to broker")
                return
            }

            val mqttMessage = MqttMessage(message.toByteArray()).apply {
                this.qos = qos
                isRetained = retained
            }

            mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Published to $topic: $message")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Failed to publish to $topic", exception)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing message to $topic", e)
        }
    }

    /**
     * Disconnect from MQTT broker
     */
    fun disconnect() {
        try {
            if (isConnected) {
                // Publish offline status before disconnecting
                publish("SmartHome/app/status", "offline", qos = 1, retained = false)

                mqttClient?.disconnect(null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG, "Disconnected from MQTT Broker")
                        isConnected = false
                        onConnectionStatusChanged(false)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e(TAG, "Error disconnecting", exception)
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }

    /**
     * Check if currently connected to broker
     */
    fun isConnected(): Boolean = isConnected && mqttClient?.isConnected == true

    /**
     * Get SSL Socket Factory for HiveMQ Cloud TLS connection
     *
     * CRITICAL: This method creates a trust-all SSL context.
     * For production apps, implement proper certificate validation!
     */
    private fun getSocketFactory(): SSLSocketFactory {
        return try {
            // Create a trust manager that trusts all certificates
            // WARNING: In production, validate certificates properly!
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            Log.d(TAG, "SSL Socket Factory created successfully")
            sslContext.socketFactory
        } catch (e: Exception) {
            Log.e(TAG, "Error creating SSL Socket Factory", e)
            throw RuntimeException("Failed to create SSL Socket Factory", e)
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            disconnect()
            mqttClient?.close()
            mqttClient = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
