package com.example.smarthome.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthome.model.HistoryLog
import com.example.smarthome.model.Room
import com.example.smarthome.model.TimeRange
import com.example.smarthome.ui.theme.*
import com.example.smarthome.viewmodel.SmartHomeViewModel

/**
 * ChartScreen - V3.3 FIXED
 * Touch interaction + Units display + Field name fix
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    rooms: List<Room>,
    chartData: List<HistoryLog>,
    isLoading: Boolean,
    viewModel: SmartHomeViewModel
) {
    var selectedDevice by remember { mutableStateOf<String?>(null) }
    var selectedTimeRange by remember { mutableStateOf(TimeRange.DAY) }
    var selectedParameter by remember { mutableStateOf("temp") }

    // Collect error state from ViewModel
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkBlue)
            .padding(16.dp)
    ) {
        // Show error banner if Firebase fails
        errorMessage?.let { error ->
            if (error.contains("Firebase") || error.contains("401") || error.contains("Load failed")) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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

        if (rooms.isNotEmpty()) {
            ChartDeviceSelectorCard(
                rooms = rooms,
                selectedDevice = selectedDevice,
                onDeviceSelected = { deviceId ->
                    selectedDevice = deviceId
                    viewModel.fetchHistory(deviceId, selectedTimeRange)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (selectedDevice != null) {
            TimeRangeSelector(
                selectedRange = selectedTimeRange,
                onRangeSelected = {
                    selectedTimeRange = it
                    selectedDevice?.let { deviceId ->
                        viewModel.fetchHistory(deviceId, it)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ParameterSelector(
                selectedParameter = selectedParameter,
                onParameterSelected = { selectedParameter = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                LoadingCard()
            } else if (chartData.isEmpty()) {
                EmptyChartCard()
            } else {
                ChartCard(data = chartData, parameter = selectedParameter)
                Spacer(modifier = Modifier.height(12.dp))
                StatsCard(data = chartData, parameter = selectedParameter)
            }
        } else {
            NoDeviceCard()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDeviceSelectorCard(
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
                Text("Device", color = TextGrey, fontSize = 12.sp)
                Text(
                    text = selectedDevice ?: "Select Device",
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
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(TimeRange.values()) { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricBlue,
                    selectedLabelColor = TextWhite,
                    containerColor = BlueGrey.copy(alpha = 0.3f),
                    labelColor = TextGrey
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParameterSelector(
    selectedParameter: String,
    onParameterSelected: (String) -> Unit
) {
    val parameters = listOf(
        "temp" to "Temperature",
        "humid" to "Humidity",
        "lux" to "Light"
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(parameters) { (key, label) ->
            FilterChip(
                selected = selectedParameter == key,
                onClick = { onParameterSelected(key) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when(key) {
                        "temp" -> TemperatureColor
                        "humid" -> HumidityColor
                        else -> LightColor
                    },
                    selectedLabelColor = TextWhite,
                    containerColor = BlueGrey.copy(alpha = 0.3f),
                    labelColor = TextGrey
                )
            )
        }
    }
}

@Composable
fun ChartCard(data: List<HistoryLog>, parameter: String) {
    Card(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            LineChart(data = data, parameter = parameter, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * FIXED v3.3: Float handling + Touch interaction + Y-axis with scale
 */
