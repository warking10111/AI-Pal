package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AIPALApplication
import com.example.data.local.entity.AIMemory
import com.example.data.local.entity.AIPersona
import com.example.data.local.entity.ChatConversation
import com.example.data.local.entity.ChatMessage
import com.example.data.repository.ChatRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class MainViewModel(
    application: Application,
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    val database = (application as AIPALApplication).database

    val isDemoMode: Boolean
        get() = com.example.BuildConfig.GEMINI_API_KEY.isEmpty() || com.example.BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY"

    // Global navigation/screen states
    var currentScreen by mutableStateOf("home") // "home", "chat", "agents", "memories", "settings", "subscription", "pdf", "image_gen"
    var activeConversationId by mutableStateOf<String?>(null)
    var selectedPersonaId by mutableStateOf<String?>(null)

    // UI state flows from repositories
    val conversations: StateFlow<List<ChatConversation>> = chatRepository.activeConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedConversations: StateFlow<List<ChatConversation>> = chatRepository.archivedConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personas: StateFlow<List<AIPersona>> = chatRepository.allPersonas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customPersonas: StateFlow<List<AIPersona>> = chatRepository.customPersonas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<AIMemory>> = chatRepository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings State flows
    val themeState: StateFlow<String> = settingsRepository.theme
    val subscriptionState: StateFlow<String> = settingsRepository.subscription
    val creditsState: StateFlow<Int> = settingsRepository.credits
    val voiceNameState: StateFlow<String> = settingsRepository.voiceName
    val voiceSpeedState: StateFlow<Float> = settingsRepository.voiceSpeed
    val voiceLanguageState: StateFlow<String> = settingsRepository.voiceLanguage
    val streamingEnabled: StateFlow<Boolean> = settingsRepository.streamingEnabled

    // Chat Interface Local States
    var currentMessages by mutableStateOf<List<ChatMessage>>(emptyList())
    var isGenerating by mutableStateOf(false)
    var searchQueries by mutableStateOf("")

    // Image Upload State (base64 and mime metadata for vision analysis)
    var attachedImageBase64 by mutableStateOf<String?>(null)
    var attachedImageMime by mutableStateOf<String?>(null)
    var attachedFileName by mutableStateOf<String?>(null)

    // Image Generator Local States
    var imgPrompt by mutableStateOf("")
    var imgStyle by mutableStateOf("Realistic") // "Anime", "Realistic", "3D", "Painting", "Cyberpunk", "Fantasy", "Pixel Art"
    var imgAspectRatio by mutableStateOf("1:1") // "1:1", "16:9", "9:16"
    var imgQuality by mutableStateOf("1K") // "512px", "1K", "2K"
    var generatedImageOutput by mutableStateOf<String?>(null) // base64 string
    var isGeneratingImage by mutableStateOf(false)
    var imgError by mutableStateOf<String?>(null)

    // PDF Assistant Local States
    var docFileName by mutableStateOf<String?>(null)
    var docContentText by mutableStateOf<String?>(null)
    var isAnalyzingDoc by mutableStateOf(false)
    var docSummary by mutableStateOf<String?>(null)
    var docFlashcards by mutableStateOf<List<Pair<String, String>>>(emptyList()) // Question -> Answer
    var docNotes by mutableStateOf<List<String>>(emptyList())

    // Native Text to Speech Engine
    private var tts: TextToSpeech? = null
    var isTtsInitialized by mutableStateOf(false)
    var isSpeaking by mutableStateOf(false)

    init {
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsInitialized = true
                }
            }
        }
    }

    fun speakText(text: String) {
        if (!isTtsInitialized || tts == null) return
        viewModelScope.launch {
            // Apply speed setting
            tts?.setSpeechRate(settingsRepository.getVoiceSpeed())
            // Simple voice-name simulation via pitch adjustments
            when (settingsRepository.getVoiceName()) {
                "Leda" -> tts?.setPitch(1.3f) // Higher female-toned voice
                "Puck" -> tts?.setPitch(1.6f) // Playful child-toned voice
                "Fenrir" -> tts?.setPitch(0.6f) // Deep baritone-toned voice
                else -> tts?.setPitch(1.0f) // Standard Kore voice
            }
            isSpeaking = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AIPAL_TTS_ID")
            // Monitor speaking state in background
            viewModelScope.launch(Dispatchers.IO) {
                while (tts?.isSpeaking == true) {
                    kotlinx.coroutines.delay(100)
                }
                withContext(Dispatchers.Main) {
                    isSpeaking = false
                }
            }
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        isSpeaking = false
    }

    override fun onCleared() {
        tts?.shutdown()
        super.onCleared()
    }

    // Settings operations
    fun updateTheme(themeName: String) = settingsRepository.setTheme(themeName)
    fun updateSubscription(planName: String) {
        settingsRepository.setSubscription(planName)
        if (planName == "Premium") {
            settingsRepository.setCredits(9999)
        } else {
            settingsRepository.setCredits(15)
        }
    }
    fun updateVoice(voiceName: String) = settingsRepository.setVoiceName(voiceName)
    fun updateVoiceSpeed(speed: Float) = settingsRepository.setVoiceSpeed(speed)
    fun updateVoiceLanguage(lang: String) = settingsRepository.setVoiceLanguage(lang)
    fun updateStreaming(enabled: Boolean) = settingsRepository.setStreamingEnabled(enabled)

    // Conversation management
    fun selectConversation(id: String) {
        activeConversationId = id
        currentScreen = "chat"
        viewModelScope.launch {
            chatRepository.getMessages(id).collect {
                currentMessages = it
            }
        }
    }

    fun startNewChat(modelId: String, personaId: String? = null, initialTitle: String = "New Chat") {
        val newId = UUID.randomUUID().toString()
        viewModelScope.launch {
            chatRepository.createConversation(
                id = newId,
                title = initialTitle,
                modelId = modelId,
                personaId = personaId
            )
            selectConversation(newId)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            if (activeConversationId == id) {
                activeConversationId = null
                currentScreen = "home"
                currentMessages = emptyList()
            }
        }
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            chatRepository.updateConversationTitle(id, newTitle)
        }
    }

    fun archiveConversation(id: String, isArchived: Boolean) {
        viewModelScope.launch {
            chatRepository.updateConversationArchived(id, isArchived)
            if (activeConversationId == id && isArchived) {
                activeConversationId = null
                currentScreen = "home"
            }
        }
    }

    fun updateConversationFolder(id: String, folderName: String?) {
        viewModelScope.launch {
            chatRepository.updateConversationFolder(id, folderName)
        }
    }

    // Message actions
    fun sendUserMessage(text: String, webSearch: Boolean = false) {
        val conId = activeConversationId ?: return
        if (text.trim().isEmpty() && attachedImageBase64 == null) return

        // Check and consume free credit
        if (!settingsRepository.consumeCredit()) {
            viewModelScope.launch {
                chatRepository.saveMessage(ChatMessage(
                    id = UUID.randomUUID().toString(),
                    conversationId = conId,
                    role = "model",
                    text = "Usage limit reached. You have completed all 15 daily free messages. Please upgrade to the Premium Plan inside your Profile screen to get unlimited priority reasoning answers, premium voices, and HD image rendering.",
                    timestamp = System.currentTimeMillis()
                ))
            }
            return
        }

        val prompt = text
        val imgB64 = attachedImageBase64
        val imgMime = attachedImageMime
        val fileName = attachedFileName

        // Reset attachment states
        attachedImageBase64 = null
        attachedImageMime = null
        attachedFileName = null

        viewModelScope.launch {
            // Save User message in database
            val userMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conId,
                role = "user",
                text = prompt,
                imageUrl = if (imgB64 != null) "data:$imgMime;base64,$imgB64" else null,
                timestamp = System.currentTimeMillis(),
                fileName = fileName,
                mimeType = imgMime
            )
            chatRepository.saveMessage(userMsg)

            // Trigger API loading
            isGenerating = true
            val responseText = chatRepository.sendMessage(
                conversationId = conId,
                promptText = prompt,
                imageBase64 = imgB64,
                mimeType = imgMime,
                fileName = fileName,
                webSearchEnabled = webSearch
            )
            isGenerating = false

            // Auto text to speech if settings suggest speaking (voice conversation mode)
            if (currentScreen == "voice") {
                speakText(responseText)
            }
        }
    }

    fun togglePinMessage(msgId: String, isPinned: Boolean) {
        viewModelScope.launch {
            database.messageDao().updatePinned(msgId, !isPinned)
        }
    }

    fun deleteMessage(msgId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(msgId)
        }
    }

    fun updateMessageText(msgId: String, text: String) {
        viewModelScope.launch {
            database.messageDao().updateMessageText(msgId, text)
        }
    }

    // Custom Persona Building
    fun createCustomAgent(name: String, avatar: String, prompt: String, desc: String, temp: Float) {
        viewModelScope.launch {
            val agent = AIPersona(
                id = "agent_" + UUID.randomUUID().toString().take(6),
                name = name,
                avatar = avatar,
                prompt = prompt,
                temperature = temp,
                isCustom = true,
                description = desc
            )
            chatRepository.insertPersona(agent)
        }
    }

    // Memories management
    fun addMemory(content: String, category: String) {
        viewModelScope.launch {
            val memory = AIMemory(
                id = UUID.randomUUID().toString(),
                content = content,
                category = category,
                isEnabled = true
            )
            chatRepository.insertMemory(memory)
        }
    }

    fun toggleMemory(id: String, isEnabled: Boolean) {
        viewModelScope.launch {
            chatRepository.updateMemoryEnabled(id, isEnabled)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            chatRepository.deleteMemory(id)
        }
    }

    // Image Generator
    fun triggerImageGeneration() {
        if (imgPrompt.trim().isEmpty()) return
        isGeneratingImage = true
        imgError = null
        generatedImageOutput = null

        viewModelScope.launch {
            try {
                // Enhance prompt with style modifiers
                val fullPrompt = when (imgStyle) {
                    "Anime" -> "$imgPrompt, anime illustration style, high details, vibrant colors"
                    "Realistic" -> "$imgPrompt, realistic photo, hyperrealistic, ultra high detail, professional lighting"
                    "3D" -> "$imgPrompt, 3D render, Pixar style, soft lighting, clean shapes"
                    "Painting" -> "$imgPrompt, beautiful oil painting texture, artistic lighting, fine details"
                    "Cyberpunk" -> "$imgPrompt, cyberpunk theme, neon glow, futuristic dark alley elements"
                    "Fantasy" -> "$imgPrompt, fantasy illustration style, epic cinematic scale, magical sparkles"
                    "Pixel Art" -> "$imgPrompt, detailed retro 8-bit or 16-bit pixel art style"
                    else -> imgPrompt
                }
                val base64Output = chatRepository.generateImage(fullPrompt, imgAspectRatio, imgQuality)
                generatedImageOutput = base64Output
            } catch (e: Exception) {
                imgError = e.message ?: "Failed to generate image"
            } finally {
                isGeneratingImage = false
            }
        }
    }

    // Document / PDF summarizer
    fun loadLocalDoc(name: String, textContent: String) {
        docFileName = name
        docContentText = textContent
    }

    fun analyzeDocument() {
        val text = docContentText ?: return
        if (text.trim().isEmpty()) return

        isAnalyzingDoc = true
        docSummary = null
        docNotes = emptyList()
        docFlashcards = emptyList()

        viewModelScope.launch {
            // Let's call the repository to generate structured summaries & study materials
            val prompt = """
                Analyze the following document text and provide:
                1. A comprehensive 2-paragraph summary.
                2. 4 bullet-point study key notes.
                3. 3 study flashcard questions and answers (Format as: "Q: [Question] | A: [Answer]").
                
                Document content:
                $text
            """.trimIndent()

            // We create a temp session to generate document summary
            val tempConId = "temp_doc_analysis"
            chatRepository.createConversation(tempConId, "Doc Temp", "AI PAL Lite")
            val rawAnalysis = chatRepository.sendMessage(tempConId, prompt)
            chatRepository.deleteConversation(tempConId)

            parseDocumentAnalysis(rawAnalysis)
            isAnalyzingDoc = false
        }
    }

    private fun parseDocumentAnalysis(raw: String) {
        try {
            // Simple parsing logic of the structured output
            val lines = raw.lines()
            val summaryLines = mutableListOf<String>()
            val notesList = mutableListOf<String>()
            val flashList = mutableListOf<Pair<String, String>>()

            var mode = "summary" // "notes", "flashcards"

            for (line in lines) {
                val cleanLine = line.trim()
                if (cleanLine.isEmpty()) continue

                if (cleanLine.contains("notes", ignoreCase = true) || cleanLine.contains("bullet-point", ignoreCase = true)) {
                    mode = "notes"
                    continue
                }
                if (cleanLine.contains("flashcard", ignoreCase = true) || cleanLine.contains("Q:", ignoreCase = true)) {
                    mode = "flashcards"
                }

                when (mode) {
                    "summary" -> {
                        summaryLines.add(cleanLine)
                    }
                    "notes" -> {
                        if (cleanLine.startsWith("-") || cleanLine.startsWith("*") || cleanLine.matches(Regex("^\\d+\\..*"))) {
                            notesList.add(cleanLine.replace(Regex("^[-*\\d.]+\\s*"), ""))
                        }
                    }
                    "flashcards" -> {
                        if (cleanLine.contains("|")) {
                            val parts = cleanLine.split("|")
                            val q = parts.getOrNull(0)?.replace("Q:", "")?.replace(Regex("^[-*\\d.]+\\s*"), "")?.trim() ?: ""
                            val a = parts.getOrNull(1)?.replace("A:", "")?.trim() ?: ""
                            if (q.isNotEmpty()) {
                                flashList.add(Pair(q, a))
                            }
                        } else if (cleanLine.startsWith("Q:") && lines.getOrNull(lines.indexOf(line) + 1)?.trim()?.startsWith("A:") == true) {
                            val q = cleanLine.substring(2).trim()
                            val a = lines[lines.indexOf(line) + 1].trim().substring(2).trim()
                            flashList.add(Pair(q, a))
                        }
                    }
                }
            }

            docSummary = if (summaryLines.isNotEmpty()) summaryLines.take(3).joinToString("\n\n") else "Document Analysis complete. Structured notes generated below."
            docNotes = if (notesList.isNotEmpty()) notesList else listOf(
                "Read through core terms and concepts regularly.",
                "Review the summaries and test your knowledge with flashcards."
            )
            docFlashcards = if (flashList.isNotEmpty()) flashList else listOf(
                Pair("What is the core subject discussed?", "Summarized as the key theme of the file context."),
                Pair("How can this material be applied?", "Through regular reviews, active recall, and exercises.")
            )
        } catch (e: Exception) {
            docSummary = raw
            docNotes = listOf("Completed raw extraction.")
            docFlashcards = listOf(Pair("Review full analysis in summary?", "Yes, full text is available."))
        }
    }

    class Factory(
        private val application: Application,
        private val chatRepository: ChatRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application, chatRepository, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
