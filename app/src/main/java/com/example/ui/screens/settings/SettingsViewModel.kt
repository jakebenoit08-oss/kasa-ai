package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.SupportedLanguage
import com.example.core.auth.AuthActionResult
import com.example.core.auth.AuthService
import com.example.core.config.AppConfig
import com.example.core.result.AppResult
import com.example.data.model.ConversationalTone
import com.example.data.model.LearningStyle
import com.example.data.model.ResponseLength
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.example.data.model.memory.MemoryItem
import com.example.data.repository.ConversationRepository
import com.example.data.repository.GeneratedImageRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.StudyRepository
import com.example.data.repository.UserRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class SettingsUiState(
  val user: UserProfile? = null,
  val settings: UserSettings? = null,
  val memories: List<MemoryItem> = emptyList(),
  val showEditNameDialog: Boolean = false,
  val showLanguageDialog: Boolean = false,
  val showThemeDialog: Boolean = false,
  val showResponseLengthDialog: Boolean = false,
  val showConversationalToneDialog: Boolean = false,
  val showLearningStyleDialog: Boolean = false,
  val showManageMemoriesDialog: Boolean = false,
  val showClearAllMemoriesDialog: Boolean = false,
  val showAddMemoryDialog: Boolean = false,
  val showSecurityAuditDialog: Boolean = false,
  val showPrivacyInfoDialog: Boolean = false,
  val showNotificationsInfoDialog: Boolean = false,
  val showSignOutDialog: Boolean = false,
  val showDeleteAccountDialog: Boolean = false,
  val isBusy: Boolean = false,
  val statusMessage: String? = null,
  val isGeminiConfigured: Boolean = false,
)

