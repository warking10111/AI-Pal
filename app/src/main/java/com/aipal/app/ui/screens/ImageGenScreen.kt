package com.aipal.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipal.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGenScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isGenerating = viewModel.isGeneratingImage
    val generatedBase64 = viewModel.generatedImageOutput
    val error = viewModel.imgError

    // List of prebuilt styles
    val stylesList = listOf("Realistic", "Anime", "3D", "Painting", "Cyberpunk", "Fantasy", "Pixel Art")
    val aspectRatios = listOf("1:1", "16:9", "9:16")
    val qualityList = listOf("512px", "1K", "2K")

    // Local state for Bitmap conversion
    val decodedBitmap = remember(generatedBase64) {
        if (!generatedBase64.isNullOrEmpty()) {
            try {
                val decodedBytes = Base64.decode(generatedBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Generation Studio", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (viewModel.isDemoMode) {
                Spacer(modifier = Modifier.height(12.dp))
                com.aipal.app.ui.components.DemoModeBanner(isDemoMode = true)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Result Visual Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("AI PAL is rendering pixels...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Using gemini-2.5-flash-image", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                } else if (decodedBitmap != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = decodedBitmap.asImageBitmap(),
                            contentDescription = "Generated result",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Floating action buttons overlay
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    Toast.makeText(context, "Saved to Pictures/AIPAL/", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(40.dp),
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(18.dp))
                            }

                            FloatingActionButton(
                                onClick = {
                                    Toast.makeText(context, "Mock sharing dialog triggered.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(40.dp),
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Image, contentDescription = "Placeholder", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Your generated masterpiece will load here.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f))) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Inputs fields
            Text("Describe your masterpiece:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = viewModel.imgPrompt,
                onValueChange = { viewModel.imgPrompt = it },
                placeholder = { Text("e.g. A hyperrealistic robot painting on a canvas...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .testTag("image_prompt_input"),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Styles selectors
            Text("Choose visual style:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stylesList) { style ->
                    val isSelected = viewModel.imgStyle == style
                    InputChip(
                        selected = isSelected,
                        onClick = { viewModel.imgStyle = style },
                        label = { Text(style) },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aspect Ratio selectors
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Aspect Ratio:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        aspectRatios.forEach { ratio ->
                            val isSelected = viewModel.imgAspectRatio == ratio
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.imgAspectRatio = ratio },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(ratio, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Render Quality:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        qualityList.forEach { qual ->
                            val isSelected = viewModel.imgQuality == qual
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .width(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.imgQuality = qual },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(qual, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action generate button
            Button(
                onClick = { viewModel.triggerImageGeneration() },
                enabled = viewModel.imgPrompt.trim().isNotEmpty() && !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("generate_image_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Generate")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Masterpiece", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
