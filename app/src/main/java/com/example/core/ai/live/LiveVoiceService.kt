package com.example.core.ai.live

import com.example.core.result.AppResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Service abstraction for real-time bidirectional voice conversation.
 * Decouples the UI layer from specific voice/realtime AI providers.
 */
interface LiveVoiceService {
  /**
   * Current lifecycle state of the real-time live session.
   */
  val sessionState: StateFlow<LiveSessionState>

  /**
   * Live streaming transcript items (both user utterances and AI responses).
   */
  val transcript: StateFlow<List<LiveTranscriptItem>>

  /**
   * Whether the user's microphone is locally muted during the session.
   */
  val isMicrophoneMuted: StateFlow<Boolean>

  /**
   * Initiates a bidirectional real-time audio session.
   */
  suspend fun startSession(
    userId: String,
    voiceName: String = AvailableLiveVoices.DEFAULT.id,
    systemInstruction: String? = null,
  ): AppResult<Unit>

  /**
   * Gracefully terminates the live session and releases audio hardware.
   */
  suspend fun stopSession(): AppResult<Unit>

  /**
   * Toggles microphone audio transmission.
   */
  fun setMicrophoneMuted(muted: Boolean)

  /**
   * Checks whether the underlying voice engine and credentials are valid.
   */
  fun isVoiceEngineAvailable(): Boolean

  /**
   * Resets the session state back to Idle cleanly.
   */
  fun resetStateToIdle()
}
