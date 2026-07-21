package com.aipal.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipal.app.viewmodel.MainViewModel
import java.io.ByteArrayOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    var promptInput by remember { mutableStateOf("") }
    var showModelSelector by remember { mutableStateOf(false) }
    var selectedModelLocal by remember { mutableStateOf("AI PAL Lite") }
    
    val context = LocalContext.current
    val credits by viewModel.creditsState.collectAsState()
    val plan by viewModel.subscriptionState.collectAsState()

    // Activity launcher for choosing an image for vision analysis
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val bytes = outputStream.toByteArray()
                viewModel.attachedImageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                viewModel.attachedImageMime = "image/jpeg"
                viewModel.attachedFileName = "image.jpg"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Quick Action configuration
    val quickActions = listOf(
        QuickActionItem("Generate Image", Icons.Default.Image, "image_gen"),
        QuickActionItem("Analyze Image", Icons.Default.PhotoCamera, "analyze_image"),
        QuickActionItem("Summarize PDF", Icons.Default.PictureAsPdf, "pdf"),
        QuickActionItem("Code Solution", Icons.Default.Code, "code_model"),
        QuickActionItem("Research Search", Icons.Default.TravelExplore, "research_model"),
        QuickActionItem("Study Guide", Icons.Default.AutoStories, "study"),
        QuickActionItem("Wanderlust Planner", Icons.Default.FlightTakeoff, "travel"),
        QuickActionItem("Fitness Coach", Icons.Default.DirectionsRun, "fitness")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Premium Billing Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Subscription plan",
                            tint = if (plan == "Premium") Color(0xFFFFB703) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (plan == "Premium") "Premium Active" else "Free Plan ($credits left)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Interactive Model Selector Trigger
                Box {
                    Button(
                        onClick = { showModelSelector = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(selectedModelLocal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", modifier = Modifier.size(16.dp))
                    }

                    DropdownMenu(
                        expanded = showModelSelector,
                        onDismissRequest = { showModelSelector = false }
                    ) {
                        val models = listOf("AI PAL Lite", "AI PAL Pro", "AI PAL Fast", "AI PAL Reasoning", "AI PAL Creative", "AI PAL Coding", "AI PAL Vision", "AI PAL Research")
                        models.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    selectedModelLocal = model
                                    showModelSelector = false
                                }
                            )
                        }
                    }
                }
            }

            if (viewModel.isDemoMode) {
                Spacer(modifier = Modifier.height(12.dp))
                com.aipal.app.ui.components.DemoModeBanner(isDemoMode = true)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Greeting layout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Hello, ",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Hein",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.5).sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "What can I help you with today?",
                fontSize = 18.sp,
                color = Color(0xFF94A3B8), // slate-400
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Premium Prompt Input Box Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(32.dp)
                    ),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    TextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = {
                            Text(
                                "Ask me anything...",
                                color = Color(0xFF64748B) // Slate placeholder
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp, max = 180.dp)
                            .testTag("home_prompt_box"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 6
                    )

                    // Image attachment indicator
                    if (viewModel.attachedImageBase64 != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attached", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("image.jpg attached", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            viewModel.attachedImageBase64 = null
                                            viewModel.attachedImageMime = null
                                            viewModel.attachedFileName = null
                                        },
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Action controllers (Attach, Voice, Send)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Camera/Image picker button
                            IconButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("home_attach_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Attach photo",
                                    tint = Color(0xFFCBD5E1) // slate-300
                                )
                            }

                            // Microphone voice chat button
                            IconButton(
                                onClick = {
                                    viewModel.startNewChat(selectedModelLocal)
                                    viewModel.currentScreen = "voice"
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("home_voice_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice mode",
                                    tint = Color(0xFFCBD5E1) // slate-300
                                )
                            }
                        }

                        // Send message button
                        IconButton(
                            onClick = {
                                if (promptInput.trim().isNotEmpty() || viewModel.attachedImageBase64 != null) {
                                    val sendPrompt = promptInput
                                    promptInput = ""
                                    viewModel.startNewChat(selectedModelLocal)
                                    viewModel.sendUserMessage(sendPrompt)
                                }
                            },
                            modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                .testTag("home_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Quick actions grid title
            Text(
                text = "Discover AI Capabilities",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Grid Layout for Actions
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(quickActions) { action ->
                    QuickActionCard(action) {
                        when (action.screenKey) {
                            "image_gen" -> viewModel.currentScreen = "image_gen"
                            "pdf" -> viewModel.currentScreen = "pdf"
                            "analyze_image" -> imagePickerLauncher.launch("image/*")
                            "code_model" -> viewModel.startNewChat("AI PAL Coding", null, "Coding Assistant")
                            "research_model" -> viewModel.startNewChat("AI PAL Research", null, "Research Assistant")
                            "study" -> viewModel.startNewChat("AI PAL Pro", "agent_teacher", "Study Guide")
                            "travel" -> viewModel.startNewChat("AI PAL Pro", "agent_travel", "Wanderlust")
                            "fitness" -> viewModel.startNewChat("AI PAL Lite", "agent_fitness", "Fitness Routine")
                        }
                    }
                }
            }
        }
    }
}

data class QuickActionItem(
    val title: String,
    val icon: ImageVector,
    val screenKey: String
)

fun getQuickActionColors(screenKey: String): Pair<Color, Color> {
    return when (screenKey) {
        "image_gen" -> Pair(Color(0xFF381E72), Color(0xFFD0BCFF)) // Purple combo
        "pdf" -> Pair(Color(0xFF004A77), Color(0xFFC2E7FF))       // Blue combo
        "code_model" -> Pair(Color(0xFF424900), Color(0xFFE7ED9B)) // Lime/Olive combo
        "research_model" -> Pair(Color(0xFF3E4946), Color(0xFFBCEBE2)) // Teal/Gray combo
        "analyze_image" -> Pair(Color(0xFF4A1E2B), Color(0xFFFFD9DF)) // Rose combo
        "study" -> Pair(Color(0xFF3E2D00), Color(0xFFFFE082))      // Gold/Amber combo
        "travel" -> Pair(Color(0xFF0A3C36), Color(0xFFA7F3D0))     // Mint combo
        "fitness" -> Pair(Color(0xFF1E3A1E), Color(0xFFC8E6C9))    // Green combo
        else -> Pair(Color(0xFF1E2330), Color(0xFF00E5FF))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(item: QuickActionItem, onClick: () -> Unit) {
    val (iconBg, iconTint) = getQuickActionColors(item.screenKey)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                lineHeight = 16.sp
            )
        }
    }
}