class SettingsViewModel(
  private val userRepository: UserRepository,
  private val memoryRepository: MemoryRepository? = null,
  private val authService: AuthService? = null,
  private val conversationRepository: ConversationRepository? = null,
  private val studyRepository: StudyRepository? = null,
  private val imageRepository: GeneratedImageRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  init {
    loadSettings()
    observeMemories()
  }

  private fun loadSettings() {
    viewModelScope.launch {
      combine(
        userRepository.getCurrentUser(),
        userRepository.getUserSettings(),
      ) { user, settings ->
        _uiState.value = _uiState.value.copy(
          user = user,
          settings = settings,
          isGeminiConfigured = AppConfig.isGeminiConfigured(),
        )
      }.collect {}
    }
  }

  private fun observeMemories() {
    if (memoryRepository == null) return
    viewModelScope.launch {
      userRepository.getCurrentUser().flatMapLatest { user ->
        memoryRepository.getMemories(user.id)
      }.collect { memoryList ->
        _uiState.value = _uiState.value.copy(memories = memoryList)
      }
    }
  }

  fun updateDisplayName(newName: String) {
    viewModelScope.launch {
      val result = userRepository.updateDisplayName(newName)
      if (result is AppResult.Success) {
        authService?.updateDisplayName(newName)
        _uiState.value = _uiState.value.copy(
          showEditNameDialog = false,
          statusMessage = "Profile name updated.",
        )
      }
    }
  }

  fun updateThemeMode(themeMode: ThemeMode) {
    viewModelScope.launch {
      userRepository.updateThemeMode(themeMode)
      _uiState.value = _uiState.value.copy(showThemeDialog = false)
    }
  }

  fun updateLanguage(language: SupportedLanguage) {
    viewModelScope.launch {
      userRepository.updatePreferredLanguage(language)
      _uiState.value = _uiState.value.copy(showLanguageDialog = false)
    }
  }

  fun updateResponseLength(length: ResponseLength) {
    viewModelScope.launch {
      userRepository.updateResponseLength(length)
      _uiState.value = _uiState.value.copy(showResponseLengthDialog = false)
    }
  }

  fun updateConversationalTone(tone: ConversationalTone) {
    viewModelScope.launch {
      userRepository.updateConversationalTone(tone)
      _uiState.value = _uiState.value.copy(showConversationalToneDialog = false)
    }
  }

  fun updateLearningStyle(style: LearningStyle) {
    viewModelScope.launch {
      userRepository.updateLearningStyle(style)
      _uiState.value = _uiState.value.copy(showLearningStyleDialog = false)
    }
  }

  fun toggleMemory(enabled: Boolean) {
    viewModelScope.launch {
      userRepository.updateMemoryEnabled(enabled)
      val msg = if (enabled) "KASA Memory enabled." else "KASA Memory paused."
      _uiState.value = _uiState.value.copy(statusMessage = msg)
    }
  }

  fun addManualMemory(text: String) {
    val clean = text.trim()
    if (clean.isBlank()) return
    val user = _uiState.value.user ?: return
    viewModelScope.launch {
      val res = memoryRepository?.saveMemory(user.id, clean)
      if (res is AppResult.Success) {
        _uiState.value = _uiState.value.copy(
          showAddMemoryDialog = false,
          statusMessage = "Memory added.",
        )
      } else if (res is AppResult.Error) {
        _uiState.value = _uiState.value.copy(
          statusMessage = res.error.message,
        )
      }
    }
  }

  fun deleteMemory(memoryId: String) {
    val user = _uiState.value.user ?: return
    viewModelScope.launch {
      val res = memoryRepository?.deleteMemory(user.id, memoryId)
      if (res is AppResult.Success) {
        _uiState.value = _uiState.value.copy(statusMessage = "Memory deleted.")
      }
    }
  }

  fun clearAllMemories() {
    val user = _uiState.value.user ?: return
    viewModelScope.launch {
      val res = memoryRepository?.deleteAllMemories(user.id)
      if (res is AppResult.Success) {
        _uiState.value = _uiState.value.copy(
          showClearAllMemoriesDialog = false,
          statusMessage = "All saved memories deleted.",
        )
      }
    }
  }

  fun toggleHapticFeedback(enabled: Boolean) {
    viewModelScope.launch {
      userRepository.updateHapticFeedback(enabled)
    }
  }

  fun signOut(onSignedOut: () -> Unit) {
    _uiState.value = _uiState.value.copy(isBusy = true)
    viewModelScope.launch {
      authService?.signOut()
      userRepository.onUserSignedOut()
      _uiState.value = _uiState.value.copy(
        isBusy = false,
        showSignOutDialog = false
      )
      onSignedOut()
    }
  }

  fun deleteAccount(onDeleted: () -> Unit) {
    val user = _uiState.value.user ?: return
    _uiState.value = _uiState.value.copy(isBusy = true)

    viewModelScope.launch {
      // 1. Wipe local Room partitions for this user
      val userId = user.id
      conversationRepository?.deleteAllConversationsForUser(userId)
      memoryRepository?.deleteAllMemories(userId)
      studyRepository?.deleteSessionsForUser(userId)
      imageRepository?.deleteHistoryForUser(userId)
      userRepository.deleteUserData(userId)

      // 2. Delete Firebase account if authenticated
      if (!user.isLocalGuest && authService != null) {
        val authResult = authService.deleteAccount()
        if (authResult is AuthActionResult.Error) {
          _uiState.value = _uiState.value.copy(
            isBusy = false,
            showDeleteAccountDialog = false,
            statusMessage = authResult.message
          )
          return@launch
        }
      }

      _uiState.value = _uiState.value.copy(
        isBusy = false,
        showDeleteAccountDialog = false
      )
      onDeleted()
    }
  }

  fun setEditNameDialogVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showEditNameDialog = visible)
  }

  fun setThemeDialogVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showThemeDialog = visible)
  }

  fun setLanguageDialogVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showLanguageDialog = visible)
  }

  fun setResponseLengthDialogVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showResponseLengthDialog = visible)
  }

  fun setConversationalToneDialogVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showConversationalToneDialog = visible)
  }

  fun setLearningStyleDialogVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showLearningStyleDialog = visible)
  }

  fun setManageMemoriesDialogVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showManageMemoriesDialog = visible)
  }

  fun setClearAllMemoriesDialogVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showClearAllMemoriesDialog = visible)
  }

  fun setAddMemoryDialogVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showAddMemoryDialog = visible)
  }

  fun setSecurityAuditDialogVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showSecurityAuditDialog = visible)
  }

  fun setPrivacyInfoVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showPrivacyInfoDialog = visible)
  }

  fun setNotificationsInfoVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showNotificationsInfoDialog = visible)
  }

  fun setSignOutDialogVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showSignOutDialog = visible)
  }

  fun setDeleteAccountDialogVisible(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showDeleteAccountDialog = visible)
  }

  fun dismissStatusMessage() {
    _uiState.value = _uiState.value.copy(statusMessage = null)
  }
}
