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
 * MainActivity - Version 3.4
 * Entry point with authentication and logout support
 */
class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            // Redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            SmartHomeTheme {
                SmartHomeApp(
                    onLogout = {
                        handleLogout()
                    }
                )
            }
        }
    }

    private fun handleLogout() {
        // Sign out from Firebase
        auth.signOut()

        // Redirect to login
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

@Composable
fun SmartHomeApp(onLogout: () -> Unit) {
    // Initialize ViewModel
    val viewModel: SmartHomeViewModel = viewModel()

    // Auto-connect to MQTT on app startup
    LaunchedEffect(Unit) {
        viewModel.connectToMqtt()
    }

    // Main Screen with navigation and logout
    MainScreen(
        viewModel = viewModel,
        onLogout = onLogout
    )
}
