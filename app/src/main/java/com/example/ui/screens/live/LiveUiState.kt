package com.example.ui.screens.live

import com.example.core.ai.live.AvailableLiveVoices
import com.example.core.ai.live.LiveSessionState
import com.example.core.ai.live.LiveTranscriptItem
import com.example.core.ai.live.LiveVoiceOption

data class LiveUiState(
  val sessionState: LiveSessionState = LiveSessionState.Idle,
  val transcript: List<LiveTranscriptItem> = emptyList(),
  val selectedVoice: LiveVoiceOption = AvailableLiveVoices.DEFAULT,
  val availableVoices: List<LiveVoiceOption> = AvailableLiveVoices.ALL,
  val isMuted: Boolean = false,
  val hasMicrophonePermission: Boolean = false,
  val showVoiceSelectionSheet: Boolean = false,
  val showPermissionRationale: Boolean = false,
  val activeConversationId: String? = null,
  val errorMessage: String? = null,
  val isAiEngineAvailable: Boolean = false,
)
