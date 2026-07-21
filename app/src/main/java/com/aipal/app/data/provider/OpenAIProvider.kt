package com.aipal.app.data.provider

import com.aipal.app.data.local.entity.ChatMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAIProvider(private val getApiKey: () -> String) : AIProvider {
    override val name = "openai"
    override val displayName = "OpenAI GPT-4o"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

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
        val apiKey = getApiKey()
        if (isDemoMode || apiKey.isBlank()) {
            return ProviderResponse(
                text = "✨ **OpenAI GPT-4o (Simulated Response)** ✨\n\n" +
                        "I am OpenAI's GPT-4o, processing your request through the AI PAL provider abstraction.\n\n" +
                        "### 🧠 Analysis & Response\n" +
                        "You asked: \"$prompt\"\n\n" +
                        "Here is a balanced, highly detailed analysis designed to optimize your task:\n" +
                        "1. **Core Concept**: Breaking this down reveals a structured set of variables that require systematic execution.\n" +
                        "2. **Strategic Steps**: Focus on incremental progress, ensuring states are saved locally and modularized effectively.\n" +
                        "3. **Optimization**: Limit redundant recompositions or loops to maintain optimal client performance.\n\n" +
                        "*This response was simulated locally because no OpenAI API Key was provided in settings.*"
            )
        }

        try {
            val messagesArray = JSONArray()
            // Add system instruction
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })

            // Add history
            for (msg in history) {
                messagesArray.put(JSONObject().apply {
                    put("role", if (msg.role == "model") "assistant" else "user")
                    put("content", msg.text)
                })
            }

            // Add current message
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })

            val jsonBody = JSONObject().apply {
                put("model", "gpt-4o")
                put("messages", messagesArray)
                if (temperature != null) {
                    put("temperature", temperature)
                }
            }

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ProviderResponse(text = "OpenAI Error: HTTP ${response.code} ${response.message}")
                }
                val bodyStr = response.body?.string() ?: return ProviderResponse(text = "Error: Received empty response body from OpenAI.")
                val json = JSONObject(bodyStr)
                val choices = json.getJSONArray("choices")
                val text = choices.getJSONObject(0).getJSONObject("message").getString("content")
                return ProviderResponse(text = text)
            }
        } catch (e: Exception) {
            return ProviderResponse(text = "OpenAI API Exception: ${e.message}")
        }
    }
}
