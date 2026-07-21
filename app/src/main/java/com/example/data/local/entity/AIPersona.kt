package com.example.data.local.entity

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
    val description: String = ""
)
