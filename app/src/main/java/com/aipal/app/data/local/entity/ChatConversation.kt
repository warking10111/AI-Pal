package com.aipal.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ChatConversation(
    @PrimaryKey val id: String,
    val title: String,
    val modelId: String, // e.g., "AI PAL Lite", "AI PAL Pro", "AI PAL Reasoning", etc.
    val personaId: String? = null, // ID of specialized agent if active
    val timestamp: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val folderName: String? = null, // Folder classification (e.g. "Work", "Personal")
    val userId: String = "guest",
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,
    val isFavourite: Boolean = false,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "synced"
)
