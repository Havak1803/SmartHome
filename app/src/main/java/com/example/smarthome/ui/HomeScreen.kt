package com.example.smarthome.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smarthome.model.Room
import com.example.smarthome.ui.theme.*
import com.example.smarthome.viewmodel.SmartHomeViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * HomeScreen - V3.4 MAJOR UPDATE
 * Features: Add Device button, Master Control, Room Detail Popup with 4 action buttons
 */
@Composable
fun HomeScreen(
    rooms: List<Room>,
    isConnected: Boolean,
    viewModel: SmartHomeViewModel
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedRoom by remember { mutableStateOf<Room?>(null) }
    var showRoomDetailDialog by remember { mutableStateOf(false) }
    var showAddDeviceDialog by remember { mutableStateOf(false) }

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
                        },
                        onRoomClick = {
                            selectedRoom = room
                            showRoomDetailDialog = true
                        },
                        onMasterToggle = { mode ->
                            viewModel.setSystemMode(room.id, mode)
                        }
                    )
                }

                // Add Device Button at bottom
                item {
                    AddDeviceButton(
                        onClick = { showAddDeviceDialog = true }
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

    // Room Detail Dialog
    if (showRoomDetailDialog && selectedRoom != null) {
        RoomDetailDialog(
            room = selectedRoom!!,
            onDismiss = { showRoomDetailDialog = false },
            viewModel = viewModel
        )
    }

    // Add Device Info Dialog
    if (showAddDeviceDialog) {
        AddDeviceInfoDialog(
            onDismiss = { showAddDeviceDialog = false }
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
    onRenameClick: () -> Unit,
    onRoomClick: () -> Unit,
    onMasterToggle: (Int) -> Unit
) {
    var fanState by remember(room.actuators.fan) { mutableStateOf(room.actuators.fan) }
    var lightState by remember(room.actuators.light) { mutableStateOf(room.actuators.light) }
    var acState by remember(room.actuators.ac) { mutableStateOf(room.actuators.ac) }
    var masterMode by remember(room.actuators.mode) { mutableStateOf(room.actuators.mode) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRoomClick() },
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with Room Name, Master Toggle, and Rename Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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

                // Master Power Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (masterMode == 1) "ON" else "OFF",
                        color = if (masterMode == 1) SuccessGreen else TextGrey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = masterMode == 1,
                        onCheckedChange = { checked ->
                            val newMode = if (checked) 1 else 0
                            masterMode = newMode
                            onMasterToggle(newMode)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = SuccessGreen,
                            uncheckedThumbColor = TextGrey,
                            uncheckedTrackColor = BlueGrey
                        )
                    )
                    IconButton(onClick = onRenameClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = ElectricBlue
                        )
                    }
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
                    },
                    masterEnabled = masterMode == 1
                )
                DeviceControlButton(
                    icon = Icons.Default.Lightbulb,
                    label = "Light",
                    isOn = lightState == 1,
                    onClick = {
                        val newState = if (lightState == 1) 0 else 1
                        lightState = newState
                        onDeviceControl("light", newState)
                    },
                    masterEnabled = masterMode == 1
                )
                DeviceControlButton(
                    icon = Icons.Default.AcUnit,
                    label = "AC",
                    isOn = acState == 1,
                    onClick = {
                        val newState = if (acState == 1) 0 else 1
                        acState = newState
                        onDeviceControl("ac", newState)
                    },
                    masterEnabled = masterMode == 1
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
    onClick: () -> Unit,
    masterEnabled: Boolean = true
) {
    // Hiển thị màu xám nếu master OFF, ngược lại hiển thị theo trạng thái thiết bị
    val displayOn = masterEnabled && isOn

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            onClick = onClick,
            enabled = masterEnabled
        )
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (displayOn) ElectricBlue
                    else BlueGrey.copy(alpha = if (masterEnabled) 1f else 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (displayOn) TextWhite else TextGrey,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = if (displayOn) ElectricBlue else TextGrey,
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
// ==========================================
// ADD DEVICE BUTTON
// ==========================================

@Composable
fun AddDeviceButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = DarkNavyBlue.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Device",
                tint = ElectricBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Add New Device",
                color = ElectricBlue,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ==========================================
// ADD DEVICE INFO DIALOG
// ==========================================

@Composable
fun AddDeviceInfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "How to Add Device",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "1. Make sure your ESP32 device is powered on\n" +
                            "2. ESP32 should connect to MQTT broker\n" +
                            "3. Device will auto-appear in Home screen\n" +
                            "4. You can rename it using Edit button",
                    color = TextGrey,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got it", color = TextWhite)
                }
            }
        }
    }
}

// ==========================================
// ROOM DETAIL DIALOG WITH 4 ACTION BUTTONS
// ==========================================

