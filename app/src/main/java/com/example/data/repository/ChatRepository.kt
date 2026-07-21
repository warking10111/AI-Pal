package com.example.data.repository

import android.util.Base64
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader
import java.io.ByteArrayOutputStream
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
        val isDemoMode = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY"

        // Get conversation info
        val conversation = conversationDao.getConversationById(conversationId)
        val modelId = conversation?.modelId ?: "AI PAL Lite"
        val personaId = conversation?.personaId

        if (isDemoMode) {
            val responseText = generateSimulatedResponse(promptText, modelId, personaId)
            val responseMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "model",
                text = responseText,
                timestamp = System.currentTimeMillis()
            )
            saveMessage(responseMessage)
            return responseText
        }

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
        val isDemoMode = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY"

        if (isDemoMode) {
            return generateProceduralImage(prompt, aspectRatio)
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

    private fun generateProceduralImage(prompt: String, aspectRatio: String): String {
        val width = 512
        val height = when (aspectRatio) {
            "16:9" -> 288
            "9:16" -> 910
            else -> 512
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Select colors based on prompt content
        val isAnime = prompt.contains("anime", ignoreCase = true)
        val isCyberpunk = prompt.contains("cyberpunk", ignoreCase = true)
        val isRealistic = prompt.contains("realistic", ignoreCase = true)
        val is3D = prompt.contains("3d", ignoreCase = true)
        val isPainting = prompt.contains("painting", ignoreCase = true)
        val isFantasy = prompt.contains("fantasy", ignoreCase = true)
        val isPixel = prompt.contains("pixel", ignoreCase = true)

        // 1. Draw Background Gradient
        val shader = when {
            isCyberpunk -> {
                LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(0xFF0D0221.toInt(), 0xFF240046.toInt(), 0xFF10002B.toInt()),
                    null, Shader.TileMode.CLAMP
                )
            }
            isAnime -> {
                LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(0xFFFA90A9.toInt(), 0xFFFFCAD4.toInt(), 0xFFB5E2FA.toInt()),
                    null, Shader.TileMode.CLAMP
                )
            }
            isFantasy -> {
                LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(0xFF1E1E24.toInt(), 0xFF4A148C.toInt(), 0xFF311B92.toInt()),
                    null, Shader.TileMode.CLAMP
                )
            }
            isRealistic -> {
                LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(0xFFF35B04.toInt(), 0xFFF18701.toInt(), 0xFF7678ED.toInt()),
                    null, Shader.TileMode.CLAMP
                )
            }
            isPainting -> {
                LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(0xFF2C3E50.toInt(), 0xFF000000.toInt(), 0xFF2980B9.toInt()),
                    null, Shader.TileMode.CLAMP
                )
            }
            isPixel -> {
                LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(0xFF1A1A1A.toInt(), 0xFF333333.toInt(), 0xFF00FF00.toInt()),
                    null, Shader.TileMode.CLAMP
                )
            }
            else -> {
                LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(0xFF1E293B.toInt(), 0xFF0F172A.toInt(), 0xFF334155.toInt()),
                    null, Shader.TileMode.CLAMP
                )
            }
        }
        paint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // 2. Draw Decorative elements based on style
        paint.style = Paint.Style.FILL
        if (isCyberpunk) {
            paint.color = 0x40FF007F
            paint.strokeWidth = 2f
            paint.style = Paint.Style.STROKE
            val gridSize = 40
            for (i in 0..width step gridSize) {
                canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paint)
            }
            for (i in 0..height step gridSize) {
                canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paint)
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = 0xFF00FFFF.toInt()
            canvas.drawCircle(width / 2f, height / 2f, 100f, paint)
            paint.color = 0xFFFF007F.toInt()
            canvas.drawCircle(width / 2f, height / 2f, 120f, paint)
        } else if (isAnime) {
            paint.color = Color.WHITE
            val random = java.util.Random(12345)
            for (i in 0..40) {
                val rx = random.nextFloat() * width
                val ry = random.nextFloat() * height
                val radius = random.nextFloat() * 3f + 1f
                canvas.drawCircle(rx, ry, radius, paint)
            }
            paint.color = 0xE6FFFFFF.toInt()
            canvas.drawCircle(width - 100f, 100f, 50f, paint)
            paint.color = 0xFF3A0CA3.toInt()
            val path = android.graphics.Path()
            path.moveTo(0f, height.toFloat())
            path.lineTo(150f, height - 120f)
            path.lineTo(300f, height.toFloat())
            path.lineTo(400f, height - 180f)
            path.lineTo(width.toFloat(), height.toFloat())
            path.close()
            canvas.drawPath(path, paint)
        } else if (isFantasy) {
            val random = java.util.Random(999)
            paint.style = Paint.Style.FILL
            for (i in 0..3) {
                val rx = random.nextFloat() * width
                val ry = random.nextFloat() * height
                val rRadius = random.nextFloat() * 120f + 60f
                val color = if (i % 2 == 0) 0x30E040FB else 0x3000E5FF
                paint.color = color
                canvas.drawCircle(rx, ry, rRadius, paint)
            }
            paint.color = 0xFFFFFFFF.toInt()
            for (i in 0..15) {
                val rx = random.nextFloat() * width
                val ry = random.nextFloat() * height
                canvas.drawRect(rx - 3f, ry - 3f, rx + 3f, ry + 3f, paint)
            }
        } else if (isRealistic) {
            paint.color = 0xFF3D0066.toInt()
            val path = android.graphics.Path()
            path.moveTo(0f, height.toFloat())
            path.lineTo(width * 0.3f, height * 0.5f)
            path.lineTo(width * 0.6f, height.toFloat())
            path.lineTo(width * 0.8f, height * 0.6f)
            path.lineTo(width.toFloat(), height.toFloat())
            path.close()
            canvas.drawPath(path, paint)
            paint.color = 0xFFFFD700.toInt()
            canvas.drawCircle(width * 0.5f, height * 0.4f, 40f, paint)
        } else if (isPainting) {
            paint.style = Paint.Style.FILL
            paint.color = 0x50FF5722
            canvas.drawCircle(width * 0.3f, height * 0.4f, 120f, paint)
            paint.color = 0x5000BCD4
            canvas.drawCircle(width * 0.7f, height * 0.6f, 150f, paint)
            paint.color = 0x509C27B0
            val path = android.graphics.Path()
            path.moveTo(width * 0.2f, height.toFloat())
            path.lineTo(width * 0.5f, height * 0.3f)
            path.lineTo(width * 0.8f, height.toFloat())
            path.close()
            canvas.drawPath(path, paint)
        } else if (isPixel) {
            val random = java.util.Random(42)
            val blockSize = 16
            for (x in 0 until width step blockSize) {
                for (y in 0 until height step blockSize) {
                    if (random.nextFloat() > 0.6f) {
                        paint.color = if (random.nextBoolean()) 0xFF00FF00.toInt() else 0xFF003300.toInt()
                        canvas.drawRect(
                            x.toFloat(), y.toFloat(),
                            (x + blockSize).toFloat(), (y + blockSize).toFloat(),
                            paint
                        )
                    }
                }
            }
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = 0x40FFFFFF
            canvas.drawCircle(width / 2f, height / 2f, 80f, paint)
            canvas.drawCircle(width / 2f, height / 2f, 140f, paint)
            canvas.drawCircle(width / 2f, height / 2f, 200f, paint)
        }

        // 3. Draw text banner referencing style and prompt words
        paint.style = Paint.Style.FILL
        paint.color = 0x99000000.toInt()
        canvas.drawRect(0f, height - 60f, width.toFloat(), height.toFloat(), paint)

        paint.color = Color.WHITE
        paint.textSize = 14f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        val styleName = when {
            isCyberpunk -> "CYBERPUNK ART"
            isAnime -> "ANIME ILLUSTRATION"
            isFantasy -> "FANTASY CONCEPT"
            isRealistic -> "REALISTIC PHOTO"
            isPainting -> "ARTISTIC PAINTING"
            isPixel -> "RETRO PIXEL ART"
            else -> "DIGITAL COMPOSITION"
        }
        canvas.drawText(styleName, 20f, height - 35f, paint)

        paint.textSize = 10f
        paint.color = 0xFFCCCCCC.toInt()
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
        val cleanPrompt = if (prompt.length > 50) prompt.take(50) + "..." else prompt
        canvas.drawText("Prompt: \"$cleanPrompt\"", 20f, height - 15f, paint)

        // 4. Compress to PNG Base64
        val outStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        val bytes = outStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun generateSimulatedResponse(
        promptText: String,
        modelId: String,
        personaId: String?
    ): String {
        val lowerPrompt = promptText.lowercase()
        val demoHeader = "✨ **AI PAL Demo Mode (Simulated Response)** ✨\n\n"

        if (lowerPrompt.contains("analyze the following document text") || lowerPrompt.contains("document content:")) {
            val isSyllabus = lowerPrompt.contains("syllabus") || lowerPrompt.contains("cs302")
            val isEthical = lowerPrompt.contains("ethical") || lowerPrompt.contains("aiguidelines")

            if (isSyllabus) {
                return """
Here is the comprehensive syllabus analysis for CS302: Mobile Systems and Jetpack Compose.

This course is designed to guide students through the advanced paradigms of modern Android engineering. It emphasizes the foundational principles of Jetpack Compose, state lifecycle management, and high-performance reactive flows using Kotlin Coroutines.

Throughout the term, students will engage in practical laboratory projects, integrating local Room databases with structured repository patterns. Security, accessibility, and Material Design 3 guidelines form the core quality metrics for all built applications.

STUDY KEY NOTES:
- Core architecture centers on Unidirectional Data Flow (UDF) using StateFlow and ViewModel.
- Lifecycle security: Composable functions must be side-effect free, leveraging LaunchedEffect and rememberCoroutineScope where appropriate.
- Database caching: Room acts as the offline-first single source of truth, integrated via Kotlin Symbol Processing (KSP).
- Accessibility and polish: Interactive components must conform to Material 3 density guidelines, assuring at least 48dp touch targets.

FLASHCARDS:
Q: What is the main design pattern of Jetpack Compose? | A: Unidirectional Data Flow (UDF), where state flows down and events flow up.
Q: How does Room handle offline data safely? | A: It acts as the offline-first single source of truth, caching local schema operations using KSP and SQLite.
Q: What is the minimum touch target size for standard Material 3 accessibility? | A: It requires at least 48dp x 48dp for all interactive clickable elements.
                """.trimIndent()
            } else if (isEthical) {
                return """
Here is the ethical AI Guidelines document analysis.

This framework outlines standard ethical boundaries when developing modern AI Companions and Operating Systems. It emphasizes user-in-the-loop validation, secure local encryption of user memories, and transparent data boundaries.

Developers are strongly encouraged to optimize token consumption, respect rate limits, and provide offline fallback behaviors to assure software accessibility and reliability under constrained conditions.

STUDY KEY NOTES:
- Ethical AI Companion development requires absolute transparency, secure local encryption, and user-consented learning loops.
- Rate limiting and local offline capabilities ensure accessible tools regardless of real-time cellular connections.
- Contextual memory management must be explicitly clear, enabling users to purge or enable specific saved preferences.
- Privacy guidelines require developers to mask or securely transmit API tokens, adhering to sandbox protocols.

FLASHCARDS:
Q: What is the primary focus of ethical AI Companion design? | A: Assuring absolute user privacy, transparent context memories, and explicit human-in-the-loop control.
Q: Why are local offline fallback systems critical? | A: They guarantee accessibility and reliable functionality under poor network conditions or key rate limits.
Q: How should contextual memory be handled? | A: It must be transparent, allowing users to toggle, view, or purge specific saved details in the database.
                """.trimIndent()
            } else {
                val extractedTitle = promptText.substringAfter("Document content:").trim().take(40)
                return """
Here is the customized study analysis for your uploaded document.

This document contains key guidelines, concepts, and informative passages related to $extractedTitle. It outlines theoretical and practical considerations that form the cornerstone of this subject matter.

By breaking down the paragraphs into key pillars, we establish a clearer path for learning, recall, and retention of these specialized details.

STUDY KEY NOTES:
- Primary theme centers on the core structures outlined in the uploaded text body.
- Structured analysis: Systematically categorizes complex concepts into scannable lists and key-value metrics.
- Active practice: Incorporates active recall questions to help verify knowledge retention during review.
- Practical optimization: Synthesizes high-density text into direct takeaways for streamlined studying.

FLASHCARDS:
Q: What is the main theme of the analyzed document? | A: It focuses on the concepts and guidelines related to: $extractedTitle.
Q: How can active recall be applied to this document? | A: By reviewing the structured keynotes and utilizing the flashcards to test memory retention.
Q: What is the benefit of synthesizing this text? | A: It filters out fluff, delivering direct takeaways to optimize study efficiency.
                """.trimIndent()
            }
        }

        if (personaId == "agent_programmer" || modelId == "AI PAL Coding" || lowerPrompt.contains("code") || lowerPrompt.contains("kotlin") || lowerPrompt.contains("java") || lowerPrompt.contains("function")) {
            return demoHeader + """
### 🚀 Optimized Kotlin Solution

Here is a modern, highly-optimized implementation using **Jetpack Compose** and **Kotlin Coroutines** for managing state safely in your application.

```kotlin
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TaskViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadTasks() {
        viewModelScope.launch {
            try {
                val data = repository.fetchTasks()
                _uiState.value = TaskUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }
}
```

#### 🛠️ Core Optimization & Security Highlights:
1. **Unidirectional Data Flow (UDF)**: Strictly uses read-only state flows (`asStateFlow`) to protect state encapsulation from external modifications.
2. **Context Safety**: Offloads network or database lookups using safe scopes in the Coroutine context (`Dispatchers.IO`).
3. **Structured Concurrency**: Properly bound to the ViewModel lifecycle (`viewModelScope`), preventing memory leaks and dangling asynchronous handles.
            """.trimIndent()
        }

        if (personaId == "agent_travel" || lowerPrompt.contains("travel") || lowerPrompt.contains("trip") || lowerPrompt.contains("paris") || lowerPrompt.contains("itinerary")) {
            return demoHeader + """
### ✈️ Customized Wanderlust Itinerary

Welcome to your curated travel guide! Here is an immersive, high-quality itinerary designed to experience local flavor, gastronomy, and cultural highlights.

| Day | Period | Activity Details | Cost Estimate |
|---|---|---|---|
| **Day 1** | Morning | Traditional local bakery tasting & walking neighborhood architecture tour | ${'$'}15 |
| | Afternoon | VIP entry to world-class historical galleries and museum collections | ${'$'}30 |
| | Evening | Handcrafted rooftop tasting menu overlooking the glowing skyline | ${'$'}65 |
| **Day 2** | Morning | Scenic rental bike ride along historical water canals and nature trails | ${'$'}12 |
| | Afternoon | Exploring hidden artisanal shops and botanical flower gardens | Free |
| | Evening | Live acoustic theater or jazz performance in a cozy cellar | ${'$'}25 |

#### 💡 Essential Travel Pro-Tips:
* **Transit Pass**: Invest in a 48-hour local transit card to enjoy unlimited public transport.
* **Artisanal Spots**: Check the botanical garden early (before 9 AM) to beat the crowd and capture perfect soft lighting!
            """.trimIndent()
        }

        if (personaId == "agent_fitness" || lowerPrompt.contains("fitness") || lowerPrompt.contains("exercise") || lowerPrompt.contains("workout") || lowerPrompt.contains("gym") || lowerPrompt.contains("diet")) {
            return demoHeader + """
### 💪 High-Performance Athletic Guide

Let's push towards your physical and nutritional goals with absolute consistency! Here is your custom-tailored daily breakdown:

#### 🏋️ Active Exercise Routine:
1. **Dynamic Mobility** (10 mins): General dynamic stretching, arm circles, leg swings, and core activation.
2. **Strength Training** (40 mins):
   * *Barbell squats / Goblet squats*: 4 sets of 8-12 reps
   * *Push-ups / Incline DB press*: 3 sets of 10-15 reps
   * *Deadlifts / Romanian DB deadlifts*: 3 sets of 8 reps
3. **HIIT Finisher** (10 mins): Tabata protocol (20s sprint / 10s recovery) cycling or kettlebell swings.

#### 🥗 Premium Nutrition Guidelines:
* **Protein Priority**: Target 1.6g of high-quality protein per kilogram of body weight daily (chicken breast, tofu, eggs, lentils).
* **Hydration**: Consume 3-4 liters of water. Replenish electrolytes after high-intensity sweating.
* **Active Sleep**: Aim for 7-8 hours of deep restorative sleep to maximize muscle fiber reconstruction.
            """.trimIndent()
        }

        if (personaId == "agent_teacher" || lowerPrompt.contains("study") || lowerPrompt.contains("explain") || lowerPrompt.contains("learn") || lowerPrompt.contains("math")) {
            return demoHeader + """
### 🎓 Socrates Academic Study Guide

Let's break down this complex academic concept clearly using logical progression and memorable analogies.

#### 🔬 The Concept Analogy:
Imagine your phone's memory is a physical writing desk. **RAM (Random Access Memory)** is the surface area of the desk—the larger the desk, the more documents you can keep open and work on simultaneously. **Storage (SSD/HDD)** is the filing cabinet underneath—it has vast space, but taking a document out of the cabinet takes significantly more time than glancing at what's already on the desk.

#### 📝 Core Principles:
1. **Active Recall**: Don't just re-read notes. Close the booklet and write down everything you remember.
2. **Spaced Repetition**: Review the material in expanding intervals (1 day, 3 days, 7 days, 14 days) to cement facts in your long-term memory.
3. **Feynman Method**: Explain the concept aloud as if you were teaching a 10-year-old child. This quickly highlights gaps in your understanding!
            """.trimIndent()
        }

        if (personaId == "agent_law" || lowerPrompt.contains("law") || lowerPrompt.contains("legal") || lowerPrompt.contains("contract")) {
            return demoHeader + """
### ⚖️ Educational Legal Breakdown

*Disclaimer: I am an educational informational AI. This is for conceptual learning purposes only and does not constitute official legal counsel.*

#### 🔑 Key Principles of Contract Law:
1. **Offer and Acceptance**: Clear intent to enter a binding agreement with mutual assent.
2. **Consideration**: Something of value exchanged between parties (money, services, action, or promise).
3. **Legal Capacity**: Both parties must be of legal age, sound mind, and have proper authority to execute.
4. **Lawful Objective**: The terms and purposes of the contract must comply with statutory laws.

#### 📂 Standard Clauses to Watch:
* **Indemnification**: Restricts liability or allocates costs in case of breach or damage.
* **Severability**: Ensures that if one clause is found invalid by a court, the remainder of the contract remains in effect.
            """.trimIndent()
        }

        if (personaId == "agent_medical" || lowerPrompt.contains("health") || lowerPrompt.contains("medical") || lowerPrompt.contains("symptom") || lowerPrompt.contains("cough") || lowerPrompt.contains("fever")) {
            return demoHeader + """
### 🩺 Clinical Wellness Facts

*Disclaimer: I am a medical informational helper. The details below are general clinical study insights and must not replace professional diagnosis or care.*

#### 🌡️ General Symptom Care & Wellness Guide:
1. **Hydration First**: Hydrate heavily with warm water or herbal teas to assist cellular respiration and clear respiratory passages.
2. **Restful Sleep**: Sleep is the body's primary active immunological state. Maximize recovery by resting 8+ hours.
3. **Monitor Vital Signs**: Watch for key red flags (persistent fever above 39°C, chest tightness, or respiratory distress) which require immediate clinical attention.

#### 🥗 Immune-Supportive Nutrition:
* **Vitamin C & D**: Help support leukocyte (white blood cell) functions.
* **Zinc**: Crucial for optimal T-cell activation and mucosal membrane repair.
            """.trimIndent()
        }

        if (lowerPrompt.contains("hello") || lowerPrompt.contains("hi") || lowerPrompt.contains("hey") || lowerPrompt.contains("who are you")) {
            return demoHeader + """
### Hello! I am AI PAL, your personal AI Companion. 👋

I'm fully initialized and ready to assist you! Since we are in **Demo Mode**, I am using a local high-performance simulation engine. 

#### 🌟 What you can explore right now:
* **Interactive Agents**: Select different expert assistants (Study Guide, Fitness, Coder, Travel) from the top bar or sidebar.
* **Document Summarizer**: Upload any `.txt` or `.pdf` (or use our presets) to generate study keynotes and double-sided flashcards instantly.
* **Visual Art Studio**: Describe any scene (e.g., *A cyberpunk coffee shop*) in the **Image Generation** screen, select a style, and generate procedural art on-the-fly!
* **Voice Companion**: Switch to **Voice Mode** to converse hands-free with active voice speed and personality settings.

*To activate live connections to real-time LLMs (including Gemini Flash and Pro), simply configure your **GEMINI_API_KEY** in the Secrets panel in AI Studio.*
            """.trimIndent()
        }

        return demoHeader + """
### 🔍 AI PAL Comprehensive Insights

You asked about **"$promptText"**. Here is an educational, highly structured breakdown addressing your inquiry.

#### 📋 Detailed Core Analysis:
* **Primary Context**: The concept of *"${promptText.substringBefore(" ").replaceFirstChar { it.uppercase() }}"* plays a significant role in modern technology, lifestyle design, and information synthesis.
* **Key Facet A**: Understanding the underlying patterns allows for increased productivity and clearer decision-making.
* **Key Facet B**: Integrating cognitive tools (like context memory and active learning) ensures your productivity stays consistently ahead.

#### 🛠️ Recommended Action Plan:
1. **Deconstruct**: Break the topic into small, manageable modules.
2. **Simulate**: Test different scenarios and practice recall exercises.
3. **Refine**: Check your outcomes periodically and adjust your writing style or parameters to improve results.

*Tip: You can use specialized AI Agent profiles (like Socrates for learning or Coder Pro for syntax) to get highly targeted feedback on this topic!*
        """.trimIndent()
    }
}
