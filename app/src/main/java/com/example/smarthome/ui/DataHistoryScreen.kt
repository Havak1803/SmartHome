package com.example.smarthome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthome.model.DataHistoryFilter
import com.example.smarthome.model.HistoryLog
import com.example.smarthome.model.Room
import com.example.smarthome.ui.theme.*
import com.example.smarthome.viewmodel.SmartHomeViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * DataHistoryScreen - V3.4
 * Table view with filters for historical data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataHistoryScreen(
    rooms: List<Room>,
    historyData: List<HistoryLog>,
    isLoading: Boolean,
    viewModel: SmartHomeViewModel
) {
    var selectedDevice by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(DataHistoryFilter.TODAY) }

    // Collect error state
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Filter data by time
    val filteredData = remember(historyData, selectedFilter) {
        val now = System.currentTimeMillis()
        when (selectedFilter) {
            DataHistoryFilter.TODAY -> {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                historyData.filter { it.timestamp >= startOfDay }
            }
            DataHistoryFilter.THIS_WEEK -> {
                val weekAgo = now - (7 * 24 * 60 * 60 * 1000L)
                historyData.filter { it.timestamp >= weekAgo }
            }
            DataHistoryFilter.THIS_MONTH -> {
                val monthAgo = now - (30 * 24 * 60 * 60 * 1000L)
                historyData.filter { it.timestamp >= monthAgo }
            }
            DataHistoryFilter.ALL_TIME -> historyData
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkBlue)
            .padding(16.dp)
    ) {
        // Error Banner
        errorMessage?.let { error ->
            if (error.contains("Firebase") || error.contains("401") || error.contains("Load failed")) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cannot load Firebase data (Check Settings)",
                            color = TextWhite,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Device Selector
        if (rooms.isNotEmpty()) {
            DataHistoryDeviceSelectorCard(
                rooms = rooms,
                selectedDevice = selectedDevice,
                onDeviceSelected = { deviceId ->
                    selectedDevice = deviceId
                    viewModel.fetchHistory(deviceId, com.example.smarthome.model.TimeRange.MONTH)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Filter Buttons
        if (selectedDevice != null) {
            FilterButtonRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Data Count Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Records",
                        color = TextGrey,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${filteredData.size} entries",
                        color = ElectricBlue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Data Table
            if (isLoading) {
                LoadingView()
            } else if (filteredData.isEmpty()) {
                EmptyDataView()
            } else {
                DataTable(data = filteredData)
            }
        } else {
            NoDeviceSelectedView()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataHistoryDeviceSelectorCard(
    rooms: List<Room>,
    selectedDevice: String?,
    onDeviceSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = true },
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Select Device", color = TextGrey, fontSize = 12.sp)
                Text(
                    text = selectedDevice?.let { id ->
                        rooms.find { it.id == id }?.name ?: id
                    } ?: "Choose a device",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(Icons.Default.ArrowDropDown, null, tint = ElectricBlue)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            rooms.forEach { room ->
                DropdownMenuItem(
                    text = { Text(room.name) },
                    onClick = {
                        onDeviceSelected(room.id)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Sensors, null)
                    }
                )
            }
        }
    }
}

@Composable
fun FilterButtonRow(
    selectedFilter: DataHistoryFilter,
    onFilterSelected: (DataHistoryFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DataHistoryFilter.values().forEach { filter ->
            FilterButton(
                label = filter.label,
                isSelected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FilterButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) ElectricBlue else BlueGrey.copy(alpha = 0.3f),
            contentColor = if (isSelected) TextWhite else TextGrey
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun DataTable(data: List<HistoryLog>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BlueGrey.copy(alpha = 0.3f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TableHeaderCell("Date & Time", Modifier.weight(2f))
                TableHeaderCell("Temp (°C)", Modifier.weight(1f))
                TableHeaderCell("Humid (%)", Modifier.weight(1f))
                TableHeaderCell("Light (lux)", Modifier.weight(1f))
            }

            Divider(color = DividerColor, thickness = 1.dp)

            // Table Rows
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                itemsIndexed(data) { index, log ->
                    DataRow(log = log, index = index)
                    if (index < data.size - 1) {
                        Divider(color = DividerColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = ElectricBlue,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
fun DataRow(log: HistoryLog, index: Int) {
    val backgroundColor = if (index % 2 == 0) {
        Color.Transparent
    } else {
        BlueGrey.copy(alpha = 0.1f)
    }

    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateFormat.format(Date(log.timestamp)),
            color = TextWhite,
            fontSize = 11.sp,
            modifier = Modifier.weight(2f)
        )
        Text(
            text = String.format("%.1f", log.temp),
            color = TemperatureColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = String.format("%.1f", log.humid),
            color = HumidityColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${log.lux}",
            color = LightColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun LoadingView() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⏳",
                    fontSize = 48.sp,
                    color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading data...",
                    color = TextGrey,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun EmptyDataView() {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                imageVector = Icons.Default.TableChart,
                contentDescription = null,
                tint = BlueGrey,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Data Available",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "No records for selected period",
                color = TextGrey,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun NoDeviceSelectedView() {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                tint = ElectricBlue,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Select a Device",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose device to view history",
                color = TextGrey,
                fontSize = 14.sp
            )
        }
    }
}
