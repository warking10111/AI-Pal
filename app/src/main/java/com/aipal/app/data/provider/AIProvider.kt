package com.aipal.app.data.provider

import com.aipal.app.data.local.entity.ChatMessage

interface AIProvider {
    val name: String
    val displayName: String

    suspend fun generateResponse(
        prompt: String,
        history: List<ChatMessage>,
        systemPrompt: String,
        temperature: Float?,
        isDemoMode: Boolean,
        webSearchEnabled: Boolean,
        imageBase64: String? = null,
        mimeType: String? = null,
        modelId: String
    ): ProviderResponse
}

data class ProviderResponse(
    val text: String,
    val sourcesJson: String? = null
)
