package com.aipal.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipal.app.data.local.entity.AIMemory
import com.aipal.app.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val memoryList by viewModel.memories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("All") }
    var selectedSortBy by remember { mutableStateOf("Newest") } // Newest, Oldest, Importance

    // Add / Edit Dialog States
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<AIMemory?>(null) }

    // Memory form states (for adding/editing)
    var contentText by remember { mutableStateOf("") }
    var categorySelected by remember { mutableStateOf("Important Facts") }
    var importanceScore by remember { mutableStateOf(3) }
    var tagsInput by remember { mutableStateOf("") }
    var isPinnedState by remember { mutableStateOf(false) }
    var isArchivedState by remember { mutableStateOf(false) }
    var isEnabledState by remember { mutableStateOf(true) }

    val categoriesList = listOf(
        "Preferences",
        "Projects",
        "Goals",
        "Writing Style",
        "People",
        "Important Facts",
        "Temporary Context",
        "Pinned Memories",
        "Archived Memories"
    )

    // Category symbols
    fun getCategoryIcon(cat: String): String {
        return when (cat) {
            "Preferences" -> "🎨"
            "Projects" -> "💼"
            "Goals" -> "🎯"
            "Writing Style" -> "✍️"
            "People" -> "👥"
            "Important Facts" -> "💡"
            "Temporary Context" -> "⏳"
            "Pinned Memories" -> "📌"
            "Archived Memories" -> "📦"
            else -> "📝"
        }
    }

    // Filter list based on search and selected category
    val filteredMemories = remember(memoryList, searchQuery, selectedFilterCategory, selectedSortBy) {
        var list = memoryList.filter { memory ->
            val matchesSearch = memory.content.contains(searchQuery, ignoreCase = true) ||
                    memory.tags.contains(searchQuery, ignoreCase = true) ||
                    memory.category.contains(searchQuery, ignoreCase = true)
            
            val matchesCategory = when (selectedFilterCategory) {
                "All" -> true
                "📌 Pinned" -> memory.isPinned || memory.category == "Pinned Memories"
                "📦 Archived" -> memory.isArchived || memory.category == "Archived Memories"
                else -> {
                    // strip emoji and compare
                    val cleanFilter = selectedFilterCategory.substring(2).trim()
                    memory.category.equals(cleanFilter, ignoreCase = true)
                }
            }
            matchesSearch && matchesCategory
        }

        // Apply sorting
        list = when (selectedSortBy) {
            "Oldest" -> list.sortedBy { it.createdDate }
            "Importance" -> list.sortedByDescending { it.importanceScore }
            else -> list.sortedByDescending { it.createdDate } // Newest
        }

        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Knowledge System", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Transforming memory into active facts", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Reset forms
                        contentText = ""
                        categorySelected = "Important Facts"
                        importanceScore = 3
                        tagsInput = ""
                        isPinnedState = false
                        isArchivedState = false
                        isEnabledState = true
                        showAddDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Memory Fact", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    contentText = ""
                    categorySelected = "Important Facts"
                    importanceScore = 3
                    tagsInput = ""
                    isPinnedState = false
                    isArchivedState = false
                    isEnabledState = true
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("add_memory_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Fact")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
        ) {
            // Search Input Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search memories, category, or tags...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("memory_search_input")
            )

            // Category Filter Scroll Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilterCategory == "All",
                        onClick = { selectedFilterCategory = "All" },
                        label = { Text("All", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilterCategory == "📌 Pinned",
                        onClick = { selectedFilterCategory = "📌 Pinned" },
                        label = { Text("📌 Pinned", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilterCategory == "📦 Archived",
                        onClick = { selectedFilterCategory = "📦 Archived" },
                        label = { Text("📦 Archived", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
                items(categoriesList) { cat ->
                    val label = "${getCategoryIcon(cat)} $cat"
                    FilterChip(
                        selected = selectedFilterCategory == label,
                        onClick = { selectedFilterCategory = label },
                        label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Sort & Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredMemories.size} knowledge entries found",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sorting Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Newest", "Oldest", "Importance").forEach { sort ->
                        val isSelected = selectedSortBy == sort
                        Text(
                            text = sort,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { selectedSortBy = sort }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Memories list
            if (filteredMemories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("🧠", fontSize = 48.sp)
                        Text(
                            text = "No Knowledge Entries Match",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Add some factual observations, goals, preferences, or project specs to feed the AI context system.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredMemories, key = { it.id }) { memory ->
                        KnowledgeMemoryCard(
                            memory = memory,
                            onToggleEnable = { viewModel.toggleMemory(memory.id, !memory.isEnabled) },
                            onTogglePin = { viewModel.toggleMemoryPin(memory.id, !memory.isPinned) },
                            onToggleArchive = { viewModel.toggleMemoryArchive(memory.id, !memory.isArchived) },
                            onToggleFavourite = { viewModel.toggleMemoryFavourite(memory.id, !memory.isFavourite) },
                            onDelete = {
                                viewModel.softDeleteMemory(memory.id)
                                Toast.makeText(context, "Fact soft-deleted", Toast.LENGTH_SHORT).show()
                            },
                            onEdit = {
                                // Initialize edit fields
                                contentText = memory.content
                                categorySelected = memory.category
                                importanceScore = memory.importanceScore
                                tagsInput = memory.tags
                                isPinnedState = memory.isPinned
                                isArchivedState = memory.isArchived
                                isEnabledState = memory.isEnabled
                                showEditDialog = memory
                            },
                            categoryIcon = getCategoryIcon(memory.category)
                        )
                    }
                }
            }
        }
    }

    // ================= ADD DIALOG =================
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Fact to Knowledge System", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        label = { Text("Observation/Fact Content") },
                        placeholder = { Text("e.g. Enjoys clean code, is building a Kotlin app.") },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("add_memory_content"),
                        maxLines = 4
                    )

                    // Category Selector
                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedCard(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${getCategoryIcon(categorySelected)} $categorySelected", fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            categoriesList.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${getCategoryIcon(cat)} $cat") },
                                    onClick = {
                                        categorySelected = cat
                                        // Auto adjust pins if selected specific categories
                                        if (cat == "Pinned Memories") {
                                            isPinnedState = true
                                        } else if (cat == "Archived Memories") {
                                            isArchivedState = true
                                        }
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Importance Star Bar
                    Text("Importance Score: $importanceScore / 5", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            val isSelected = star <= importanceScore
                            IconButton(
                                onClick = { importanceScore = star },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = "Star $star",
                                    tint = if (isSelected) Color(0xFFF1C40F) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    // Tags input
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = { Text("Tags (comma separated)") },
                        placeholder = { Text("e.g. personal, coding, android") },
                        modifier = Modifier.fillMaxWidth().testTag("add_memory_tags"),
                        singleLine = true
                    )

                    // Switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pin immediately?", fontSize = 13.sp)
                        Switch(checked = isPinnedState, onCheckedChange = { isPinnedState = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Archive immediately?", fontSize = 13.sp)
                        Switch(checked = isArchivedState, onCheckedChange = { isArchivedState = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (contentText.trim().isEmpty()) {
                            Toast.makeText(context, "Fact content cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addMemory(
                            content = contentText.trim(),
                            category = categorySelected,
                            importanceScore = importanceScore,
                            tags = tagsInput.trim(),
                            isPinned = isPinnedState,
                            isArchived = isArchivedState
                        )
                        showAddDialog = false
                        Toast.makeText(context, "Knowledge fact registered!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("add_memory_submit")
                ) {
                    Text("Save to System")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ================= EDIT DIALOG =================
    showEditDialog?.let { currentMemory ->
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Edit Knowledge Entry", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        label = { Text("Observation/Fact Content") },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("edit_memory_content"),
                        maxLines = 4
                    )

                    // Category Selector
                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedCard(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${getCategoryIcon(categorySelected)} $categorySelected", fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            categoriesList.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${getCategoryIcon(cat)} $cat") },
                                    onClick = {
                                        categorySelected = cat
                                        if (cat == "Pinned Memories") {
                                            isPinnedState = true
                                        } else if (cat == "Archived Memories") {
                                            isArchivedState = true
                                        }
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Importance Star Bar
                    Text("Importance Score: $importanceScore / 5", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            val isSelected = star <= importanceScore
                            IconButton(
                                onClick = { importanceScore = star },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = "Star $star",
                                    tint = if (isSelected) Color(0xFFF1C40F) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    // Tags input
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = { Text("Tags (comma separated)") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_memory_tags"),
                        singleLine = true
                    )

                    // Switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active fact?", fontSize = 13.sp)
                        Switch(checked = isEnabledState, onCheckedChange = { isEnabledState = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pin Fact?", fontSize = 13.sp)
                        Switch(checked = isPinnedState, onCheckedChange = { isPinnedState = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Archive Fact?", fontSize = 13.sp)
                        Switch(checked = isArchivedState, onCheckedChange = { isArchivedState = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (contentText.trim().isEmpty()) {
                            Toast.makeText(context, "Fact content cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.updateMemory(
                            id = currentMemory.id,
                            content = contentText.trim(),
                            category = categorySelected,
                            importanceScore = importanceScore,
                            tags = tagsInput.trim(),
                            isEnabled = isEnabledState,
                            isPinned = isPinnedState,
                            isArchived = isArchivedState
                        )
                        showEditDialog = null
                        Toast.makeText(context, "Knowledge entry updated!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("edit_memory_submit")
                ) {
                    Text("Apply Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun KnowledgeMemoryCard(
    memory: AIMemory,
    onToggleEnable: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onToggleFavourite: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    categoryIcon: String
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val formattedCreated = remember(memory.createdDate) { dateFormat.format(Date(memory.createdDate)) }
    val formattedModified = remember(memory.modifiedDate) { dateFormat.format(Date(memory.modifiedDate)) }

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    if (memory.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ),
                RoundedCornerShape(16.dp)
            )
            .testTag("memory_card_${memory.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (memory.isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
            else if (memory.isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(14.dp)
        ) {
            // Header: Category & Pin Indicator & Importance Stars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip / Label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$categoryIcon ${memory.category}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    if (memory.isPinned) {
                        Text(
                            text = "PINNED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    if (memory.isArchived) {
                        Text(
                            text = "ARCHIVED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Importance stars display
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= memory.importanceScore) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = null,
                            tint = if (star <= memory.importanceScore) Color(0xFFF1C40F) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body: Content
            Text(
                text = memory.content,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (memory.isEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tags row (if any)
            if (memory.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    memory.tags.split(",").forEach { tag ->
                        val cleanTag = tag.trim()
                        if (cleanTag.isNotEmpty()) {
                            Text(
                                text = "#$cleanTag",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Sync Status & Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dates
                Text(
                    text = "Modified: $formattedModified",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                // Sync status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (memory.syncStatus) {
                                    "synced" -> Color(0xFF2ECC71) // Nice Green
                                    "pending" -> Color(0xFFE67E22) // Orange
                                    else -> Color(0xFFE74C3C) // Red
                                }
                            )
                    )
                    Text(
                        text = memory.syncStatus.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Expandable details (UUID & Created Date)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "UUID: ${memory.id}",
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Created Date: $formattedCreated",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "User ID: ${memory.userId}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            // Interactive Actions: Pin, Archive, Edit, Delete, Enable Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pin & Archive buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Pin Icon Button
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (memory.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            contentDescription = "Pin memory",
                            tint = if (memory.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Archive Icon Button
                    IconButton(
                        onClick = onToggleArchive,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (memory.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = "Archive memory",
                            tint = if (memory.isArchived) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Edit Button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit memory",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Favourite Star Button
                    IconButton(
                        onClick = onToggleFavourite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (memory.isFavourite) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Favourite memory",
                            tint = if (memory.isFavourite) Color(0xFFF1C40F) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Enable toggle (Switch/Active status) and Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (memory.isEnabled) "Active" else "Disabled",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (memory.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Switch(
                        checked = memory.isEnabled,
                        onCheckedChange = { onToggleEnable() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.scale(0.8f)
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete memory",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
