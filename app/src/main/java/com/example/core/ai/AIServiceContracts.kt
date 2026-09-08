package com.example.core.ai

import com.example.core.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Service contracts for future AI capabilities in KASA AI.
 * 
 * In Phase 0, these interfaces define the clean architectural boundaries so that future
 * provider integrations (Gemini, Firebase AI, Realtime Speech, Ghanaian language TTS/STT)
 * can be plugged in without requiring rewrites of the presentation or repository layers.
 */

enum class SupportedLanguage(val code: String, val displayName: String, val nativeName: String) {
  ENGLISH("en", "English", "English"),
  TWI("tw", "Akan / Twi", "Twi"),
  GA("ga", "Ga", "Gã"),
  EWE("ee", "Ewe", "Èʋegbe"),
  FANTE("fat", "Fante", "Mfantse"),
  DAGBANI("dag", "Dagbani", "Dagbanli"),
  HAUSA("ha", "Hausa", "Hausa"),
}

data class ChatTurn(
  val role: String, // "user" or "model"
  val text: String,
)

data class GenerationPrompt(
  val userPrompt: String,
  val history: List<ChatTurn> = emptyList(),
  val systemInstruction: String? = null,
  val language: SupportedLanguage = SupportedLanguage.ENGLISH,
  val attachments: List<AttachmentReference> = emptyList(),
)

data class AttachmentReference(
  val id: String,
  val mimeType: String,
  val uri: String,
  val sizeBytes: Long,
)

data class AIResponseMessage(
  val id: String,
  val content: String,
  val timestamp: Long,
  val finishReason: String? = null,
)

interface ConversationalAIService {
  suspend fun generateResponse(
    userId: String,
    prompt: GenerationPrompt,
  ): AppResult<AIResponseMessage>

  fun streamResponse(
    userId: String,
    prompt: GenerationPrompt,
  ): Flow<AppResult<String>>
}

interface VoiceAIService {
  fun isVoiceEngineAvailable(): Boolean
  suspend fun startRealtimeSession(userId: String, language: SupportedLanguage): AppResult<Unit>
  suspend fun stopRealtimeSession(): AppResult<Unit>
}

interface SpeechRecognitionService {
  suspend fun transcribeAudio(
    audioData: ByteArray,
    languageHint: SupportedLanguage,
  ): AppResult<String>
}

interface TextToSpeechService {
  suspend fun synthesizeSpeech(
    text: String,
    language: SupportedLanguage,
  ): AppResult<ByteArray>
}

interface MediaGenerationService {
  suspend fun generateImage(
    userId: String,
    prompt: String,
  ): AppResult<String>

  suspend fun generateMusic(
    userId: String,
    prompt: String,
  ): AppResult<String>
}

interface VisionAnalysisService {
  suspend fun analyzeImage(
    userId: String,
    imageBytes: ByteArray,
    prompt: String,
  ): AppResult<String>
}
