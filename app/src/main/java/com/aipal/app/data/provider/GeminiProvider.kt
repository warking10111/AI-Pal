package com.aipal.app.data.provider

import com.aipal.app.BuildConfig
import com.aipal.app.data.local.entity.ChatMessage
import com.aipal.app.data.model.*
import com.aipal.app.data.remote.RetrofitClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class GeminiProvider : AIProvider {
    override val name = "gemini"
    override val displayName = "Google Gemini"

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val sourcesAdapter = moshi.adapter<List<WebSource>>(
        Types.newParameterizedType(List::class.java, WebSource::class.java)
    )

    override suspend fun generateResponse(
        prompt: String,
        history: List<ChatMessage>,
        systemPrompt: String,
        temperature: Float?,
        isDemoMode: Boolean,
        webSearchEnabled: Boolean,
        imageBase64: String?,
        mimeType: String?,
        modelId: String
    ): ProviderResponse {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // Resolve model mapping
        var apiModel = "gemini-3.5-flash"
        var temp = temperature
        var thinkingConfig: ThinkingConfig? = null
        var toolsList: List<Tool>? = null

        when (modelId) {
            "AI PAL Pro" -> {
                apiModel = "gemini-3.1-pro-preview"
                if (temp == null) temp = 0.7f
            }
            "AI PAL Fast" -> {
                apiModel = "gemini-3.5-flash"
                if (temp == null) temp = 0.5f
            }
            "AI PAL Reasoning" -> {
                apiModel = "gemini-3.1-pro-preview"
                thinkingConfig = ThinkingConfig(thinkingLevel = "low")
            }
            "AI PAL Creative" -> {
                apiModel = "gemini-3.1-pro-preview"
                if (temp == null) temp = 1.0f
            }
            "AI PAL Coding" -> {
                apiModel = "gemini-3.1-pro-preview"
                if (temp == null) temp = 0.2f
            }
            "AI PAL Vision" -> {
                apiModel = "gemini-3.5-flash"
                if (temp == null) temp = 0.4f
            }
            "AI PAL Research" -> {
                apiModel = "gemini-3.1-pro-preview"
                if (temp == null) temp = 0.4f
                toolsList = listOf(Tool(googleSearchRetrieval = GoogleSearchRetrieval()))
            }
        }

        if (webSearchEnabled && toolsList == null) {
            toolsList = listOf(Tool(googleSearchRetrieval = GoogleSearchRetrieval()))
        }

        val contents = mutableListOf<Content>()
        for (msg in history) {
            val parts = mutableListOf<Part>()
            if (msg.text.isNotBlank()) {
                parts.add(Part(text = msg.text))
            }
            if (msg.imageUrl != null && msg.mimeType != null) {
                if (msg.imageUrl.startsWith("data:")) {
                    val rawBase64 = msg.imageUrl.substringAfter(",")
                    parts.add(Part(inlineData = InlineData(mimeType = msg.mimeType, data = rawBase64)))
                }
            }
            if (parts.isNotEmpty()) {
                contents.add(Content(parts = parts, role = msg.role))
            }
        }

        val currentParts = mutableListOf<Part>()
        if (prompt.isNotBlank()) {
            currentParts.add(Part(text = prompt))
        }
        if (imageBase64 != null && mimeType != null) {
            currentParts.add(Part(inlineData = InlineData(mimeType = mimeType, data = imageBase64)))
        }
        if (currentParts.isNotEmpty()) {
            contents.add(Content(parts = currentParts, role = "user"))
        }

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = GenerationConfig(
                temperature = temp,
                thinkingConfig = thinkingConfig
            ),
            tools = toolsList,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val response = RetrofitClient.service.generateContent(apiModel, apiKey, request)
        val candidate = response.candidates?.firstOrNull()
        val textResponse = candidate?.content?.parts?.firstOrNull()?.text ?: "Error: Received empty response from Gemini."

        var sourcesJson: String? = null
        val groundingMetadata = candidate?.groundingMetadata
        val chunks = groundingMetadata?.groundingChunks
        if (chunks != null && chunks.isNotEmpty()) {
            val sources = chunks.mapNotNull { it.web }
            if (sources.isNotEmpty()) {
                sourcesJson = sourcesAdapter.toJson(sources)
            }
        }

        return ProviderResponse(text = textResponse, sourcesJson = sourcesJson)
    }
}