@Composable
fun LineChart(
    data: List<HistoryLog>,
    parameter: String,
    modifier: Modifier = Modifier
) {
    // Convert to List<Float> explicitly
    val values: List<Float> = data.map {
        when (parameter) {
            "temp" -> it.temp.toFloat()
            "humid" -> it.humid.toFloat()
            else -> it.lux.toFloat()
        }
    }

    // Touch interaction state
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Check for empty or invalid data
    if (values.isEmpty() || values.all { it == 0f }) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No valid data", color = TextGrey)
        }
        return
    }

    // Use maxOrNull() on List<Float>
    val maxValue = values.maxOrNull() ?: 1f
    val minValue = values.minOrNull() ?: 0f
    val range = maxValue - minValue

    val lineColor = when (parameter) {
        "temp" -> TemperatureColor
        "humid" -> HumidityColor
        else -> LightColor
    }

    val unit = when (parameter) {
        "temp" -> "°C"
        "humid" -> "%"
        else -> " lux"
    }

    Box(modifier = modifier) {
        // Y-axis labels (left side)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(50.dp)
                .align(Alignment.CenterStart),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top label (max value)
            Text(
                text = "${String.format("%.1f", maxValue)}$unit",
                color = TextGrey,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Middle labels
            for (i in 1..3) {
                val value = maxValue - (range / 4 * i)
                Text(
                    text = String.format("%.1f", value),
                    color = TextGrey.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
            }

            // Bottom label (min value)
            Text(
                text = "${String.format("%.1f", minValue)}$unit",
                color = TextGrey,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 55.dp)
                .pointerInput(values.size) {
                    detectTapGestures(
                        onPress = { offset ->
                            val padding = 40f
                            val chartWidth = size.width - padding * 2
                            val stepX = chartWidth / (values.size - 1).coerceAtLeast(1)
                            val index = ((offset.x - padding) / stepX).toInt()
                                .coerceIn(0, values.size - 1)
                            selectedIndex = index
                            tryAwaitRelease()
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val padding = 40f

            val chartWidth = width - padding * 2
            val chartHeight = height - padding * 2

            if (values.size < 2) return@Canvas

            // Grid lines with tick marks
            for (i in 0..4) {
                val y = padding + (chartHeight / 4) * i

                // Horizontal grid line
                drawLine(
                    color = DividerColor,
                    start = Offset(padding, y),
                    end = Offset(width - padding, y),
                    strokeWidth = 1f
                )

                // Left tick mark
                drawLine(
                    color = TextGrey.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(padding - 5f, y),
                    strokeWidth = 2f
                )
            }

            // Line path
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = padding + (chartWidth / (values.size - 1)) * index
                val normalizedValue = if (range > 0) (value - minValue) / range else 0.5f
                val y = padding + chartHeight - (chartHeight * normalizedValue)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(path = path, color = lineColor, style = Stroke(width = 3f))

            // Points
            values.forEachIndexed { index, value ->
                val x = padding + (chartWidth / (values.size - 1)) * index
                val normalizedValue = if (range > 0) (value - minValue) / range else 0.5f
                val y = padding + chartHeight - (chartHeight * normalizedValue)

                // Highlight selected point
                if (index == selectedIndex) {
                    drawCircle(color = Color.White, radius = 10f, center = Offset(x, y))
                    drawCircle(color = lineColor, radius = 7f, center = Offset(x, y))
                } else {
                    drawCircle(color = lineColor, radius = 5f, center = Offset(x, y))
                }
            }
        }

        // Tooltip for selected point
        selectedIndex?.let { index ->
            if (index in values.indices) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (parameter) {
                                "temp" -> "${String.format("%.1f", values[index])}°C"
                                "humid" -> "${String.format("%.1f", values[index])}%"
                                else -> "${values[index].toInt()} lux"
                            },
                            color = lineColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = java.text.SimpleDateFormat("HH:mm dd/MM", java.util.Locale.getDefault())
                                .format(java.util.Date(data[index].timestamp)),
                            color = TextGrey,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * FIXED: Proper average() on List<Double>
 */
@Composable
fun StatsCard(data: List<HistoryLog>, parameter: String) {
    // Convert to List<Double>
    val values: List<Double> = data.map {
        when (parameter) {
            "temp" -> it.temp.toDouble()
            "humid" -> it.humid.toDouble()
            else -> it.lux.toDouble()
        }
    }

    // Use average() on List<Double>
    val avg = if (values.isNotEmpty()) values.average() else 0.0
    val max = values.maxOrNull() ?: 0.0
    val min = values.minOrNull() ?: 0.0

    val unit = when (parameter) {
        "temp" -> "°C"
        "humid" -> "%"
        else -> " lux"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Average", String.format("%.1f", avg) + unit, ElectricBlue)
            StatItem("Maximum", String.format("%.1f", max) + unit, SuccessGreen)
            StatItem("Minimum", String.format("%.1f", min) + unit, WarningAmber)
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextGrey, fontSize = 12.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⏳",
                    fontSize = 48.sp,
                    color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(8.dp))
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
fun EmptyChartCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.ShowChart, null, tint = BlueGrey, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Data Available", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("No history for selected range", color = TextGrey, fontSize = 14.sp)
        }
    }
}

@Composable
fun NoDeviceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Timeline, null, tint = ElectricBlue, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Select a Device", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Choose device to view history", color = TextGrey, fontSize = 14.sp)
        }
    }
}
