package com.example.core.ai.live

sealed interface LiveSessionState {
  data object Idle : LiveSessionState
  data object Connecting : LiveSessionState
  data class Listening(val micAmplitude: Float = 0f) : LiveSessionState
  data object Thinking : LiveSessionState
  data class Speaking(val speakerAmplitude: Float = 0f) : LiveSessionState
  data class Error(val message: String, val canRetry: Boolean = true) : LiveSessionState
  data class Ended(val reason: String? = null) : LiveSessionState
}

data class LiveTranscriptItem(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val timestamp: Long = System.currentTimeMillis(),
  val isFinal: Boolean = true,
)

data class LiveVoiceOption(
  val id: String,
  val displayName: String,
  val description: String,
  val gender: String,
)

object AvailableLiveVoices {
  val DEFAULT = LiveVoiceOption(
    id = "Puck",
    displayName = "Puck",
    description = "Warm, engaging, and balanced tone",
    gender = "Neutral / Warm",
  )

  val ALL = listOf(
    DEFAULT,
    LiveVoiceOption(
      id = "Aoede",
      displayName = "Aoede",
      description = "Clear, friendly, and expressive voice",
      gender = "Female",
    ),
    LiveVoiceOption(
      id = "Charon",
      displayName = "Charon",
      description = "Deep, calm, and grounded tone",
      gender = "Male",
    ),
    LiveVoiceOption(
      id = "Kore",
      displayName = "Kore",
      description = "Gentle, natural, and thoughtful voice",
      gender = "Female",
    ),
    LiveVoiceOption(
      id = "Fenrir",
      displayName = "Fenrir",
      description = "Crisp, direct, and authoritative voice",
      gender = "Male",
    ),
  )
}
