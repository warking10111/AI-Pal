package com.aipal.app.data.provider

import com.aipal.app.data.local.entity.ChatMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GrokProvider(private val getApiKey: () -> String) : AIProvider {
    override val name = "grok"
    override val displayName = "xAI Grok"

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
                text = "✨ **xAI Grok (Simulated Witty Response)** ✨\n\n" +
                        "Let's grok this situation together, human. I'm processing your inquiry through AI PAL's active provider abstraction.\n\n" +
                        "### 🚀 Grokking: \"$prompt\"\n" +
                        "Here is the absolute reality, with a touch of cosmic perspective:\n" +
                        "- **The Big Picture**: Often, what seems like a complex mystery is just a series of logical inputs awaiting structured organization. \n" +
                        "- **Direct Answer**: Focus on building highly decoupled modules. Keep things stateless, direct, and fast.\n" +
                        "- **Humorous Take**: Remember, code is like humor. When you have to explain it, it's bad. Keep it simple and clear!\n\n" +
                        "*This response was simulated locally because no xAI Grok API Key was provided in settings.*"
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
                put("model", "grok-beta") // standard chat completion model
                put("messages", messagesArray)
                if (temperature != null) {
                    put("temperature", temperature)
                }
            }

            val request = Request.Builder()
                .url("https://api.x.ai/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ProviderResponse(text = "Grok Error: HTTP ${response.code} ${response.message}")
                }
                val bodyStr = response.body?.string() ?: return ProviderResponse(text = "Error: Received empty response body from Grok.")
                val json = JSONObject(bodyStr)
                val choices = json.getJSONArray("choices")
                val text = choices.getJSONObject(0).getJSONObject("message").getString("content")
                return ProviderResponse(text = text)
            }
        } catch (e: Exception) {
            return ProviderResponse(text = "Grok API Exception: ${e.message}")
        }
    }
}
