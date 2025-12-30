package com.example.smarthome

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarthome.ui.MainScreen
import com.example.smarthome.ui.theme.SmartHomeTheme
import com.example.smarthome.viewmodel.SmartHomeViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * MainActivity - Version 3.1
 * Entry point with authentication check
 */
class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // ⚠️ DEV MODE: Check for bypass flag
        val bypassAuth = intent.getBooleanExtra("BYPASS_AUTH", false)

        // Check if user is logged in (or bypassed)
        if (auth.currentUser == null && !bypassAuth) {
            // Redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            SmartHomeTheme {
                SmartHomeApp()
            }
        }
    }
}

@Composable
fun SmartHomeApp() {
    // Initialize ViewModel
    val viewModel: SmartHomeViewModel = viewModel()

    // Auto-connect to MQTT on app startup
    LaunchedEffect(Unit) {
        viewModel.connectToMqtt()
    }

    // Main Screen with navigation
    MainScreen(viewModel = viewModel)
}
