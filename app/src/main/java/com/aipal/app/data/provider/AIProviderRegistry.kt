package com.aipal.app.data.provider

import com.aipal.app.data.repository.SettingsRepository

class AIProviderRegistry(private val settingsRepository: SettingsRepository) {

    private val providers = mapOf<String, AIProvider>(
        "gemini" to GeminiProvider(),
        "openai" to OpenAIProvider { settingsRepository.getOpenAiKey() },
        "claude" to ClaudeProvider { settingsRepository.getClaudeKey() },
        "deepseek" to DeepSeekProvider { settingsRepository.getDeepSeekKey() },
        "grok" to GrokProvider { settingsRepository.getGrokKey() },
        "ollama" to OllamaProvider { settingsRepository.getOllamaUrl() }
    )

    fun getProvider(name: String): AIProvider {
        return providers[name.lowercase()] ?: providers["gemini"]!!
    }

    fun getAllProviders(): List<AIProvider> {
        return providers.values.toList()
    }
}
