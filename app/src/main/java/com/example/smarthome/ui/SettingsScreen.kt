package com.example.smarthome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthome.model.FirebaseConfig
import com.example.smarthome.model.MqttConfig
import com.example.smarthome.model.Room
import com.example.smarthome.ui.theme.PrimaryBlue
import com.example.smarthome.viewmodel.SmartHomeViewModel

/**
 * SettingsScreen - Version 3.5
 * Threshold Settings + Push Notifications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mqttConfig: MqttConfig,
    firebaseConfig: FirebaseConfig,
    rooms: List<Room>,
    isConnected: Boolean,
    onSaveMqttConfig: (MqttConfig) -> Unit,
    onSaveFirebaseConfig: (FirebaseConfig) -> Unit,
    onSetInterval: (deviceId: String, interval: Int) -> Unit,
    onRebootDevice: (deviceId: String) -> Unit,
    onDisconnect: () -> Unit,
    onConnect: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SmartHomeViewModel
) {
    // HARDCODED MQTT CREDENTIALS - Auto connect
    val broker = mqttConfig.broker
    val port = mqttConfig.port.toString()
    val username = "SmartHome"
    val password = "SmartHome01"

    // Threshold settings
    var tempThreshold by remember { mutableStateOf(viewModel.getThresholdSettings().tempThreshold.toString()) }
    var humidThreshold by remember { mutableStateOf(viewModel.getThresholdSettings().humidThreshold.toString()) }
    var luxThreshold by remember { mutableStateOf(viewModel.getThresholdSettings().luxThreshold.toString()) }

    // Notification enabled state
    val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsState()

    // Auto-connect on launch with hardcoded credentials
    LaunchedEffect(Unit) {
        if (!isConnected && username.isNotEmpty() && password.isNotEmpty()) {
            val autoConfig = MqttConfig(
                broker = broker,
                port = port.toIntOrNull() ?: 8883,
                username = username,
                password = password
            )
            onSaveMqttConfig(autoConfig)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        SettingsTopBar()

        // Settings Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Threshold Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Thermostat,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Threshold Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Divider()

                    Text(
                        text = "Set alert thresholds for sensor values",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // Temperature Threshold
                    OutlinedTextField(
                        value = tempThreshold,
                        onValueChange = { tempThreshold = it },
                        label = { Text("Temperature Threshold (°C)") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Thermostat, null, tint = Color(0xFFFF5252))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF5252),
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    // Humidity Threshold
                    OutlinedTextField(
                        value = humidThreshold,
                        onValueChange = { humidThreshold = it },
                        label = { Text("Humidity Threshold (%)") },
                        leadingIcon = {
                            Icon(Icons.Rounded.WaterDrop, null, tint = Color(0xFF2196F3))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    // Light Threshold
                    OutlinedTextField(
                        value = luxThreshold,
                        onValueChange = { luxThreshold = it },
                        label = { Text("Light Threshold (lux)") },
                        leadingIcon = {
                            Icon(Icons.Rounded.LightMode, null, tint = Color(0xFFFFAB00))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFAB00),
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    // Save Button
                    Button(
                        onClick = {
                            val temp = tempThreshold.toFloatOrNull() ?: 30f
                            val humid = humidThreshold.toFloatOrNull() ?: 70f
                            val lux = luxThreshold.toIntOrNull() ?: 500
                            viewModel.saveThresholdSettings(temp, humid, lux)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Thresholds", fontSize = 16.sp)
                    }
                }
            }

            // Push Notification Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = Color(0xFFFFAB00),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Notifications",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Divider()

                    // Master Push Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isNotificationEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                                contentDescription = null,
                                tint = if (isNotificationEnabled) Color(0xFF4CAF50) else Color.Gray
                            )
                            Column {
                                Text(
                                    text = "Push Notifications",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isNotificationEnabled) "Alerts enabled" else "Alerts disabled",
                                    fontSize = 12.sp,
                                    color = if (isNotificationEnabled) Color(0xFF4CAF50) else Color.Gray
                                )
                            }
                        }

                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = { viewModel.setNotificationEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4CAF50)
                            )
                        )
                    }

                    Text(
                        text = "Receive alerts when sensor values exceed thresholds",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Account Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Account",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Divider()

                    // Logout Button
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5252)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout", fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Settings Top Bar
 */
@Composable
fun SettingsTopBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PrimaryBlue,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
