package com.example.smarthome

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthome.ui.theme.*
import com.example.smarthome.utils.AppConfig
import com.google.firebase.auth.FirebaseAuth

/**
 * LoginActivity - Smart Home V3.3
 * Separate Login and Register buttons
 */
class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Check if already logged in
        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        setContent {
            SmartHomeTheme {
                LoginScreen(
                    auth = auth,
                    onLoginSuccess = { navigateToMain() },
                    onShowMessage = { message ->
                        runOnUiThread {
                            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    onLoginSuccess: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    // Pre-fill with demo credentials for convenience
    var email by remember { mutableStateOf(AppConfig.DEMO_EMAIL) }
    var password by remember { mutableStateOf(AppConfig.DEMO_PASSWORD) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkBlue),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Logo
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Logo",
                    tint = ElectricBlue,
                    modifier = Modifier.size(80.dp)
                )

                Text(
                    text = "Smart Home",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Text(
                    text = "Control your devices anywhere",
                    fontSize = 14.sp,
                    color = TextGrey
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = TextGrey) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, null, tint = ElectricBlue)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BlueGrey,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = ElectricBlue
                    )
                )

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = TextGrey) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, null, tint = ElectricBlue)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = BlueGrey
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BlueGrey,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = ElectricBlue
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // LOGIN BUTTON (Sign In ONLY)
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            onShowMessage("Please enter email and password")
                            return@Button
                        }

                        isLoading = true
                        try {
                            // ONLY sign in - do NOT create account
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        onShowMessage("Login successful!")
                                        onLoginSuccess()
                                    } else {
                                        // Show error - do NOT auto-register
                                        val errorMsg = task.exception?.message ?: "Login failed"
                                        onShowMessage("Login failed: $errorMsg")
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    isLoading = false
                                    onShowMessage("Login error: ${exception.message ?: "Unknown error"}")
                                }
                        } catch (e: Exception) {
                            isLoading = false
                            onShowMessage("Firebase blocked: ${e.message ?: "Please try again later"}")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        Text(
                            text = "⏳",
                            color = TextWhite,
                            fontSize = 24.sp
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Login, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LOGIN",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f), color = DividerColor)
                    Text("OR", color = TextGrey, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    Divider(modifier = Modifier.weight(1f), color = DividerColor)
                }

                // REGISTER BUTTON (Create Account)
                OutlinedButton(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            onShowMessage("Please enter email and password")
                            return@OutlinedButton
                        }

                        isLoading = true
                        try {
                            // Create new account
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        onShowMessage("Account created successfully!")
                                        onLoginSuccess()
                                    } else {
                                        val errorMsg = task.exception?.message ?: "Registration failed"
                                        onShowMessage("Registration failed: $errorMsg")
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    isLoading = false
                                    onShowMessage("Registration error: ${exception.message ?: "Unknown error"}")
                                }
                        } catch (e: Exception) {
                            isLoading = false
                            onShowMessage("Firebase blocked: ${e.message ?: "Please try again later"}")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ElectricBlue
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading,
                    border = androidx.compose.foundation.BorderStroke(2.dp, ElectricBlue)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CREATE ACCOUNT",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Demo credentials hint
                Text(
                    text = "Demo: ${AppConfig.DEMO_EMAIL}",
                    fontSize = 11.sp,
                    color = TextGrey.copy(alpha = 0.6f)
                )

                Text(
                    text = "Version ${AppConfig.APP_VERSION} - Asia Region",
                    fontSize = 12.sp,
                    color = TextGrey,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
