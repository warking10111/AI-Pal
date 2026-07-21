package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class AIMemory(
    @PrimaryKey val id: String,
    val content: String,
    val category: String = "general", // "preference", "project", "writing_style", etc.
    val timestamp: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true
)
