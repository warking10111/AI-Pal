package com.aipal.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipal.app.data.auth.AuthState
import com.aipal.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: MainViewModel) {
    val authState by viewModel.authService.authState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRegisterMode by remember { mutableStateOf(false) }
    var isResetMode by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var localError by remember { mutableStateOf<String?>(null) }
    var localMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Logo and Greeting
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "AI PAL Workspace",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = if (isResetMode) {
                    "Enter your email to receive a password reset code."
                } else if (isRegisterMode) {
                    "Create your account to unlock cross-device synchronization, premium voices, and priority models."
                } else {
                    "Welcome back! Sign in or use Guest Mode to explore the workspace."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Alert Cards for success/errors
            if (localError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                        Text(localError!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (localMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
                        Text(localMessage!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (authState is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp))
            } else {
                // Interactive Forms
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Display Name (Registration Only)
                    if (isRegisterMode && !isResetMode) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User Icon") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("auth_name_input")
                        )
                    }

                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                    )

                    // Password Input (Not for Password Reset)
                    if (!isResetMode) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock Icon") },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                        )
                    }
                }

                // Action Buttons
                Button(
                    onClick = {
                        localError = null
                        localMessage = null
                        if (isResetMode) {
                            if (email.trim().isEmpty()) {
                                localError = "Please enter your email address."
                                return@Button
                            }
                            coroutineScope.launch {
                                val res = viewModel.authService.sendPasswordResetEmail(email)
                                if (res.isSuccess) {
                                    localMessage = "Password reset instructions sent to $email."
                                    isResetMode = false
                                } else {
                                    localError = res.exceptionOrNull()?.message ?: "Reset failed."
                                }
                            }
                        } else if (isRegisterMode) {
                            if (email.trim().isEmpty() || password.trim().isEmpty() || displayName.trim().isEmpty()) {
                                localError = "All fields are required to register."
                                return@Button
                            }
                            coroutineScope.launch {
                                val res = viewModel.authService.signUpWithEmail(email, password, displayName)
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Welcome, ${res.getOrNull()?.displayName}!", Toast.LENGTH_SHORT).show()
                                } else {
                                    localError = res.exceptionOrNull()?.message ?: "Registration failed."
                                }
                            }
                        } else {
                            if (email.trim().isEmpty() || password.trim().isEmpty()) {
                                localError = "Please enter your email and password."
                                return@Button
                            }
                            coroutineScope.launch {
                                val res = viewModel.authService.signInWithEmail(email, password)
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Logged in as ${res.getOrNull()?.displayName}", Toast.LENGTH_SHORT).show()
                                } else {
                                    localError = res.exceptionOrNull()?.message ?: "Sign-in failed."
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_primary_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isResetMode) "Send Reset Email" else if (isRegisterMode) "Create Account" else "Sign In",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Divider or labels
                if (!isResetMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Text(
                            text = "OR",
                            modifier = Modifier.padding(horizontal = 12.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    }

                    // Google Sign-In Styled Button
                    Card(
                        onClick = {
                            localError = null
                            localMessage = null
                            coroutineScope.launch {
                                // Google Sign In OAuth flow simulation using the defined parameters
                                val res = viewModel.authService.signInWithGoogle(
                                    idToken = "google-oauth-mock-id-token",
                                    email = if (email.contains("@")) email else "hein.google@gmail.com",
                                    displayName = if (displayName.isNotEmpty()) displayName else "Hein Google"
                                )
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Signed in via Google successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    localError = "Google Sign-In failed."
                                }
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_google_button"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub, // Beautiful connection/hub symbol used represent Google accounts
                                contentDescription = "Google Logo symbol",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Continue with Google", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Guest Mode Action Button
                    OutlinedButton(
                        onClick = {
                            localError = null
                            localMessage = null
                            coroutineScope.launch {
                                val res = viewModel.authService.signInAsGuest()
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Continuing as Guest User", Toast.LENGTH_SHORT).show()
                                } else {
                                    localError = "Failed to launch Guest Mode."
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_guest_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = "Guest icon")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue as Guest", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Mode Toggles and resets
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isResetMode) {
                        Text(
                            text = "Back to Sign In",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                isResetMode = false
                                localError = null
                                localMessage = null
                            }
                        )
                    } else {
                        Text(
                            text = if (isRegisterMode) "Already have an account? Sign In" else "New here? Register Account",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                isRegisterMode = !isRegisterMode
                                localError = null
                                localMessage = null
                            }
                        )

                        Text(
                            text = "Forgot Password?",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable {
                                isResetMode = true
                                localError = null
                                localMessage = null
                            }
                        )
                    }
                }
            }
        }
    }
}
