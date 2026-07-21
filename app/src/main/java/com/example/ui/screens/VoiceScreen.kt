package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun VoiceScreen(viewModel: MainViewModel) {
    var isListeningState by remember { mutableStateOf(true) }
    var userVoiceTranscript by remember { mutableStateOf("") }
    var aiVoiceTranscript by remember { mutableStateOf("") }

    val isSpeaking = viewModel.isSpeaking
    val currentMessages = viewModel.currentMessages

    // Listen state transitions
    LaunchedEffect(isListeningState) {
        if (isListeningState) {
            aiVoiceTranscript = ""
            userVoiceTranscript = "Listening to you..."
            delay(1800)
            userVoiceTranscript = "What is the best way to learn Jetpack Compose?"
            delay(800)
            isListeningState = false

            // Automatically triggers a real message sending to the AI PAL Pro model!
            viewModel.sendUserMessage(userVoiceTranscript)
        }
    }

    // Capture latest AI response to render as live transcript
    LaunchedEffect(currentMessages.size) {
        val lastMsg = currentMessages.lastOrNull()
        if (lastMsg != null && lastMsg.role == "model") {
            aiVoiceTranscript = lastMsg.text
        }
    }

    // Ripple Pulsing Animation
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Top exit bar
        IconButton(
            onClick = {
                viewModel.stopSpeaking()
                viewModel.currentScreen = "chat"
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close Voice Screen", tint = MaterialTheme.colorScheme.onSurface)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            if (viewModel.isDemoMode) {
                com.example.ui.components.DemoModeBanner(isDemoMode = true, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Subtitle state info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AI PAL VOICE MODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isListeningState) "Listening..." else if (isSpeaking) "Speaking response..." else "Tap Mic to Talk",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Central Pulsating Mic Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                // Wave ripples background circles
                if (isListeningState || isSpeaking) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Inner circle microphone button
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .clickable {
                            if (isSpeaking) {
                                viewModel.stopSpeaking()
                            } else {
                                isListeningState = !isListeningState
                            }
                        }
                        .testTag("voice_mic_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListeningState) Icons.Default.Mic else if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.MicOff,
                        contentDescription = "Microphone Status",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Bottom transcripts panel
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 220.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "YOU SAID:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = userVoiceTranscript.ifEmpty { "..." },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                    Text(
                        text = "AI PAL TRANSCRIPT:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    Text(
                        text = aiVoiceTranscript.ifEmpty { "..." },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 4
                    )
                }
            }
        }
    }
}
