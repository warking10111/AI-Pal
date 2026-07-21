package com.aipal.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personas")
data class AIPersona(
    @PrimaryKey val id: String,
    val name: String,
    val avatar: String, // Can be an emoji, e.g. "💻" or "🎓"
    val prompt: String,
    val voiceName: String = "Kore",
    val temperature: Float = 0.7f,
    val isCustom: Boolean = false,
    val isPublic: Boolean = false,
    val description: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val isFavourite: Boolean = false,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "synced"
)
