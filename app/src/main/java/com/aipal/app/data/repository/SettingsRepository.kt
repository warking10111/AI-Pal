package com.aipal.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("aipal_settings", Context.MODE_PRIVATE)

    // Flows for reactive UI updates
    private val _theme = MutableStateFlow(getTheme())
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _subscription = MutableStateFlow(getSubscription())
    val subscription: StateFlow<String> = _subscription.asStateFlow()

    private val _voiceName = MutableStateFlow(getVoiceName())
    val voiceName: StateFlow<String> = _voiceName.asStateFlow()

    private val _voiceSpeed = MutableStateFlow(getVoiceSpeed())
    val voiceSpeed: StateFlow<Float> = _voiceSpeed.asStateFlow()

    private val _voiceLanguage = MutableStateFlow(getVoiceLanguage())
    val voiceLanguage: StateFlow<String> = _voiceLanguage.asStateFlow()

    private val _streamingEnabled = MutableStateFlow(isStreamingEnabled())
    val streamingEnabled: StateFlow<Boolean> = _streamingEnabled.asStateFlow()

    private val _credits = MutableStateFlow(getCredits())
    val credits: StateFlow<Int> = _credits.asStateFlow()

    private val _activeProvider = MutableStateFlow(getActiveProvider())
    val activeProvider: StateFlow<String> = _activeProvider.asStateFlow()

    private val _openaiKey = MutableStateFlow(getOpenAiKey())
    val openaiKey: StateFlow<String> = _openaiKey.asStateFlow()

    private val _claudeKey = MutableStateFlow(getClaudeKey())
    val claudeKey: StateFlow<String> = _claudeKey.asStateFlow()

    private val _deepseekKey = MutableStateFlow(getDeepSeekKey())
    val deepseekKey: StateFlow<String> = _deepseekKey.asStateFlow()

    private val _grokKey = MutableStateFlow(getGrokKey())
    val grokKey: StateFlow<String> = _grokKey.asStateFlow()

    private val _ollamaUrl = MutableStateFlow(getOllamaUrl())
    val ollamaUrl: StateFlow<String> = _ollamaUrl.asStateFlow()

    fun getTheme(): String = prefs.getString("key_theme", "AMOLED") ?: "AMOLED"
    fun setTheme(value: String) {
        prefs.edit().putString("key_theme", value).apply()
        _theme.value = value
    }

    fun getSubscription(): String = prefs.getString("key_subscription", "Free") ?: "Free"
    fun setSubscription(value: String) {
        prefs.edit().putString("key_subscription", value).apply()
        _subscription.value = value
    }

    fun getVoiceName(): String = prefs.getString("key_voice", "Kore") ?: "Kore"
    fun setVoiceName(value: String) {
        prefs.edit().putString("key_voice", value).apply()
        _voiceName.value = value
    }

    fun getVoiceSpeed(): Float = prefs.getFloat("key_voice_speed", 1.0f)
    fun setVoiceSpeed(value: Float) {
        prefs.edit().putFloat("key_voice_speed", value).apply()
        _voiceSpeed.value = value
    }

    fun getVoiceLanguage(): String = prefs.getString("key_voice_language", "English") ?: "English"
    fun setVoiceLanguage(value: String) {
        prefs.edit().putString("key_voice_language", value).apply()
        _voiceLanguage.value = value
    }

    fun isStreamingEnabled(): Boolean = prefs.getBoolean("key_streaming", true)
    fun setStreamingEnabled(value: Boolean) {
        prefs.edit().putBoolean("key_streaming", value).apply()
        _streamingEnabled.value = value
    }

    fun getCredits(): Int = prefs.getInt("key_credits", 15) // Free users get 15 credits initially
    fun setCredits(value: Int) {
        prefs.edit().putInt("key_credits", value).apply()
        _credits.value = value
    }

    fun getActiveProvider(): String = prefs.getString("key_active_provider", "gemini") ?: "gemini"
    fun setActiveProvider(value: String) {
        prefs.edit().putString("key_active_provider", value).apply()
        _activeProvider.value = value
    }

    fun getOpenAiKey(): String = prefs.getString("key_openai_key", "") ?: ""
    fun setOpenAiKey(value: String) {
        prefs.edit().putString("key_openai_key", value).apply()
        _openaiKey.value = value
    }

    fun getClaudeKey(): String = prefs.getString("key_claude_key", "") ?: ""
    fun setClaudeKey(value: String) {
        prefs.edit().putString("key_claude_key", value).apply()
        _claudeKey.value = value
    }

    fun getDeepSeekKey(): String = prefs.getString("key_deepseek_key", "") ?: ""
    fun setDeepSeekKey(value: String) {
        prefs.edit().putString("key_deepseek_key", value).apply()
        _deepseekKey.value = value
    }

    fun getGrokKey(): String = prefs.getString("key_grok_key", "") ?: ""
    fun setGrokKey(value: String) {
        prefs.edit().putString("key_grok_key", value).apply()
        _grokKey.value = value
    }

    fun getOllamaUrl(): String = prefs.getString("key_ollama_url", "http://10.0.2.2:11434") ?: "http://10.0.2.2:11434"
    fun setOllamaUrl(value: String) {
        prefs.edit().putString("key_ollama_url", value).apply()
        _ollamaUrl.value = value
    }

    fun consumeCredit(): Boolean {
        val current = getCredits()
        val currentSub = getSubscription()
        if (currentSub != "Free") return true // Premium users have unlimited messages
        if (current <= 0) return false
        setCredits(current - 1)
        return true
    }

    fun getDailyMessageLimit(): Int = 15
}
