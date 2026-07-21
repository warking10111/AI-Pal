package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import coil.compose.AsyncImage
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract DI container objects
        val app = application as AIPALApplication
        val chatRepo = app.chatRepository
        val settingsRepo = app.settingsRepository

        // Setup MainViewModel
        val factory = MainViewModel.Factory(application, chatRepo, settingsRepo)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            val themeSetting by viewModel.themeState.collectAsState()

            MyApplicationTheme(themeSetting = themeSetting) {
                MainLayoutContainer(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayoutContainer(viewModel: MainViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val activeScreen = viewModel.currentScreen
    val activeConId = viewModel.activeConversationId
    val conversationsList by viewModel.conversations.collectAsState()
    val archivedList by viewModel.archivedConversations.collectAsState()

    var showRenameDialogId by remember { mutableStateOf<String?>(null) }
    var renameInputTitle by remember { mutableStateOf("") }

    // Dialog for renaming threads
    if (showRenameDialogId != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialogId = null },
            title = { Text("Rename Chat Thread") },
            text = {
                OutlinedTextField(
                    value = renameInputTitle,
                    onValueChange = { renameInputTitle = it },
                    placeholder = { Text("Enter custom title...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = showRenameDialogId!!
                        if (renameInputTitle.trim().isNotEmpty()) {
                            viewModel.renameConversation(id, renameInputTitle.trim())
                            showRenameDialogId = null
                            renameInputTitle = ""
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialogId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
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
                            Icon(Icons.Default.SupportAgent, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "AI PAL Workspace",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Start New Chat Button
                    Button(
                        onClick = {
                            viewModel.startNewChat("AI PAL Lite")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("drawer_new_chat_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Conversation")
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "Active Chat History",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Historic threads list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(conversationsList) { con ->
                            val isSelected = activeConId == con.id
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else {
                                        Color.Transparent
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectConversation(con.id)
                                        scope.launch { drawerState.close() }
                                    },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = "Chat",
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = con.title,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Action mini-buttons (rename, archive, delete)
                                    Row {
                                        IconButton(
                                            onClick = {
                                                showRenameDialogId = con.id
                                                renameInputTitle = con.title
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(12.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.archiveConversation(con.id, true)
                                                Toast.makeText(context, "Thread archived", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Archive, contentDescription = "Archive", modifier = Modifier.size(12.dp))
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteConversation(con.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Collapsible Archived threads section
                    if (archivedList.isNotEmpty()) {
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))
                        Text(
                            "Archived Threads (${archivedList.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(archivedList) { archivedCon ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            viewModel.archiveConversation(archivedCon.id, false)
                                            viewModel.selectConversation(archivedCon.id)
                                            scope.launch { drawerState.close() }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = archivedCon.title,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.Unarchive, contentDescription = "Restore", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Main layout App Bar when drawer is closed and NOT inside detailed Chat or Voice screen
                if (activeScreen != "chat" && activeScreen != "voice") {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Gradient Logo "P"
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFFD0BCFF),
                                                    Color(0xFF381E72)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "P",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                }

                                Column {
                                    Text(
                                        text = "AI PAL",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFA8A2FF),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .background(
                                                color = Color(0xFF1C1B1F),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color(0xFF49454F),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Pro v2.4",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown indicator",
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFF49454F), CircleShape)
                                    .clickable { viewModel.currentScreen = "subscription" }
                            ) {
                                AsyncImage(
                                    model = "https://api.dicebear.com/7.x/avataaars/svg?seed=Hein",
                                    contentDescription = "User Avatar",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (activeScreen != "voice") {
                    NavigationBar(
                        tonalElevation = 8.dp,
                        modifier = Modifier.height(72.dp)
                    ) {
                        NavigationBarItem(
                            selected = activeScreen == "home",
                            onClick = { viewModel.currentScreen = "home" },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_home_tab")
                        )

                        NavigationBarItem(
                            selected = activeScreen == "chat",
                            onClick = {
                                if (viewModel.activeConversationId == null) {
                                    viewModel.startNewChat("AI PAL Lite")
                                } else {
                                    viewModel.currentScreen = "chat"
                                }
                            },
                            icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chat") },
                            label = { Text("Chat", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_chat_tab")
                        )

                        NavigationBarItem(
                            selected = activeScreen == "agents",
                            onClick = { viewModel.currentScreen = "agents" },
                            icon = { Icon(Icons.Default.SupportAgent, contentDescription = "Agents") },
                            label = { Text("Agents", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_agents_tab")
                        )

                        NavigationBarItem(
                            selected = activeScreen == "memories",
                            onClick = { viewModel.currentScreen = "memories" },
                            icon = { Icon(Icons.Default.Memory, contentDescription = "Memory") },
                            label = { Text("Memory", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_memory_tab")
                        )

                        NavigationBarItem(
                            selected = activeScreen == "settings" || activeScreen == "subscription",
                            onClick = { viewModel.currentScreen = "settings" },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_settings_tab")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (activeScreen) {
                    "home" -> HomeScreen(viewModel)
                    "chat" -> ChatScreen(viewModel)
                    "agents" -> AgentsScreen(viewModel)
                    "memories" -> MemoriesScreen(viewModel)
                    "pdf" -> PdfScreen(viewModel)
                    "image_gen" -> ImageGenScreen(viewModel)
                    "subscription" -> SubscriptionScreen(viewModel)
                    "settings" -> SettingsScreen(viewModel)
                    "voice" -> VoiceScreen(viewModel)
                }
            }
        }
    }
}
