package com.example.smarthome.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthome.model.HistoryLog
import com.example.smarthome.model.Room
import com.example.smarthome.model.TimeRange
import com.example.smarthome.ui.theme.*
import com.example.smarthome.viewmodel.SmartHomeViewModel

/**
 * ChartScreen - V3.2 FIXED
 * Proper Material3 annotations + Float handling
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkBlue)
            .padding(16.dp)
    ) {
        if (rooms.isNotEmpty()) {
            DeviceSelectorCard(
                rooms = rooms,
                selectedDevice = selectedDevice,
                onDeviceSelected = {
                    selectedDevice = it
                    viewModel.fetchHistory(it, selectedTimeRange)
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
fun DeviceSelectorCard(
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
 * FIXED: Proper Float handling with explicit conversion
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

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 40f

        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        if (values.size < 2) return@Canvas

        // Grid lines
        for (i in 0..4) {
            val y = padding + (chartHeight / 4) * i
            drawLine(
                color = DividerColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
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

            drawCircle(color = lineColor, radius = 5f, center = Offset(x, y))
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
            CircularProgressIndicator(color = ElectricBlue)
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
