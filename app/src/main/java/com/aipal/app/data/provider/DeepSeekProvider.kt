package com.aipal.app.data.provider

import com.aipal.app.data.local.entity.ChatMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DeepSeekProvider(private val getApiKey: () -> String) : AIProvider {
    override val name = "deepseek"
    override val displayName = "DeepSeek V3/R1"

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
                text = "✨ **DeepSeek-R1 (Simulated Reasoning Response)** ✨\n\n" +
                        "### 💭 Thinking Process\n" +
                        "1. **Analyze user query**: \"$prompt\"\n" +
                        "2. **Deconstruct elements**: User is testing the multi-provider system on AI PAL. I need to formulate a response demonstrating deep logical analysis, high-density structured outlines, and precision coding capabilities.\n" +
                        "3. **Synthesize strategy**: Provide a highly efficient, production-grade outline showing optimal algorithms or architectural solutions.\n" +
                        "4. **Format output**: Render clear markdown headers with step-by-step instructions.\n" +
                        "--- \n\n" +
                        "Greetings from DeepSeek! I am processing your query via the AI PAL custom provider layer.\n\n" +
                        "### ⚡ Optimized Strategic Solution\n" +
                        "- **Direct Synthesis**: This challenge is best addressed by prioritizing clean, stateless function execution.\n" +
                        "- **Efficiency Metrics**: Keeping variables localized prevents memory overhead and improves speed by up to 40%.\n" +
                        "- **Step-by-step**: Design a clean, decoupled repository pattern, allowing different components to interact purely through interfaces.\n\n" +
                        "*This response was simulated locally because no DeepSeek API Key was provided in settings.*"
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
                put("model", "deepseek-chat")
                put("messages", messagesArray)
                if (temperature != null) {
                    put("temperature", temperature)
                }
            }

            val request = Request.Builder()
                .url("https://api.deepseek.com/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ProviderResponse(text = "DeepSeek Error: HTTP ${response.code} ${response.message}")
                }
                val bodyStr = response.body?.string() ?: return ProviderResponse(text = "Error: Received empty response body from DeepSeek.")
                val json = JSONObject(bodyStr)
                val choices = json.getJSONArray("choices")
                val text = choices.getJSONObject(0).getJSONObject("message").getString("content")
                return ProviderResponse(text = text)
            }
        } catch (e: Exception) {
            return ProviderResponse(text = "DeepSeek API Exception: ${e.message}")
        }
    }
}
