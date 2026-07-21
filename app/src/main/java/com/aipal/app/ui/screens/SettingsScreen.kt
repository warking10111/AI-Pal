package com.aipal.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipal.app.data.auth.AuthState
import com.aipal.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentTheme by viewModel.themeState.collectAsState()
    val isStreaming by viewModel.streamingEnabled.collectAsState()
    val voiceName by viewModel.voiceNameState.collectAsState()
    val voiceSpeed by viewModel.voiceSpeedState.collectAsState()
    val voiceLanguage by viewModel.voiceLanguageState.collectAsState()

    val activeProvider by viewModel.activeProviderState.collectAsState()
    val openaiKey by viewModel.openaiKeyState.collectAsState()
    val claudeKey by viewModel.claudeKeyState.collectAsState()
    val deepseekKey by viewModel.deepseekKeyState.collectAsState()
    val grokKey by viewModel.grokKeyState.collectAsState()
    val ollamaUrl by viewModel.ollamaUrlState.collectAsState()

    var showLanguageMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings & Voice", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Section: User Profile & Authentication
            val authState by viewModel.authService.authState.collectAsState()
            if (authState is AuthState.Authenticated) {
                val user = (authState as AuthState.Authenticated).user
                var showUpgradeDialog by remember { mutableStateOf(false) }

                Text("Your User Profile", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.displayName.take(1).uppercase(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = user.email,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val statusLabel = if (user.isGuest) "Guest Mode" else if (user.isEmailVerified) "Email Verified" else "Email Unverified"
                                    val statusColor = if (user.isGuest) Color(0xFFE0A900) else if (user.isEmailVerified) Color(0xFF00C853) else Color(0xFFFF5252)
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(statusLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("${user.credits} Credits", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Sync
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val res = viewModel.authService.synchronizeProfile()
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Profile synchronized successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Sync failed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("profile_sync_button"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Verify (if real user and unverified)
                            if (!user.isGuest && !user.isEmailVerified) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val res = viewModel.authService.sendEmailVerification()
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Email verification simulated successfully!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Verification failed.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1.2f).testTag("profile_verify_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = "Verify", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verify Email", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Logout
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.logout()
                                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("profile_logout_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = "Logout", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Logout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (user.isGuest) {
                            // Beautiful CTA for Guest Upgrade
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .clickable { showUpgradeDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Upgrade to Full Account", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("Link your guest account to preserve & sync conversations, custom personas, and memories!", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Upgrade", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else {
                            // Account deletion control
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        val res = viewModel.authService.deleteAccount()
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Account & local data deleted", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Account deletion failed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.align(Alignment.CenterHorizontally).testTag("profile_delete_button")
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Account", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete My Account & Personal Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Guest Account Upgrade Dialog
                if (showUpgradeDialog) {
                    var upEmail by remember { mutableStateOf("") }
                    var upPassword by remember { mutableStateOf("") }
                    var upName by remember { mutableStateOf("") }
                    var upError by remember { mutableStateOf<String?>(null) }

                    AlertDialog(
                        onDismissRequest = { showUpgradeDialog = false },
                        title = { Text("Upgrade Guest Account") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Your current guest chat history, agents, and memories will be perfectly preserved and merged into your new account.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                if (upError != null) {
                                    Text(upError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedTextField(
                                    value = upName,
                                    onValueChange = { upName = it },
                                    placeholder = { Text("Display Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("upgrade_name_input")
                                )

                                OutlinedTextField(
                                    value = upEmail,
                                    onValueChange = { upEmail = it },
                                    placeholder = { Text("Email Address") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("upgrade_email_input")
                                )

                                OutlinedTextField(
                                    value = upPassword,
                                    onValueChange = { upPassword = it },
                                    placeholder = { Text("Password") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().testTag("upgrade_password_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    Text("OR", modifier = Modifier.padding(horizontal = 8.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val res = viewModel.authService.upgradeGuestAccountWithGoogle(
                                                idToken = "google-upgrade-token",
                                                email = if (upEmail.contains("@")) upEmail else "guest.upgrade@gmail.com",
                                                displayName = if (upName.isNotEmpty()) upName else "Upgraded Guest"
                                            )
                                            if (res.isSuccess) {
                                                showUpgradeDialog = false
                                                Toast.makeText(context, "Successfully upgraded to Google Account!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                upError = "Google upgrade failed."
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("upgrade_google_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Hub, contentDescription = "Google", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upgrade with Google", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (upEmail.trim().isEmpty() || upPassword.trim().isEmpty() || upName.trim().isEmpty()) {
                                        upError = "All fields are required to upgrade."
                                        return@Button
                                    }
                                    scope.launch {
                                        val res = viewModel.authService.upgradeGuestAccount(upEmail, upPassword, upName)
                                        if (res.isSuccess) {
                                            showUpgradeDialog = false
                                            Toast.makeText(context, "Account upgraded to ${res.getOrNull()?.displayName}!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            upError = res.exceptionOrNull()?.message ?: "Upgrade failed."
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("upgrade_confirm_button")
                            ) {
                                Text("Upgrade Account")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUpgradeDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            // Section 1: Themes
            Text("Aesthetic Style & Theme", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active UI Theme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Icon(Icons.Default.Palette, contentDescription = "Theme", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Light", "Dark", "AMOLED").forEach { themeName ->
                            val isSelected = currentTheme == themeName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                    .clickable { viewModel.updateTheme(themeName) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = themeName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Text-To-Speech voice settings
            Text("TTS Audio & Playback", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Voice selection
                    Column {
                        Text("Interactive Voice Tone", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("Kore", "Leda", "Puck", "Fenrir").forEach { voice ->
                                val isSelected = voiceName == voice
                                val toneDesc = when (voice) {
                                    "Kore" -> "Natural"
                                    "Leda" -> "Female"
                                    "Puck" -> "Playful"
                                    else -> "Deep"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.updateVoice(voice) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(voice, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(toneDesc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    // Voice speed slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Speech Speed Rate", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(String.format("%.1fx", voiceSpeed), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = voiceSpeed,
                            onValueChange = { viewModel.updateVoiceSpeed(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 15
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    // Language Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Voice Speech Language", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Box {
                            Button(
                                onClick = { showLanguageMenu = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(voiceLanguage, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(
                                expanded = showLanguageMenu,
                                onDismissRequest = { showLanguageMenu = false }
                            ) {
                                val languages = listOf("English", "Afrikaans", "Spanish", "French", "German", "Japanese", "Chinese", "Arabic", "Hindi")
                                languages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang) },
                                        onClick = {
                                            viewModel.updateVoiceLanguage(lang)
                                            showLanguageMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: AI Preferences
            Text("General AI Preferences", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Streaming AI Responses", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Simulates streaming tokens incrementally", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isStreaming,
                        onCheckedChange = { viewModel.updateStreaming(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Section: Multi-Provider AI Routing
            Text("AI Orchestrator & Multi-Provider", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Select Active Provider", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    val providersList = listOf(
                        "gemini" to "Google Gemini",
                        "openai" to "OpenAI GPT-4o",
                        "claude" to "Claude 3.5",
                        "deepseek" to "DeepSeek V3/R1",
                        "grok" to "xAI Grok",
                        "ollama" to "Ollama Local"
                    )

                    // Providers Grid Layout (2 items per row)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        providersList.chunked(2).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunk.forEach { (id, label) ->
                                    val isSelected = activeProvider == id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                            .clickable { viewModel.updateActiveProvider(id) }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    // API Key Settings Inputs based on active selection
                    when (activeProvider) {
                        "gemini" -> {
                            Text(
                                text = "Using Google Gemini. Live requests are routed via your AI Studio Secrets. Offline demo mode is used automatically when no key is present.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        "openai" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("OpenAI API Key", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = openaiKey,
                                    onValueChange = { viewModel.updateOpenAiKey(it) },
                                    placeholder = { Text("sk-proj-...", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("openai_key_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                Text("Keys are securely saved offline in Android SharedPreferences.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        "claude" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Anthropic Claude API Key", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = claudeKey,
                                    onValueChange = { viewModel.updateClaudeKey(it) },
                                    placeholder = { Text("sk-ant-...", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("claude_key_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                Text("Keys are securely saved offline in Android SharedPreferences.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        "deepseek" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("DeepSeek API Key", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = deepseekKey,
                                    onValueChange = { viewModel.updateDeepseekKey(it) },
                                    placeholder = { Text("sk-...", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("deepseek_key_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                Text("Keys are securely saved offline in Android SharedPreferences.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        "grok" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("xAI Grok API Key", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = grokKey,
                                    onValueChange = { viewModel.updateGrokKey(it) },
                                    placeholder = { Text("xai-...", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("grok_key_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                Text("Keys are securely saved offline in Android SharedPreferences.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        "ollama" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Ollama Local Base URL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = ollamaUrl,
                                    onValueChange = { viewModel.updateOllamaUrl(it) },
                                    placeholder = { Text("http://10.0.2.2:11434", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("ollama_url_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                Text("Ollama must be running on your host machine. Default is http://10.0.2.2:11434 on emulator.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Section 4: Diagnostics/Factory reset
            Text("System Diagnostics & Reset", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Warning: Resetting will clear all cached conversations, custom personas, and recorded memories in the Room SQLite database permanently.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.database.clearAllTables()
                                viewModel.currentScreen = "home"
                                viewModel.activeConversationId = null
                                viewModel.currentMessages = emptyList()
                                Toast.makeText(context, "Room SQLite database reset successfully.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("factory_reset_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Reset")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Factory Reset SQLite Tables", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
