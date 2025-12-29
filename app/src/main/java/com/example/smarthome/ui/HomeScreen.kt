package com.example.smarthome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smarthome.model.Room
import com.example.smarthome.ui.theme.*
import com.example.smarthome.viewmodel.SmartHomeViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * HomeScreen - V3.1 Midnight Deep Blue Edition
 * Features: Dark theme, Room renaming, MQTT control
 */
@Composable
fun HomeScreen(
    rooms: List<Room>,
    isConnected: Boolean,
    viewModel: SmartHomeViewModel
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedRoom by remember { mutableStateOf<Room?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkBlue)
            .padding(16.dp)
    ) {
        // Connection Status Banner
        ConnectionStatusCard(isConnected)

        Spacer(modifier = Modifier.height(16.dp))

        // Rooms List
        if (rooms.isEmpty()) {
            EmptyStateCard()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rooms) { room ->
                    RoomCard(
                        room = room,
                        onDeviceControl = { device, state ->
                            viewModel.controlDevice(room.id, device, state)
                        },
                        onRenameClick = {
                            selectedRoom = room
                            showRenameDialog = true
                        }
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog && selectedRoom != null) {
        RenameRoomDialog(
            currentName = selectedRoom!!.name,
            deviceId = selectedRoom!!.id,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                viewModel.saveRoomName(selectedRoom!!.id, newName)
                showRenameDialog = false
            }
        )
    }
}

@Composable
fun ConnectionStatusCard(isConnected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) DarkNavyBlue else DarkNavyBlue.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) SuccessGreen else ErrorRed)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isConnected) "MQTT Connected" else "Disconnected",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = null,
                tint = if (isConnected) SuccessGreen else ErrorRed
            )
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DevicesOther,
                contentDescription = null,
                tint = BlueGrey,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Devices Found",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Waiting for ESP32 devices...",
                color = TextGrey,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun RoomCard(
    room: Room,
    onDeviceControl: (String, Int) -> Unit,
    onRenameClick: () -> Unit
) {
    var fanState by remember(room.actuators.fan) { mutableStateOf(room.actuators.fan) }
    var lightState by remember(room.actuators.light) { mutableStateOf(room.actuators.light) }
    var acState by remember(room.actuators.ac) { mutableStateOf(room.actuators.ac) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with Room Name and Rename Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = room.name,
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${room.id}",
                        color = TextGrey,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onRenameClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = ElectricBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sensor Readings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SensorChip(
                    icon = Icons.Default.Thermostat,
                    value = "${String.format("%.1f", room.sensors.temperature)}°C",
                    color = TemperatureColor
                )
                SensorChip(
                    icon = Icons.Default.WaterDrop,
                    value = "${String.format("%.1f", room.sensors.humidity)}%",
                    color = HumidityColor
                )
                SensorChip(
                    icon = Icons.Default.LightMode,
                    value = "${room.sensors.light} lux",
                    color = LightColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Device Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DeviceControlButton(
                    icon = Icons.Default.Air,
                    label = "Fan",
                    isOn = fanState == 1,
                    onClick = {
                        val newState = if (fanState == 1) 0 else 1
                        fanState = newState
                        onDeviceControl("fan", newState)
                    }
                )
                DeviceControlButton(
                    icon = Icons.Default.Lightbulb,
                    label = "Light",
                    isOn = lightState == 1,
                    onClick = {
                        val newState = if (lightState == 1) 0 else 1
                        lightState = newState
                        onDeviceControl("light", newState)
                    }
                )
                DeviceControlButton(
                    icon = Icons.Default.AcUnit,
                    label = "AC",
                    isOn = acState == 1,
                    onClick = {
                        val newState = if (acState == 1) 0 else 1
                        acState = newState
                        onDeviceControl("ac", newState)
                    }
                )
            }
        }
    }
}

@Composable
fun SensorChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            color = TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DeviceControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isOn: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isOn) ElectricBlue else BlueGrey),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextWhite,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = if (isOn) ElectricBlue else TextGrey,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RenameRoomDialog(
    currentName: String,
    deviceId: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Rename Room",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Room Name", color = TextGrey) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BlueGrey,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = ElectricBlue
                    )
                )

                Text(
                    text = "Device ID: $deviceId",
                    fontSize = 12.sp,
                    color = TextGrey,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = BlueGrey)
                    }
                    Button(
                        onClick = { onConfirm(newName) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        enabled = newName.isNotBlank()
                    ) {
                        Text("Save", color = TextWhite)
                    }
                }
            }
        }
    }
}
