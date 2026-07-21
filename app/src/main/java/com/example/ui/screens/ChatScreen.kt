package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.ChatMessage
import com.example.data.model.WebSource
import com.example.ui.components.MarkdownText
import com.example.viewmodel.MainViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputMessageText by remember { mutableStateOf("") }
    var searchWebChecked by remember { mutableStateOf(false) }
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var showModelDropdownChat by remember { mutableStateOf(false) }

    val messages = viewModel.currentMessages
    val isGenerating = viewModel.isGenerating
    val activeConId = viewModel.activeConversationId ?: ""

    // Automatically scroll to bottom when a new message arrives or during generation
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Parse grounding references JSON
    val moshi = remember { Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build() }
    val sourcesAdapter = remember {
        moshi.adapter<List<WebSource>>(
            Types.newParameterizedType(List::class.java, WebSource::class.java)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { viewModel.currentScreen = "home" }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showModelDropdownChat = true }
                        ) {
                            Column {
                                Text(
                                    text = "AI PAL Conversation",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to switch models",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Change Model", modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = showModelDropdownChat,
                            onDismissRequest = { showModelDropdownChat = false }
                        ) {
                            val models = listOf("AI PAL Lite", "AI PAL Pro", "AI PAL Fast", "AI PAL Reasoning", "AI PAL Creative", "AI PAL Coding", "AI PAL Vision", "AI PAL Research")
                            models.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        viewModel.startNewChat(model, null, "Chat session")
                                        showModelDropdownChat = false
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.currentScreen = "voice" }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice mode", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Interactive controllers (Search Web Toggle, status info)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google Search Web Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { searchWebChecked = !searchWebChecked }
                    ) {
                        Checkbox(
                            checked = searchWebChecked,
                            onCheckedChange = { searchWebChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.scale(0.85f)
                        )
                        Icon(Icons.Default.Language, contentDescription = "Web Search", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Search Web (Perplexity Mode)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (isGenerating) {
                        Text(
                            "AI PAL is writing...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Text Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = inputMessageText,
                        onValueChange = { inputMessageText = it },
                        placeholder = { Text("Ask or instruct anything...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_prompt_box"),
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    IconButton(
                        onClick = {
                            if (inputMessageText.trim().isNotEmpty()) {
                                if (editingMessageId != null) {
                                    // Save edited prompt
                                    val editedId = editingMessageId!!
                                    scope.launch {
                                        viewModel.updateMessageText(editedId, inputMessageText)
                                        editingMessageId = null
                                        inputMessageText = ""
                                    }
                                } else {
                                    val sendText = inputMessageText
                                    inputMessageText = ""
                                    viewModel.sendUserMessage(sendText, searchWebChecked)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = if (editingMessageId != null) Icons.Default.Check else Icons.Default.ArrowUpward,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (viewModel.isDemoMode) {
                com.example.ui.components.DemoModeBanner(isDemoMode = true, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
            if (messages.isEmpty() && !isGenerating) {
                // Empty state greeting
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "No chats",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Starting a new premium conversation.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Input a question below or switch model parameters at the top.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageItem(
                            message = message,
                            sourcesAdapter = sourcesAdapter,
                            onPinToggle = { viewModel.togglePinMessage(message.id, message.isPinned) },
                            onDelete = { viewModel.deleteMessage(message.id) },
                            onEdit = {
                                editingMessageId = message.id
                                inputMessageText = message.text
                            },
                            onSpeak = { viewModel.speakText(message.text) }
                        )
                    }

                    if (isGenerating) {
                        item {
                            TypingAnimationItem()
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    sourcesAdapter: com.squareup.moshi.JsonAdapter<List<WebSource>>,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onSpeak: () -> Unit
) {
    val isUser = message.role == "user"
    val clipboardManager = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val context = LocalContext.current

    // Extract sources
    val sources = remember(message.sourcesJson) {
        if (!message.sourcesJson.isNullOrEmpty()) {
            try {
                sourcesAdapter.fromJson(message.sourcesJson)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Bubble Layout
        Card(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                }
            ),
            modifier = Modifier
                .widthIn(max = 310.dp)
                .border(
                    1.dp,
                    if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "You" else "AI PAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                    if (message.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = Color(0xFFFFB703),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Render uploaded / attached base64 image if exists
                if (message.imageUrl != null) {
                    val imgUri = message.imageUrl
                    AsyncImage(
                        model = imgUri,
                        contentDescription = "User upload",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                // Core response text (parsed as Markdown!)
                MarkdownText(
                    text = message.text,
                    textColor = MaterialTheme.colorScheme.onSurface
                )

                // Render search grounding references like Perplexity!
                if (!sources.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sources & Grounding References:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sources) { source ->
                            Card(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clickable {
                                        Toast.makeText(context, "Navigating to: ${source.uri}", Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text(
                                        text = source.title,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = source.uri,
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Toolbar below the chat bubble
        Row(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .padding(top = 2.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val clip = ClipData.newPlainText("Copied Message", message.text)
                    clipboardManager.setPrimaryClip(clip)
                    Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }

            IconButton(onClick = onPinToggle, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.PushPin, contentDescription = "Pin", modifier = Modifier.size(13.dp), tint = if (message.isPinned) Color(0xFFFFB703) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }

            if (!isUser) {
                // Speak button
                IconButton(onClick = onSpeak, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Speak response", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (isUser) {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit prompt", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun TypingAnimationItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.widthIn(max = 160.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "AI PAL is writing",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                DotAnimation()
            }
        }
    }
}

@Composable
fun DotAnimation() {
    // Simply display structured pulsating trailing dots
    var state by remember { mutableStateOf(".") }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(400)
            state = when (state) {
                "." -> ".."
                ".." -> "..."
                else -> "."
            }
        }
    }
    Text(state, fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
}