@Composable
fun RoomDetailDialog(
    room: Room,
    onDismiss: () -> Unit,
    viewModel: SmartHomeViewModel
) {
    var showIntervalDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    var showWiFiDialog by remember { mutableStateOf(false) }
    var showRebootDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = room.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Device: ${room.id}",
                            fontSize = 12.sp,
                            color = TextGrey
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = TextGrey)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sensor Info
                Card(
                    colors = CardDefaults.cardColors(containerColor = BlueGrey.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sensor Data", color = TextGrey, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow("Temperature", "${String.format("%.1f", room.sensors.temperature)}°C", TemperatureColor)
                        DetailRow("Humidity", "${String.format("%.1f", room.sensors.humidity)}%", HumidityColor)
                        DetailRow("Light", "${room.sensors.light} lux", LightColor)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Device Info
                Card(
                    colors = CardDefaults.cardColors(containerColor = BlueGrey.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Device Info", color = TextGrey, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow("IP Address", room.deviceInfo.ip.ifEmpty { "N/A" }, ElectricBlue)
                        DetailRow("WiFi SSID", room.deviceInfo.ssid.ifEmpty { "N/A" }, ElectricBlue)
                        DetailRow("Firmware", room.deviceInfo.firmware.ifEmpty { "N/A" }, ElectricBlue)
                        DetailRow("Interval", "${room.actuators.interval}s", WarningAmber)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4 Action Buttons
                Text("Actions", color = TextGrey, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionButton(
                        icon = Icons.Default.Timer,
                        label = "Interval",
                        color = ElectricBlue,
                        onClick = { showIntervalDialog = true }
                    )
                    ActionButton(
                        icon = Icons.Default.Schedule,
                        label = "Set Time",
                        color = WarningAmber,
                        onClick = { showTimeDialog = true }
                    )
                    ActionButton(
                        icon = Icons.Default.Wifi,
                        label = "WiFi",
                        color = SuccessGreen,
                        onClick = { showWiFiDialog = true }
                    )
                    ActionButton(
                        icon = Icons.Default.PowerSettingsNew,
                        label = "Reboot",
                        color = ErrorRed,
                        onClick = { showRebootDialog = true }
                    )
                }
            }
        }
    }

    // Sub-dialogs
    if (showIntervalDialog) {
        SetIntervalDialog(
            currentInterval = room.actuators.interval,
            onDismiss = { showIntervalDialog = false },
            onConfirm = { newInterval ->
                viewModel.setSensorInterval(room.id, newInterval)
                showIntervalDialog = false
            }
        )
    }

    if (showTimeDialog) {
        SetTimeDialog(
            deviceId = room.id,
            onDismiss = { showTimeDialog = false },
            viewModel = viewModel
        )
    }

    if (showWiFiDialog) {
        WiFiConfigDialog(
            deviceId = room.id,
            onDismiss = { showWiFiDialog = false }
        )
    }

    if (showRebootDialog) {
        RebootConfirmDialog(
            deviceName = room.name,
            onDismiss = { showRebootDialog = false },
            onConfirm = {
                viewModel.rebootDevice(room.id)
                showRebootDialog = false
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextGrey, fontSize = 13.sp)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextGrey,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
// SET INTERVAL DIALOG
// ==========================================

@Composable
fun SetIntervalDialog(
    currentInterval: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var intervalInput by remember { mutableStateOf(currentInterval.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Set Measurement Interval",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = intervalInput,
                    onValueChange = { intervalInput = it },
                    label = { Text("Interval (seconds)", color = TextGrey) },
                    placeholder = { Text("5", color = TextGrey) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BlueGrey,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = ElectricBlue
                    )
                )

                Text(
                    text = "Range: 5 - 3600 seconds",
                    fontSize = 12.sp,
                    color = TextGrey,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = BlueGrey)
                    }
                    Button(
                        onClick = {
                            val interval = intervalInput.toIntOrNull() ?: 5
                            onConfirm(interval.coerceIn(5, 3600))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Apply", color = TextWhite)
                    }
                }
            }
        }
    }
}

// ==========================================
// SET TIME DIALOG
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetTimeDialog(
    deviceId: String,
    onDismiss: () -> Unit,
    viewModel: SmartHomeViewModel
) {
    var timeInput by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Set ESP32 Time",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter the time to sync with ESP32 device.",
                    color = TextGrey,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Editable time input
                OutlinedTextField(
                    value = timeInput,
                    onValueChange = { timeInput = it },
                    label = { Text("Date & Time", color = TextGrey) },
                    placeholder = { Text("yyyy-MM-dd HH:mm:ss", color = BlueGrey) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarningAmber,
                        unfocusedBorderColor = BlueGrey,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = WarningAmber
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Sync current time button
                TextButton(
                    onClick = {
                        timeInput = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Use Current Time", color = ElectricBlue, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = BlueGrey)
                    }
                    Button(
                        onClick = {
                            try {
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                val timestamp = dateFormat.parse(timeInput)?.time ?: System.currentTimeMillis()
                                viewModel.sendCommand(deviceId, "set_time", mapOf("timestamp" to timestamp / 1000))
                            } catch (e: Exception) {
                                // If parsing fails, use current time
                                viewModel.sendCommand(deviceId, "set_time", mapOf("timestamp" to System.currentTimeMillis() / 1000))
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                    ) {
                        Text("Set Time", color = TextWhite)
                    }
                }
            }
        }
    }
}

// ==========================================
// WIFI CONFIG DIALOG
// ==========================================

@Composable
fun WiFiConfigDialog(
    deviceId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "WiFi Configuration Guide",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Text(
                    text = "for \"$deviceId\"",
                    fontSize = 14.sp,
                    color = TextGrey,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📶 Step 1: Connect to ESP32 WiFi",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Open WiFi settings on your phone\n" +
                                    "2. Find network: SmartHome\n" +
                                    "3. Password: 12345678",
                            color = TextGrey,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = ElectricBlue.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://192.168.4.1"))
                        context.startActivity(intent)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🌐 Step 2: Open Config Page",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap here to open browser",
                                color = ElectricBlue,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            tint = ElectricBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✅ Step 3: Enter WiFi Info",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Select your home WiFi\n" +
                                    "2. Enter WiFi password\n" +
                                    "3. Click Save/Connect",
                            color = TextGrey,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = TextWhite)
                }
            }
        }
    }
}

// ==========================================
// REBOOT CONFIRM DIALOG
// ==========================================

@Composable
fun RebootConfirmDialog(
    deviceName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Reboot Device?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "This will restart \"$deviceName\".\nDevice will be offline for ~2 seconds.",
                    color = TextGrey,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = BlueGrey)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Text("Reboot", color = TextWhite)
                    }
                }
            }
        }
    }
}