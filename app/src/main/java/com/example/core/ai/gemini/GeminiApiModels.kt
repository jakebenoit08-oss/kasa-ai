package com.example.core.ai.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
  @Json(name = "mimeType") val mimeType: String? = null,
  @Json(name = "data") val data: String? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
  @Json(name = "text") val text: String? = null,
  @Json(name = "inlineData") val inlineData: GeminiInlineData? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
  @Json(name = "role") val role: String? = null, // "user" or "model"
  @Json(name = "parts") val parts: List<GeminiPart> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GeminiImageConfig(
  @Json(name = "aspectRatio") val aspectRatio: String? = "1:1",
  @Json(name = "imageSize") val imageSize: String? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
  @Json(name = "temperature") val temperature: Float? = null,
  @Json(name = "topP") val topP: Float? = null,
  @Json(name = "topK") val topK: Int? = null,
  @Json(name = "responseMimeType") val responseMimeType: String? = null,
  @Json(name = "responseModalities") val responseModalities: List<String>? = null,
  @Json(name = "imageConfig") val imageConfig: GeminiImageConfig? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiTool(
  @Json(name = "googleSearch") val googleSearch: Map<String, String>? = null,
  @Json(name = "google_search") val google_search: Map<String, String>? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentRequest(
  @Json(name = "contents") val contents: List<GeminiContent>,
  @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
  @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
  @Json(name = "tools") val tools: List<GeminiTool>? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingWeb(
  @Json(name = "uri") val uri: String? = null,
  @Json(name = "title") val title: String? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingChunk(
  @Json(name = "web") val web: GeminiGroundingWeb? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingSegment(
  @Json(name = "startIndex") val startIndex: Int? = null,
  @Json(name = "endIndex") val endIndex: Int? = null,
  @Json(name = "text") val text: String? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingSupport(
  @Json(name = "groundingChunkIndices") val groundingChunkIndices: List<Int>? = null,
  @Json(name = "confidenceScores") val confidenceScores: List<Float>? = null,
  @Json(name = "segment") val segment: GeminiGroundingSegment? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingMetadata(
  @Json(name = "webSearchQueries") val webSearchQueries: List<String>? = null,
  @Json(name = "groundingChunks") val groundingChunks: List<GeminiGroundingChunk>? = null,
  @Json(name = "groundingSupports") val groundingSupports: List<GeminiGroundingSupport>? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
  @Json(name = "content") val content: GeminiContent? = null,
  @Json(name = "finishReason") val finishReason: String? = null,
  @Json(name = "groundingMetadata") val groundingMetadata: GeminiGroundingMetadata? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentResponse(
  @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null,
)

// Imagen 3 Predict Request & Response Models
@JsonClass(generateAdapter = true)
data class ImagenInstance(
  @Json(name = "prompt") val prompt: String,
)

@JsonClass(generateAdapter = true)
data class ImagenParameters(
  @Json(name = "sampleCount") val sampleCount: Int = 1,
  @Json(name = "aspectRatio") val aspectRatio: String = "1:1",
  @Json(name = "outputMimeType") val outputMimeType: String = "image/jpeg",
)

@JsonClass(generateAdapter = true)
data class ImagenPredictRequest(
  @Json(name = "instances") val instances: List<ImagenInstance>,
  @Json(name = "parameters") val parameters: ImagenParameters? = null,
)

@JsonClass(generateAdapter = true)
data class ImagenPrediction(
  @Json(name = "bytesBase64Encoded") val bytesBase64Encoded: String? = null,
  @Json(name = "mimeType") val mimeType: String? = null,
)

@JsonClass(generateAdapter = true)
data class ImagenPredictResponse(
  @Json(name = "predictions") val predictions: List<ImagenPrediction>? = null,
)

