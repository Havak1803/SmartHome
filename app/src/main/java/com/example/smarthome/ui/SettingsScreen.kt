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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthome.model.FirebaseConfig
import com.example.smarthome.model.MqttConfig
import com.example.smarthome.model.Room
import com.example.smarthome.ui.theme.PrimaryBlue

/**
 * SettingsScreen - Version 3.1
 * MQTT + Firebase Configuration and Device Management
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
    onConnect: () -> Unit
) {
    var broker by remember { mutableStateOf(mqttConfig.broker) }
    var port by remember { mutableStateOf(mqttConfig.port.toString()) }
    var username by remember { mutableStateOf(mqttConfig.username) }
    var password by remember { mutableStateOf(mqttConfig.password) }
    var showPassword by remember { mutableStateOf(false) }

    var firebaseUrl by remember { mutableStateOf(firebaseConfig.databaseUrl) }
    var firebaseApiKey by remember { mutableStateOf(firebaseConfig.apiKey) }
    var showFirebaseKey by remember { mutableStateOf(false) }

    var selectedDeviceId by remember { mutableStateOf("") }
    var intervalInput by remember { mutableStateOf("5") }

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
            // MQTT Configuration Section
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
                            imageVector = Icons.Rounded.Cloud,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "MQTT Broker Configuration",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Divider()

                    // Broker Host
                    OutlinedTextField(
                        value = broker,
                        onValueChange = { broker = it },
                        label = { Text("Broker Host") },
                        placeholder = { Text("broker.hivemq.com") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Dns, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Port
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        placeholder = { Text("8883") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Pin, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Username
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        placeholder = { Text("mqtt_user") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        placeholder = { Text("Enter password") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff
                                    else Icons.Rounded.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Connection Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = if (isConnected) Color.Green else Color.Red,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                            Text(
                                text = if (isConnected) "Connected" else "Disconnected",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (isConnected) {
                            TextButton(onClick = onDisconnect) {
                                Text("Disconnect")
                            }
                        }
                    }

                    // Save Button
                    Button(
                        onClick = {
                            val newConfig = MqttConfig(
                                broker = broker,
                                port = port.toIntOrNull() ?: 8883,
                                username = username,
                                password = password
                            )
                            onSaveMqttConfig(newConfig)
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
                        Text("Save & Connect", fontSize = 16.sp)
                    }
                }
            }

            // Firebase Configuration Section (NEW in v3.1)
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
                            imageVector = Icons.Rounded.CloudSync,
                            contentDescription = null,
                            tint = Color(0xFFFFAB00),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Firebase Configuration",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Divider()

                    // Firebase Database URL
                    OutlinedTextField(
                        value = firebaseUrl,
                        onValueChange = { firebaseUrl = it },
                        label = { Text("Database URL") },
                        placeholder = { Text("https://your-project.firebasedatabase.app/") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Link, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Firebase API Key (Optional)
                    OutlinedTextField(
                        value = firebaseApiKey,
                        onValueChange = { firebaseApiKey = it },
                        label = { Text("API Key (Optional)") },
                        placeholder = { Text("Enter API key if required") },
                        leadingIcon = {
                            Icon(Icons.Rounded.VpnKey, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showFirebaseKey = !showFirebaseKey }) {
                                Icon(
                                    imageVector = if (showFirebaseKey) Icons.Rounded.VisibilityOff
                                    else Icons.Rounded.Visibility,
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        },
                        visualTransformation = if (showFirebaseKey) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Save Firebase Config Button
                    Button(
                        onClick = {
                            val newFirebaseConfig = FirebaseConfig(
                                databaseUrl = firebaseUrl,
                                apiKey = firebaseApiKey
                            )
                            onSaveFirebaseConfig(newFirebaseConfig)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFAB00)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Firebase Config", fontSize = 16.sp)
                    }
                }
            }

            // Device Management Section
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
                            imageVector = Icons.Rounded.DevicesOther,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Device Management",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Divider()

                    if (rooms.isEmpty()) {
                        Text(
                            text = "No devices available. Connect to MQTT to see devices.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        // Device Selector Dropdown
                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = if (selectedDeviceId.isEmpty()) "Select Device"
                                else rooms.find { it.id == selectedDeviceId }?.name ?: selectedDeviceId,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Device") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                rooms.forEach { room ->
                                    DropdownMenuItem(
                                        text = { Text(room.name) },
                                        onClick = {
                                            selectedDeviceId = room.id
                                            intervalInput = room.actuators.interval.toString()
                                            expanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.Memory,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedDeviceId.isNotEmpty()) {
                            // Interval Configuration
                            OutlinedTextField(
                                value = intervalInput,
                                onValueChange = { intervalInput = it },
                                label = { Text("Sensor Interval (seconds)") },
                                placeholder = { Text("5") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Timer, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                supportingText = {
                                    Text("Range: 5 - 3600 seconds")
                                }
                            )

                            // Set Interval Button
                            Button(
                                onClick = {
                                    val interval = intervalInput.toIntOrNull() ?: 5
                                    val clampedInterval = interval.coerceIn(5, 3600)
                                    onSetInterval(selectedDeviceId, clampedInterval)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryBlue
                                ),
                                shape = RoundedCornerShape(8.dp),
                                enabled = isConnected
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Update,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Set Interval")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Reboot Button
                            var showRebootDialog by remember { mutableStateOf(false) }

                            OutlinedButton(
                                onClick = { showRebootDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF5252)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    Color(0xFFFF5252)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                enabled = isConnected
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reboot Device", fontWeight = FontWeight.Bold)
                            }

                            // Reboot Confirmation Dialog
                            if (showRebootDialog) {
                                AlertDialog(
                                    onDismissRequest = { showRebootDialog = false },
                                    icon = {
                                        Icon(
                                            Icons.Rounded.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFF5252)
                                        )
                                    },
                                    title = { Text("Reboot Device?") },
                                    text = {
                                        Text(
                                            "This will restart the ESP32 device. " +
                                                    "It will be offline for about 2 seconds."
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                onRebootDevice(selectedDeviceId)
                                                showRebootDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFF5252)
                                            )
                                        ) {
                                            Text("Reboot")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showRebootDialog = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // App Info Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "About",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Divider()

                    InfoRow(label = "App Version", value = "3.1 (MQTT + Firebase)")
                    InfoRow(label = "MQTT Protocol", value = "v3.1.1 (HiveMQ)")
                    InfoRow(label = "Firebase", value = "Realtime Database")
                    InfoRow(label = "Library", value = "Eclipse Paho")
                    InfoRow(label = "Security", value = "SSL/TLS")
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

/**
 * Info Row Component
 */
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
