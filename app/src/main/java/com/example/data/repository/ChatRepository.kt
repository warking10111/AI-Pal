package com.example.data.repository

import android.util.Base64
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AIMemory
import com.example.data.local.entity.AIPersona
import com.example.data.local.entity.ChatConversation
import com.example.data.local.entity.ChatMessage
import com.example.data.model.*
import com.example.data.remote.RetrofitClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(private val database: AppDatabase) {

    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val personaDao = database.personaDao()
    private val memoryDao = database.memoryDao()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val sourcesAdapter = moshi.adapter<List<WebSource>>(
        Types.newParameterizedType(List::class.java, WebSource::class.java)
    )

    // Conversations
    val activeConversations: Flow<List<ChatConversation>> = conversationDao.getActiveConversations()
    val archivedConversations: Flow<List<ChatConversation>> = conversationDao.getArchivedConversations()

    // Personas & Memories
    val allPersonas: Flow<List<AIPersona>> = personaDao.getAllPersonas()
    val customPersonas: Flow<List<AIPersona>> = personaDao.getCustomPersonas()
    val allMemories: Flow<List<AIMemory>> = memoryDao.getAllMemories()

    fun getMessages(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    suspend fun getPersonaById(id: String): AIPersona? {
        return personaDao.getPersonaById(id)
    }

    suspend fun insertPersona(persona: AIPersona) {
        personaDao.insertPersona(persona)
    }

    suspend fun deletePersona(id: String) {
        personaDao.deletePersonaById(id)
    }

    suspend fun insertMemory(memory: AIMemory) {
        memoryDao.insertMemory(memory)
    }

    suspend fun updateMemoryEnabled(id: String, isEnabled: Boolean) {
        memoryDao.updateMemoryEnabled(id, isEnabled)
    }

    suspend fun deleteMemory(id: String) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun createConversation(
        id: String,
        title: String,
        modelId: String,
        personaId: String? = null,
        folderName: String? = null
    ) {
        val conversation = ChatConversation(
            id = id,
            title = title,
            modelId = modelId,
            personaId = personaId,
            timestamp = System.currentTimeMillis(),
            isArchived = false,
            folderName = folderName
        )
        conversationDao.insertConversation(conversation)
    }

    suspend fun updateConversationTitle(id: String, title: String) {
        conversationDao.updateTitle(id, title)
    }

    suspend fun updateConversationFolder(id: String, folderName: String?) {
        conversationDao.updateFolder(id, folderName)
    }

    suspend fun updateConversationArchived(id: String, isArchived: Boolean) {
        conversationDao.updateArchived(id, isArchived)
    }

    suspend fun deleteConversation(id: String) {
        messageDao.deleteMessagesForConversation(id)
        conversationDao.deleteConversationById(id)
    }

    suspend fun saveMessage(message: ChatMessage) {
        messageDao.insertMessage(message)
    }

    suspend fun updateMessageText(id: String, text: String) {
        messageDao.updateMessageText(id, text)
    }

    suspend fun deleteMessage(id: String) {
        messageDao.deleteMessageById(id)
    }

    /**
     * Sends a message to the Gemini API, retrieves and caches the response.
     */
    suspend fun sendMessage(
        conversationId: String,
        promptText: String,
        imageBase64: String? = null,
        mimeType: String? = null,
        fileName: String? = null,
        webSearchEnabled: Boolean = false
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val errorMsg = "Error: Gemini API Key is missing. Please configure your key in the Secrets panel in AI Studio."
            saveMessage(ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "model",
                text = errorMsg,
                timestamp = System.currentTimeMillis()
            ))
            return errorMsg
        }

        // Get conversation info
        val conversation = conversationDao.getConversationById(conversationId)
        val modelId = conversation?.modelId ?: "AI PAL Lite"
        val personaId = conversation?.personaId

        // Resolve API Model name and specific configurations
        var apiModel = "gemini-3.5-flash"
        var temp: Float? = null
        var systemPrompt = "You are AI PAL, a highly capable, premium, and friendly AI assistant."
        var thinkingConfig: ThinkingConfig? = null
        var toolsList: List<Tool>? = null

        // Model settings mapping
        when (modelId) {
            "AI PAL Pro" -> {
                apiModel = "gemini-3.1-pro-preview"
                temp = 0.7f
            }
            "AI PAL Fast" -> {
                apiModel = "gemini-3.5-flash"
                temp = 0.5f
            }
            "AI PAL Reasoning" -> {
                apiModel = "gemini-3.1-pro-preview"
                thinkingConfig = ThinkingConfig(thinkingLevel = "low")
            }
            "AI PAL Creative" -> {
                apiModel = "gemini-3.1-pro-preview"
                temp = 1.0f
                systemPrompt = "You are AI PAL Creative, a imaginative, literary, and brainstorming assistant."
            }
            "AI PAL Coding" -> {
                apiModel = "gemini-3.1-pro-preview"
                temp = 0.2f
                systemPrompt = "You are AI PAL Coding, a senior software architect. Provide direct, highly optimized, secure code blocks with concise annotations."
            }
            "AI PAL Vision" -> {
                apiModel = "gemini-3.5-flash"
                temp = 0.4f
            }
            "AI PAL Research" -> {
                apiModel = "gemini-3.1-pro-preview"
                temp = 0.4f
                toolsList = listOf(Tool(googleSearchRetrieval = GoogleSearchRetrieval()))
            }
        }

        // Override system prompt if specialized persona is active
        if (personaId != null) {
            val persona = personaDao.getPersonaById(personaId)
            if (persona != null) {
                systemPrompt = persona.prompt
                temp = persona.temperature
            }
        }

        // Load enabled memories to ground the AI
        val memories = memoryDao.getEnabledMemoriesSync()
        if (memories.isNotEmpty()) {
            val memoryBlock = memories.joinToString(separator = "\n") { "- ${it.content}" }
            systemPrompt += "\n\nCRITICAL CONTEXT MEMORY (Remember these facts about the user):\n$memoryBlock"
        }

        // Add search tool if research model or web search is checked
        if (webSearchEnabled && toolsList == null) {
            toolsList = listOf(Tool(googleSearchRetrieval = GoogleSearchRetrieval()))
        }

        // Get past messages for context
        val pastMessages = messageDao.getMessagesForConversationSync(conversationId)
        val contents = mutableListOf<Content>()

        // Append historical turns (limit to last 20 messages for context window management)
        val windowSize = 20
        val limitedHistory = if (pastMessages.size > windowSize) {
            pastMessages.takeLast(windowSize)
        } else {
            pastMessages
        }

        for (msg in limitedHistory) {
            val parts = mutableListOf<Part>()
            if (msg.text.isNotBlank()) {
                parts.add(Part(text = msg.text))
            }
            
            if (msg.imageUrl != null && msg.mimeType != null) {
                // If we saved base64 inside imageUrl, we load it here to pass to vision API
                if (msg.imageUrl.startsWith("data:")) {
                    val rawBase64 = msg.imageUrl.substringAfter(",")
                    parts.add(Part(inlineData = InlineData(mimeType = msg.mimeType, data = rawBase64)))
                }
            }
            if (parts.isNotEmpty()) {
                contents.add(Content(parts = parts, role = msg.role))
            }
        }

        // Append current message
        val currentParts = mutableListOf<Part>()
        if (promptText.isNotBlank()) {
            currentParts.add(Part(text = promptText))
        }
        if (imageBase64 != null && mimeType != null) {
            currentParts.add(Part(inlineData = InlineData(mimeType = mimeType, data = imageBase64)))
        }
        if (currentParts.isNotEmpty()) {
            contents.add(Content(parts = currentParts, role = "user"))
        }

        // Create the final request
        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = GenerationConfig(
                temperature = temp,
                thinkingConfig = thinkingConfig
            ),
            tools = toolsList,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiModel, apiKey, request)
            val candidate = response.candidates?.firstOrNull()
            val textResponse = candidate?.content?.parts?.firstOrNull()?.text ?: "Error: Received empty response from model."

            // Extract Grounding Sources (Search references)
            var sourcesJson: String? = null
            val groundingMetadata = candidate?.groundingMetadata
            val chunks = groundingMetadata?.groundingChunks
            if (chunks != null && chunks.isNotEmpty()) {
                val sources = chunks.mapNotNull { it.web }
                if (sources.isNotEmpty()) {
                    sourcesJson = sourcesAdapter.toJson(sources)
                }
            }

            // Save Response message
            val responseMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "model",
                text = textResponse,
                timestamp = System.currentTimeMillis(),
                sourcesJson = sourcesJson
            )
            saveMessage(responseMessage)
            return textResponse
        } catch (e: Exception) {
            val errorText = "Network Error: ${e.message}. Please check your internet connection or verify your API key limits."
            val errorMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "model",
                text = errorText,
                timestamp = System.currentTimeMillis()
            )
            saveMessage(errorMsg)
            return errorText
        }
    }

    /**
     * Generates an image using the gemini-2.5-flash-image model.
     * Returns a base64-encoded PNG image string.
     */
    suspend fun generateImage(prompt: String, aspectRatio: String = "1:1", quality: String = "1K"): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw Exception("Gemini API Key is missing. Please configure it in the Secrets panel.")
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user")),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = quality),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        try {
            val response = RetrofitClient.service.generateContent("gemini-2.5-flash-image", apiKey, request)
            val imagePart = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }
            if (imagePart?.inlineData != null) {
                return imagePart.inlineData.data // returns raw base64 string
            }
            throw Exception("Failed to extract generated image from response.")
        } catch (e: Exception) {
            throw Exception("Image Generation Error: ${e.message}")
        }
    }
}
