package com.example.ui.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.live.AvailableLiveVoices
import com.example.core.ai.live.GeminiLiveVoiceService
import com.example.core.ai.live.LiveSessionState
import com.example.core.ai.live.LiveTranscriptItem
import com.example.core.ai.live.LiveVoiceOption
import com.example.core.ai.live.LiveVoiceService
import com.example.core.result.AppResult
import com.example.data.model.ConversationalTone
import com.example.data.model.MessageDeliveryStatus
import com.example.data.model.memory.MemoryItem
import com.example.data.repository.ConversationRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LiveViewModel(
  private val userRepository: UserRepository,
  private val conversationRepository: ConversationRepository,
  private val memoryRepository: MemoryRepository? = null,
  private val liveVoiceService: LiveVoiceService = GeminiLiveVoiceService(),
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    LiveUiState(
      isAiEngineAvailable = liveVoiceService.isVoiceEngineAvailable(),
    )
  )
  val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

  private var activeLiveConversationId: String? = null

  init {
    // Observe Live Voice Service State
    viewModelScope.launch {
      liveVoiceService.sessionState.collectLatest { state ->
        _uiState.update { current ->
          current.copy(
            sessionState = state,
            errorMessage = if (state is LiveSessionState.Error) state.message else current.errorMessage,
          )
        }
      }
    }

    // Observe Live Voice Transcript and persist turns to Room database
    viewModelScope.launch {
      liveVoiceService.transcript.collectLatest { items ->
        _uiState.update { it.copy(transcript = items) }
        persistTranscriptItems(items)
      }
    }

    // Observe Microphone Mute state
    viewModelScope.launch {
      liveVoiceService.isMicrophoneMuted.collectLatest { muted ->
        _uiState.update { it.copy(isMuted = muted) }
      }
    }
  }

  fun updateMicrophonePermission(granted: Boolean) {
    _uiState.update {
      it.copy(
        hasMicrophonePermission = granted,
        showPermissionRationale = !granted,
      )
    }
  }

  fun startLiveSession() {
    viewModelScope.launch {
      val currentUser = try {
        userRepository.getCurrentUser().first()
      } catch (e: Exception) {
        null
      }
      val userId = currentUser?.id ?: "guest_user"

      // Create or locate a dedicated Live Session conversation in Room database
      val timeFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
      val title = "KASA Live • ${timeFormat.format(Date())}"
      
      when (val convResult = conversationRepository.createConversation(userId, title)) {
        is AppResult.Success -> {
          activeLiveConversationId = convResult.data.id
          _uiState.update { it.copy(activeConversationId = convResult.data.id, errorMessage = null) }
        }
        else -> {
          // If conversation creation fails, fallback to temporary session
          activeLiveConversationId = null
        }
      }

      val userSettings = try {
        userRepository.getUserSettings().first()
      } catch (e: Exception) {
        null
      }
      val preferredLanguage = userSettings?.preferredLanguage ?: com.example.core.ai.SupportedLanguage.ENGLISH
      val tone = userSettings?.conversationalTone ?: ConversationalTone.FRIENDLY
      val activeMemories = if (userSettings?.memoryEnabled != false && memoryRepository != null) {
        val res = memoryRepository.getActiveMemories(userId)
        if (res is AppResult.Success) res.data else emptyList()
      } else {
        emptyList()
      }

      val dynamicLiveInstruction = com.example.core.ai.context.GhanaianContextConfig.buildLiveSystemInstruction(
        preferredLanguage = preferredLanguage,
        conversationalTone = tone,
        memories = activeMemories,
        memoryEnabled = userSettings?.memoryEnabled ?: true,
      )

      val result = liveVoiceService.startSession(
        userId = userId,
        voiceName = _uiState.value.selectedVoice.id,
        systemInstruction = dynamicLiveInstruction,
      )

      if (result is AppResult.Error) {
        _uiState.update {
          it.copy(
            errorMessage = result.error.message,
            sessionState = LiveSessionState.Error(result.error.message),
          )
        }
      }
    }
  }

  fun stopLiveSession() {
    viewModelScope.launch {
      liveVoiceService.stopSession()
    }
  }

  fun toggleMute() {
    val newMuted = !_uiState.value.isMuted
    liveVoiceService.setMicrophoneMuted(newMuted)
  }

  fun selectVoice(voice: LiveVoiceOption) {
    _uiState.update {
      it.copy(
        selectedVoice = voice,
        showVoiceSelectionSheet = false,
      )
    }
  }

  fun setVoiceSelectionSheetVisible(visible: Boolean) {
    _uiState.update { it.copy(showVoiceSelectionSheet = visible) }
  }

  fun setPermissionRationaleVisible(visible: Boolean) {
    _uiState.update { it.copy(showPermissionRationale = visible) }
  }

  fun dismissError() {
    _uiState.update { it.copy(errorMessage = null) }
    liveVoiceService.resetStateToIdle()
  }

  private fun persistTranscriptItems(items: List<LiveTranscriptItem>) {
    val convId = activeLiveConversationId ?: return

    viewModelScope.launch {
      val currentUser = try {
        userRepository.getCurrentUser().first()
      } catch (e: Exception) {
        null
      }
      val userId = currentUser?.id ?: "guest_user"

      for (item in items) {
        if (item.text.isNotBlank()) {
          if (item.isUser) {
            conversationRepository.appendUserMessage(
              userId = userId,
              conversationId = convId,
              content = item.text,
            )
          } else {
            conversationRepository.appendAssistantMessage(
              userId = userId,
              conversationId = convId,
              content = item.text,
              status = MessageDeliveryStatus.DELIVERED_LOCAL,
            )
          }
        }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    viewModelScope.launch {
      liveVoiceService.stopSession()
    }
  }
}

