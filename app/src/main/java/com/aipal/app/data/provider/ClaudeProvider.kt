package com.aipal.app.data.provider

import com.aipal.app.data.local.entity.ChatMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ClaudeProvider(private val getApiKey: () -> String) : AIProvider {
    override val name = "claude"
    override val displayName = "Anthropic Claude 3.5"

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
                text = "✨ **Anthropic Claude 3.5 (Simulated Response)** ✨\n\n" +
                        "Hello, I am Claude. I am currently running through the provider abstraction layer of AI PAL.\n\n" +
                        "### 🌿 Nuanced Contextual Insight\n" +
                        "Concerning: \"$prompt\"\n\n" +
                        "Let's explore this with the clarity, depth, and structured reasoning typical of my design:\n" +
                        "- **Synthesized Perspective**: This subject centers on a core set of logical principles, demanding careful attention to detail.\n" +
                        "- **Refinement**: To approach this optimally, I recommend dividing the steps into clear, distinct, and highly testable modules.\n" +
                        "- **Elegant Execution**: Maintaining a robust single source of truth ensures the architectural integrity remains uncompromised.\n\n" +
                        "*This response was simulated locally because no Anthropic API Key was provided in settings.*"
            )
        }

        try {
            val messagesArray = JSONArray()
            // Add history
            for (msg in history) {
                messagesArray.put(JSONObject().apply {
                    put("role", if (msg.role == "model") "assistant" else "user")
                    put("content", msg.text)
                })
            }

            // Add current user prompt
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })

            val jsonBody = JSONObject().apply {
                put("model", "claude-3-5-sonnet-20241022")
                put("max_tokens", 4096)
                put("system", systemPrompt)
                put("messages", messagesArray)
                if (temperature != null) {
                    put("temperature", temperature)
                }
            }

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ProviderResponse(text = "Claude Error: HTTP ${response.code} ${response.message}")
                }
                val bodyStr = response.body?.string() ?: return ProviderResponse(text = "Error: Received empty response body from Claude.")
                val json = JSONObject(bodyStr)
                val content = json.getJSONArray("content")
                val text = content.getJSONObject(0).getString("text")
                return ProviderResponse(text = text)
            }
        } catch (e: Exception) {
            return ProviderResponse(text = "Claude API Exception: ${e.message}")
        }
    }
}
