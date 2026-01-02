package com.example.smarthome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
 * Sort column enum for data table
 */
enum class SortColumn {
    DATETIME, TEMPERATURE, HUMIDITY, LIGHT
}

/**
 * Sort direction enum
 */
enum class SortDirection {
    ASCENDING,  // Tăng dần (mũi tên xuống)
    DESCENDING  // Giảm dần (mũi tên lên)
}

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

    // Debug logging
    LaunchedEffect(historyData.size, selectedDevice) {
        android.util.Log.d("DataHistoryScreen", "📊 History data size: ${historyData.size}, Selected device: $selectedDevice")
    }

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
                    // Fetch ALL_TIME data, then filter in UI
                    viewModel.fetchHistory(deviceId, com.example.smarthome.model.TimeRange.ALL_TIME)
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
        modifier = modifier.height(42.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) ElectricBlue else BlueGrey.copy(alpha = 0.3f),
            contentColor = if (isSelected) TextWhite else TextGrey
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
fun DataTable(data: List<HistoryLog>) {
    var sortColumn by remember { mutableStateOf<SortColumn?>(null) }
    var sortDirection by remember { mutableStateOf(SortDirection.DESCENDING) }

    // Pagination state
    var currentPage by remember { mutableStateOf(0) }
    val itemsPerPage = 10

    // Sắp xếp dữ liệu dựa trên cột và hướng được chọn
    val sortedData = remember(data, sortColumn, sortDirection) {
        if (sortColumn == null) {
            data
        } else {
            val sorted = when (sortColumn) {
                SortColumn.DATETIME -> data.sortedBy { it.timestamp }
                SortColumn.TEMPERATURE -> data.sortedBy { it.temp }
                SortColumn.HUMIDITY -> data.sortedBy { it.humid }
                SortColumn.LIGHT -> data.sortedBy { it.lux }
                else -> data
            }
            if (sortDirection == SortDirection.DESCENDING) {
                sorted.reversed()
            } else {
                sorted
            }
        }
    }

    // Calculate pagination
    val totalPages = (sortedData.size + itemsPerPage - 1) / itemsPerPage
    val startIndex = currentPage * itemsPerPage
    val endIndex = minOf(startIndex + itemsPerPage, sortedData.size)
    val currentPageData = sortedData.subList(startIndex, endIndex)

    // Reset page when data changes
    LaunchedEffect(sortedData.size) {
        if (currentPage >= totalPages && totalPages > 0) {
            currentPage = totalPages - 1
        }
    }

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
                TableHeaderCell(
                    text = "Date & Time",
                    modifier = Modifier.weight(2f),
                    sortColumn = SortColumn.DATETIME,
                    currentSortColumn = sortColumn,
                    sortDirection = sortDirection,
                    onSort = { column ->
                        if (sortColumn == column) {
                            sortDirection = if (sortDirection == SortDirection.ASCENDING) {
                                SortDirection.DESCENDING
                            } else {
                                SortDirection.ASCENDING
                            }
                        } else {
                            sortColumn = column
                            sortDirection = SortDirection.DESCENDING
                        }
                    }
                )
                TableHeaderCell(
                    text = "Temp (°C)",
                    modifier = Modifier.weight(1f),
                    sortColumn = SortColumn.TEMPERATURE,
                    currentSortColumn = sortColumn,
                    sortDirection = sortDirection,
                    onSort = { column ->
                        if (sortColumn == column) {
                            sortDirection = if (sortDirection == SortDirection.ASCENDING) {
                                SortDirection.DESCENDING
                            } else {
                                SortDirection.ASCENDING
                            }
                        } else {
                            sortColumn = column
                            sortDirection = SortDirection.DESCENDING
                        }
                    }
                )
                TableHeaderCell(
                    text = "Humid (%)",
                    modifier = Modifier.weight(1f),
                    sortColumn = SortColumn.HUMIDITY,
                    currentSortColumn = sortColumn,
                    sortDirection = sortDirection,
                    onSort = { column ->
                        if (sortColumn == column) {
                            sortDirection = if (sortDirection == SortDirection.ASCENDING) {
                                SortDirection.DESCENDING
                            } else {
                                SortDirection.ASCENDING
                            }
                        } else {
                            sortColumn = column
                            sortDirection = SortDirection.DESCENDING
                        }
                    }
                )
                TableHeaderCell(
                    text = "Light (lux)",
                    modifier = Modifier.weight(1f),
                    sortColumn = SortColumn.LIGHT,
                    currentSortColumn = sortColumn,
                    sortDirection = sortDirection,
                    onSort = { column ->
                        if (sortColumn == column) {
                            sortDirection = if (sortDirection == SortDirection.ASCENDING) {
                                SortDirection.DESCENDING
                            } else {
                                SortDirection.ASCENDING
                            }
                        } else {
                            sortColumn = column
                            sortDirection = SortDirection.DESCENDING
                        }
                    }
                )
            }

            Divider(color = DividerColor, thickness = 1.dp)

            // Table Rows
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                itemsIndexed(currentPageData) { index, log ->
                    DataRow(log = log, index = startIndex + index)
                    if (index < currentPageData.size - 1) {
                        Divider(color = DividerColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }

            // Pagination Controls
            if (totalPages > 1) {
                Divider(color = DividerColor, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    IconButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous",
                            tint = if (currentPage > 0) ElectricBlue else TextGrey
                        )
                    }

                    // Page Info
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page ${currentPage + 1} of $totalPages",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "(${startIndex + 1}-$endIndex of ${sortedData.size})",
                            color = TextGrey,
                            fontSize = 11.sp
                        )
                    }

                    // Next Button
                    IconButton(
                        onClick = { if (currentPage < totalPages - 1) currentPage++ },
                        enabled = currentPage < totalPages - 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            tint = if (currentPage < totalPages - 1) ElectricBlue else TextGrey
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TableHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    sortColumn: SortColumn,
    currentSortColumn: SortColumn?,
    sortDirection: SortDirection,
    onSort: (SortColumn) -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onSort(sortColumn) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = ElectricBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Hiển thị icon mũi tên nếu cột này đang được sắp xếp
        if (currentSortColumn == sortColumn) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (sortDirection == SortDirection.ASCENDING) {
                    Icons.Default.ArrowDownward  // Mũi tên xuống = tăng dần
                } else {
                    Icons.Default.ArrowUpward    // Mũi tên lên = giảm dần
                },
                contentDescription = if (sortDirection == SortDirection.ASCENDING) "Ascending" else "Descending",
                tint = ElectricBlue,
                modifier = Modifier.size(14.dp)
            )
        }
    }
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
