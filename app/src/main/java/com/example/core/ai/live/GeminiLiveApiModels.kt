package com.example.core.ai.live

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LivePrebuiltVoiceConfig(
  @Json(name = "voiceName") val voiceName: String = "Puck",
)

@JsonClass(generateAdapter = true)
data class LiveVoiceConfig(
  @Json(name = "prebuiltVoiceConfig") val prebuiltVoiceConfig: LivePrebuiltVoiceConfig = LivePrebuiltVoiceConfig(),
)

@JsonClass(generateAdapter = true)
data class LiveSpeechConfig(
  @Json(name = "voiceConfig") val voiceConfig: LiveVoiceConfig = LiveVoiceConfig(),
)

@JsonClass(generateAdapter = true)
data class LiveGenerationConfig(
  @Json(name = "responseModalities") val responseModalities: List<String> = listOf("AUDIO"),
  @Json(name = "speechConfig") val speechConfig: LiveSpeechConfig? = LiveSpeechConfig(),
  @Json(name = "temperature") val temperature: Float? = 0.7f,
)

@JsonClass(generateAdapter = true)
data class LivePart(
  @Json(name = "text") val text: String? = null,
  @Json(name = "inlineData") val inlineData: LiveBlob? = null,
)

@JsonClass(generateAdapter = true)
data class LiveContent(
  @Json(name = "role") val role: String? = null,
  @Json(name = "parts") val parts: List<LivePart> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class LiveBlob(
  @Json(name = "mimeType") val mimeType: String,
  @Json(name = "data") val data: String, // Base64 PCM
)

@JsonClass(generateAdapter = true)
data class LiveSetupMessage(
  @Json(name = "model") val model: String,
  @Json(name = "generationConfig") val generationConfig: LiveGenerationConfig? = LiveGenerationConfig(),
  @Json(name = "systemInstruction") val systemInstruction: LiveContent? = null,
)

@JsonClass(generateAdapter = true)
data class LiveRealtimeInput(
  @Json(name = "mediaChunks") val mediaChunks: List<LiveBlob> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class LiveClientContent(
  @Json(name = "turns") val turns: List<LiveContent> = emptyList(),
  @Json(name = "turnComplete") val turnComplete: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class LiveClientMessage(
  @Json(name = "setup") val setup: LiveSetupMessage? = null,
  @Json(name = "realtimeInput") val realtimeInput: LiveRealtimeInput? = null,
  @Json(name = "clientContent") val clientContent: LiveClientContent? = null,
)

@JsonClass(generateAdapter = true)
data class LiveServerContent(
  @Json(name = "modelTurn") val modelTurn: LiveContent? = null,
  @Json(name = "turnComplete") val turnComplete: Boolean? = null,
  @Json(name = "interrupted") val interrupted: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class LiveServerMessage(
  @Json(name = "setupComplete") val setupComplete: Map<String, Any>? = null,
  @Json(name = "serverContent") val serverContent: LiveServerContent? = null,
)
