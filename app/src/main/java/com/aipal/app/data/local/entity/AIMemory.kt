package com.aipal.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class AIMemory(
    @PrimaryKey val id: String,
    val content: String,
    val category: String = "Important Facts", // Preferences, Projects, Goals, Writing Style, People, Important Facts, Temporary Context, Pinned Memories, Archived Memories
    val timestamp: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true,
    val userId: String = "guest",
    val importanceScore: Int = 3, // 1 to 5 (or similar)
    val tags: String = "", // Comma-separated tags
    val createdDate: Long = System.currentTimeMillis(),
    val modifiedDate: Long = System.currentTimeMillis(),
    val syncStatus: String = "synced", // "synced", "pending", "failed"
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val isFavourite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
