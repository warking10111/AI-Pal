package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class ChatMessage(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user" or "model"
    val text: String,
    val imageUrl: String? = null, // Path/URL to generated or analyzed image
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val fileName: String? = null, // Optional document/file attachment name
    val mimeType: String? = null, // Optional document mime type
    val sourcesJson: String? = null // Web search sources as serializable JSON array string
)
