package com.example.smarthome.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthome.ui.theme.*
import com.example.smarthome.viewmodel.SmartHomeViewModel

/**
 * MainScreen - Version 3.4
 * Main navigation with Home, Chart, Data History, and Settings tabs
 * HYBRID ARCHITECTURE: MQTT (Home) + Firebase (Chart + Data)
 */

// ==========================================
// NAVIGATION TAB ENUM
// ==========================================

enum class NavigationTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    CHART("Chart", Icons.Default.ShowChart),
    DATA("Data", Icons.Default.TableChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

// ==========================================
// MAIN SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: SmartHomeViewModel,
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.HOME) }

    // State flows - MQTT (Real-time)
    val rooms by viewModel.rooms.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val mqttConfig by viewModel.mqttConfig.collectAsState()

    // State flows - Firebase (Historical)
    val firebaseConfig by viewModel.firebaseConfig.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    val isLoadingChart by viewModel.isLoadingChart.collectAsState()

    // Common state
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Show error message if any
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Auto-dismiss after 3 seconds
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Smart Home v3.4",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (selectedTab) {
                                NavigationTab.HOME -> if (isConnected) "MQTT Connected" else "MQTT Disconnected"
                                NavigationTab.CHART -> "Firebase Chart View"
                                NavigationTab.DATA -> "Firebase Data History"
                                NavigationTab.SETTINGS -> "Configuration"
                            },
                            fontSize = 12.sp,
                            color = if (isConnected || selectedTab != NavigationTab.HOME) {
                                Color(0xFF4CAF50)
                            } else {
                                Color.Gray
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceColor,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceColor,
                tonalElevation = 8.dp,
                modifier = Modifier.shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                NavigationTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            indicatorColor = PrimaryBlue.copy(alpha = 0.1f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        },
        snackbarHost = {
            // Show error messages
            errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color(0xFFFF5252),
                    contentColor = Color.White
                ) {
                    Text(error)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                NavigationTab.HOME -> {
                    // MQTT-powered real-time control
                    HomeScreen(
                        rooms = rooms,
                        isConnected = isConnected,
                        viewModel = viewModel
                    )
                }
                NavigationTab.CHART -> {
                    // Firebase-powered historical chart
                    ChartScreen(
                        rooms = rooms,
                        chartData = chartData,
                        isLoading = isLoadingChart,
                        viewModel = viewModel
                    )
                }
                NavigationTab.DATA -> {
                    // Firebase-powered data history table
                    DataHistoryScreen(
                        rooms = rooms,
                        historyData = chartData,
                        isLoading = isLoadingChart,
                        viewModel = viewModel
                    )
                }
                NavigationTab.SETTINGS -> {
                    SettingsScreen(
                        mqttConfig = mqttConfig,
                        firebaseConfig = firebaseConfig,
                        rooms = rooms,
                        isConnected = isConnected,
                        onSaveMqttConfig = { config ->
                            viewModel.updateMqttConfig(config)
                        },
                        onSaveFirebaseConfig = { config ->
                            viewModel.updateFirebaseConfig(config)
                        },
                        onSetInterval = { deviceId, interval ->
                            viewModel.setSensorInterval(deviceId, interval)
                        },
                        onRebootDevice = { deviceId ->
                            viewModel.rebootDevice(deviceId)
                        },
                        onDisconnect = {
                            viewModel.disconnectFromMqtt()
                        },
                        onConnect = {
                            viewModel.connectToMqtt()
                        },
                        onLogout = onLogout,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
