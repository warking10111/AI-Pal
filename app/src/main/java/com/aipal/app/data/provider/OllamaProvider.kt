package com.aipal.app.data.provider

import com.aipal.app.data.local.entity.ChatMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OllamaProvider(private val getBaseUrl: () -> String) : AIProvider {
    override val name = "ollama"
    override val displayName = "Ollama Local (Llama 3)"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
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
        val url = getBaseUrl()
        if (isDemoMode || url.isBlank()) {
            return ProviderResponse(
                text = "✨ **Ollama Llama 3 (Simulated Local Response)** ✨\n\n" +
                        "Hello, I am a local Llama 3 instance running offline inside Ollama.\n\n" +
                        "### 💻 Local Execution Logs\n" +
                        "- Target: `llama3:latest`\n" +
                        "- Context Window: `8192` tokens\n" +
                        "- Generation Speed: `~48 tokens/sec` (simulated)\n\n" +
                        "### ✏️ Local Assistant Answer\n" +
                        "For: \"$prompt\"\n\n" +
                        "1. **Offline Privacy**: Because I run entirely on your local hardware, your questions never traverse external commercial cloud networks.\n" +
                        "2. **Reliable Performance**: I offer consistent latency without being affected by internet speeds or API rate-limits.\n" +
                        "3. **Modular Integration**: You can customize Ollama configurations, switch local models, and tweak system prompts on-the-fly.\n\n" +
                        "*This response was simulated locally because no custom Ollama URL endpoint was specified or configured.*"
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
                put("model", "llama3")
                put("messages", messagesArray)
                put("stream", false)
                if (temperature != null) {
                    val options = JSONObject()
                    options.put("temperature", temperature)
                    put("options", options)
                }
            }

            // Ensure the URL ends correctly for the chat API
            val cleanUrl = if (url.endsWith("/")) "${url}api/chat" else "$url/api/chat"

            val request = Request.Builder()
                .url(cleanUrl)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ProviderResponse(text = "Ollama Error: HTTP ${response.code} ${response.message}")
                }
                val bodyStr = response.body?.string() ?: return ProviderResponse(text = "Error: Received empty response body from Ollama.")
                val json = JSONObject(bodyStr)
                val messageJson = json.getJSONObject("message")
                val text = messageJson.getString("content")
                return ProviderResponse(text = text)
            }
        } catch (e: Exception) {
            return ProviderResponse(text = "Ollama API Connection Exception: ${e.message}\nMake sure your local Ollama server is running and accessible (default is http://10.0.2.2:11434 on emulator).")
        }
    }
}
