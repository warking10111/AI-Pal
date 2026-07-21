package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AIPersona
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentsScreen(viewModel: MainViewModel) {
    var showBuilderForm by remember { mutableStateOf(false) }
    
    // Custom Builder fields
    var agentName by remember { mutableStateOf("") }
    var agentDesc by remember { mutableStateOf("") }
    var agentPrompt by remember { mutableStateOf("") }
    var agentAvatar by remember { mutableStateOf("🤖") }
    var agentTemp by remember { mutableStateOf(0.7f) }

    val allPersonas by viewModel.personas.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Specialized AI Agents", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showBuilderForm = !showBuilderForm }) {
                        Icon(
                            imageVector = if (showBuilderForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Add custom agent"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Inline GPT-style Custom Builder form
                if (showBuilderForm) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    RoundedCornerShape(16.dp)
                                ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Build Your Custom AI PAL", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                
                                OutlinedTextField(
                                    value = agentName,
                                    onValueChange = { agentName = it },
                                    label = { Text("Agent Name (e.g. Finance Coach)") },
                                    modifier = Modifier.fillMaxWidth().testTag("agent_name_input"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = agentDesc,
                                    onValueChange = { agentDesc = it },
                                    label = { Text("Short Description (e.g. Smart investment assistant)") },
                                    modifier = Modifier.fillMaxWidth().testTag("agent_desc_input"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = agentPrompt,
                                    onValueChange = { agentPrompt = it },
                                    label = { Text("System Instructions (Be precise)") },
                                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("agent_prompt_input"),
                                    maxLines = 4
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Avatar Emoji:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    val emojis = listOf("🤖", "💻", "📚", "🍳", "💡", "🎨", "📈", "🎵")
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        emojis.forEach { emoji ->
                                            Card(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clickable { agentAvatar = emoji },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (agentAvatar == emoji) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
                                                ),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Text(emoji, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Creativity (Temperature):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(String.format("%.1f", agentTemp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = agentTemp,
                                        onValueChange = { agentTemp = it },
                                        valueRange = 0.1f..1.2f,
                                        steps = 10
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (agentName.trim().isNotEmpty() && agentPrompt.trim().isNotEmpty()) {
                                            viewModel.createCustomAgent(
                                                name = agentName,
                                                avatar = agentAvatar,
                                                prompt = agentPrompt,
                                                desc = agentDesc,
                                                temp = agentTemp
                                            )
                                            // Reset fields
                                            agentName = ""
                                            agentDesc = ""
                                            agentPrompt = ""
                                            agentAvatar = "🤖"
                                            agentTemp = 0.7f
                                            showBuilderForm = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("save_agent_button")
                                ) {
                                    Text("Save and Build Agent")
                                }
                            }
                        }
                    }
                }

                // Grid/List of active agents
                items(allPersonas) { persona ->
                    AgentItemCard(persona = persona) {
                        // Start conversation with active agent
                        viewModel.startNewChat(
                            modelId = "AI PAL Pro",
                            personaId = persona.id,
                            initialTitle = persona.name
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgentItemCard(persona: AIPersona, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(persona.avatar, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = persona.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (persona.isCustom) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "Custom",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = persona.description.ifEmpty { "Personalized system agent configuration" },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
