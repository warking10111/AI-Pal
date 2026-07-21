package com.aipal.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "tools") val tools: List<Tool>? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String // Base64 encoded image or content
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseFormat") val responseFormat: ResponseFormat? = null,
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null,
    @Json(name = "thinkingConfig") val thinkingConfig: ThinkingConfig? = null,
    @Json(name = "imageConfig") val imageConfig: ImageConfig? = null,
    @Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @Json(name = "speechConfig") val speechConfig: SpeechConfig? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    @Json(name = "text") val text: ResponseFormatText? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(
    @Json(name = "mimeType") val mimeType: String
)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    @Json(name = "thinkingLevel") val thinkingLevel: String
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    @Json(name = "aspectRatio") val aspectRatio: String,
    @Json(name = "imageSize") val imageSize: String
)

@JsonClass(generateAdapter = true)
data class SpeechConfig(
    @Json(name = "voiceConfig") val voiceConfig: VoiceConfig
)

@JsonClass(generateAdapter = true)
data class VoiceConfig(
    @Json(name = "prebuiltVoiceConfig") val prebuiltVoiceConfig: PrebuiltVoiceConfig
)

@JsonClass(generateAdapter = true)
data class PrebuiltVoiceConfig(
    @Json(name = "voiceName") val voiceName: String
)

@JsonClass(generateAdapter = true)
data class Tool(
    @Json(name = "googleSearchRetrieval") val googleSearchRetrieval: GoogleSearchRetrieval? = null,
    @Json(name = "functionDeclarations") val functionDeclarations: List<FunctionDeclaration>? = null
)

@JsonClass(generateAdapter = true)
data class GoogleSearchRetrieval(
    @Json(name = "dynamicRetrievalConfig") val dynamicRetrievalConfig: DynamicRetrievalConfig? = null
)

@JsonClass(generateAdapter = true)
data class DynamicRetrievalConfig(
    @Json(name = "mode") val mode: String = "MODE_DYNAMIC",
    @Json(name = "dynamicThreshold") val dynamicThreshold: Float = 0.3f
)

@JsonClass(generateAdapter = true)
data class FunctionDeclaration(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "parameters") val parameters: Map<String, Any>? = null
)

// --- Response Data Classes ---

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null,
    @Json(name = "usageMetadata") val usageMetadata: UsageMetadata? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null,
    @Json(name = "finishReason") val finishReason: String? = null,
    @Json(name = "groundingMetadata") val groundingMetadata: GroundingMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GroundingMetadata(
    @Json(name = "webSearchQueries") val webSearchQueries: List<String>? = null,
    @Json(name = "groundingChunks") val groundingChunks: List<GroundingChunk>? = null,
    @Json(name = "groundingSupports") val groundingSupports: List<GroundingSupport>? = null
)

@JsonClass(generateAdapter = true)
data class GroundingChunk(
    @Json(name = "web") val web: WebSource? = null
)

@JsonClass(generateAdapter = true)
data class WebSource(
    @Json(name = "uri") val uri: String,
    @Json(name = "title") val title: String
)

@JsonClass(generateAdapter = true)
data class GroundingSupport(
    @Json(name = "segment") val segment: GroundingSegment? = null,
    @Json(name = "groundingChunkIndices") val groundingChunkIndices: List<Int>? = null
)

@JsonClass(generateAdapter = true)
data class GroundingSegment(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class UsageMetadata(
    @Json(name = "promptTokenCount") val promptTokenCount: Int? = null,
    @Json(name = "candidatesTokenCount") val candidatesTokenCount: Int? = null,
    @Json(name = "totalTokenCount") val totalTokenCount: Int? = null
)
