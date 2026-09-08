package com.example.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.ChatTurn
import com.example.core.ai.ConversationalAIService
import com.example.core.ai.GenerationPrompt
import com.example.core.ai.gemini.GeminiConversationalAIService
import com.example.core.ai.memory.MemoryDetectionResult
import com.example.core.ai.memory.MemoryIntentDetector
import com.example.core.ai.search.GeminiSearchGroundingService
import com.example.core.ai.search.SearchIntentDetector
import com.example.core.ai.search.SearchService
import com.example.core.config.AppConfig
import com.example.core.result.AppResult
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MessageDeliveryStatus
import com.example.data.model.MessageSender
import com.example.data.model.memory.MemoryItem
import com.example.data.repository.ConversationRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ChatUiState(
  val currentUserId: String = "",
  val activeConversation: Conversation? = null,
  val conversations: List<Conversation> = emptyList(),
  val messages: List<ChatMessage> = emptyList(),
  val inputText: String = "",
  val isGenerating: Boolean = false,
  val isSearchModeEnabled: Boolean = false,
  val isSearchingWeb: Boolean = false,
  val searchStatusText: String? = null,
  val streamingMessageId: String? = null,
  val streamingContent: String = "",
  val errorMessage: String? = null,
  val failedPromptForRetry: String? = null,
  val isAiEngineConnected: Boolean = false,
  val showRenameDialog: Boolean = false,
  val renameTargetConversation: Conversation? = null,
  val showDeleteConfirmDialog: Boolean = false,
  val deleteTargetConversation: Conversation? = null,
  val showConversationsDrawer: Boolean = false,
  val pendingMemoryCandidate: String? = null,
  val memoryFeedbackMessage: String? = null,
)

class ChatViewModel(
  private val userRepository: UserRepository,
  private val conversationRepository: ConversationRepository,
  private val memoryRepository: MemoryRepository? = null,
  private val aiService: ConversationalAIService = GeminiConversationalAIService(),
  private val searchService: SearchService = GeminiSearchGroundingService(),
) : ViewModel() {

  private val _uiState = MutableStateFlow(ChatUiState())
  val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

  private var activeMessageJob: Job? = null
  private var messagesObservationJob: Job? = null

  init {
    initializeSession()
  }

  private fun initializeSession() {
    viewModelScope.launch {
      val user = userRepository.getCurrentUser().first()
      val isConnected = AppConfig.isGeminiConfigured()
      _uiState.value = _uiState.value.copy(
        currentUserId = user.id,
        isAiEngineConnected = isConnected,
      )

      // Observe conversations for this specific user (enforcing per-user partition)
      conversationRepository.getConversations(user.id).collect { conversations ->
        val currentActive = _uiState.value.activeConversation
        val newActive = if (currentActive != null && conversations.any { it.id == currentActive.id }) {
          conversations.first { it.id == currentActive.id }
        } else {
          conversations.firstOrNull()
        }

        _uiState.value = _uiState.value.copy(
          conversations = conversations,
          activeConversation = newActive,
        )

        if (newActive != null) {
          observeMessages(user.id, newActive.id)
        } else {
          _uiState.value = _uiState.value.copy(messages = emptyList())
        }
      }
    }
  }

  private fun observeMessages(userId: String, conversationId: String) {
    messagesObservationJob?.cancel()
    messagesObservationJob = viewModelScope.launch {
      conversationRepository.getMessages(userId, conversationId).collect { msgs ->
        _uiState.value = _uiState.value.copy(messages = msgs)
      }
    }
  }

  fun onInputTextChanged(newText: String) {
    _uiState.value = _uiState.value.copy(inputText = newText)
  }

  fun sendMessage(explicitText: String? = null) {
    val textToSend = (explicitText ?: _uiState.value.inputText).trim()
    if (textToSend.isBlank() || _uiState.value.isGenerating) return

    activeMessageJob?.cancel()
    activeMessageJob = viewModelScope.launch {
      val userId = _uiState.value.currentUserId
      var activeConv = _uiState.value.activeConversation

      // Check for explicit memory intent in the message
      val memoryIntent = MemoryIntentDetector.scanForMemoryIntent(textToSend)
      when (memoryIntent) {
        is MemoryDetectionResult.RejectedSensitive -> {
          _uiState.value = _uiState.value.copy(
            memoryFeedbackMessage = memoryIntent.reason
          )
        }
        is MemoryDetectionResult.Candidate -> {
          val userSettings = try { userRepository.getUserSettings().first() } catch (_: Exception) { null }
          if (userSettings?.memoryEnabled != false) {
            _uiState.value = _uiState.value.copy(
              pendingMemoryCandidate = memoryIntent.cleanSnippet
            )
          }
        }
        is MemoryDetectionResult.None -> Unit
      }

      // Clear input and previous error
      _uiState.value = _uiState.value.copy(
        inputText = "",
        errorMessage = null,
        failedPromptForRetry = null,
        isGenerating = true,
        streamingContent = "",
      )

      // Create conversation if none active
      if (activeConv == null) {
        val titleSnippet = if (textToSend.length > 28) textToSend.take(28) + "..." else textToSend
        val createResult = conversationRepository.createConversation(userId, titleSnippet)
        if (createResult is AppResult.Success) {
          activeConv = createResult.data
          _uiState.value = _uiState.value.copy(activeConversation = activeConv)
          observeMessages(userId, activeConv.id)
        } else {
          _uiState.value = _uiState.value.copy(
            isGenerating = false,
            errorMessage = "Could not initialize conversation session.",
            failedPromptForRetry = textToSend,
          )
          return@launch
        }
      }

      val conversationId = activeConv.id

      // 1. Append User Message
      val userMsgResult = conversationRepository.appendUserMessage(userId, conversationId, textToSend)
      if (userMsgResult is AppResult.Error) {
        _uiState.value = _uiState.value.copy(
          isGenerating = false,
          errorMessage = userMsgResult.error.message,
          failedPromptForRetry = textToSend,
        )
        return@launch
      }

      // 2. Fetch multi-turn history from database
      val allHistory = conversationRepository.getMessagesList(userId, conversationId)
      val historyTurns = allHistory.dropLast(1).mapNotNull { msg ->
        when (msg.sender) {
          MessageSender.USER -> ChatTurn("user", msg.content)
          MessageSender.ASSISTANT -> if (msg.deliveryStatus == MessageDeliveryStatus.DELIVERED_LOCAL) {
            ChatTurn("model", msg.content)
          } else null
          MessageSender.SYSTEM -> null
        }
      }

      val isWebSearchRequested = SearchIntentDetector.shouldTriggerSearch(textToSend, _uiState.value.isSearchModeEnabled)

      // 3. Create Placeholder Assistant Message
      val placeholderResult = conversationRepository.appendAssistantMessage(
        userId = userId,
        conversationId = conversationId,
        content = "",
        status = MessageDeliveryStatus.SENDING,
        isWebSearch = isWebSearchRequested,
      )

      val assistantMsgId = if (placeholderResult is AppResult.Success) {
        placeholderResult.data.id
      } else {
        null
      }

      _uiState.value = _uiState.value.copy(
        streamingMessageId = assistantMsgId,
        streamingContent = "",
        isSearchingWeb = isWebSearchRequested,
        searchStatusText = if (isWebSearchRequested) "Searching the web..." else null,
      )

      // 4. Fetch User Settings & Active Memories for Personalization
      val userSettings = try {
        userRepository.getUserSettings().first()
      } catch (e: Exception) {
        null
      }

      val activeMemories: List<MemoryItem> = if (userSettings?.memoryEnabled != false && memoryRepository != null) {
        val memResult = memoryRepository.getActiveMemories(userId)
        if (memResult is AppResult.Success) memResult.data else emptyList()
      } else {
        emptyList()
      }

      val preferredLanguage = userSettings?.preferredLanguage ?: com.example.core.ai.SupportedLanguage.ENGLISH

      if (isWebSearchRequested) {
        // Execute Real Web Search Grounding
        val dynamicSearchSystemInstruction = com.example.core.ai.context.GhanaianContextConfig.buildSearchSystemInstruction(
          settings = userSettings,
          memories = activeMemories,
        )

        val cleanQuery = SearchIntentDetector.sanitizeSearchQuery(textToSend)

        val searchResult = searchService.searchAndGenerate(
          userId = userId,
          query = cleanQuery,
          history = historyTurns,
          systemInstruction = dynamicSearchSystemInstruction,
          language = preferredLanguage,
        )

        when (searchResult) {
          is AppResult.Success -> {
            val responseData = searchResult.data
            if (assistantMsgId != null) {
              conversationRepository.updateMessageContent(
                userId = userId,
                messageId = assistantMsgId,
                content = responseData.content,
                status = MessageDeliveryStatus.DELIVERED_LOCAL,
                isWebSearch = true,
                searchSources = responseData.sources,
              )
            }
            _uiState.value = _uiState.value.copy(
              isGenerating = false,
              isSearchingWeb = false,
              searchStatusText = null,
              streamingMessageId = null,
              streamingContent = "",
              errorMessage = null,
              failedPromptForRetry = null,
            )
          }
          is AppResult.Error -> {
            val displayError = searchResult.error.message
            if (assistantMsgId != null) {
              conversationRepository.updateMessageContent(
                userId = userId,
                messageId = assistantMsgId,
                content = "Sorry, I was unable to complete the web search. $displayError",
                status = MessageDeliveryStatus.FAILED,
                isWebSearch = true,
                searchSources = emptyList(),
              )
            }
            _uiState.value = _uiState.value.copy(
              isGenerating = false,
              isSearchingWeb = false,
              searchStatusText = null,
              streamingMessageId = null,
              streamingContent = "",
              errorMessage = displayError,
              failedPromptForRetry = textToSend,
            )
          }
          is AppResult.Loading -> Unit
        }
      } else {
        // Standard Conversational Streaming Flow
        val dynamicSystemInstruction = com.example.core.ai.context.GhanaianContextConfig.buildSystemInstruction(
          settings = userSettings,
          memories = activeMemories,
        )

        val prompt = GenerationPrompt(
          userPrompt = textToSend,
          history = historyTurns,
          systemInstruction = dynamicSystemInstruction,
          language = preferredLanguage,
        )

        val responseAccumulator = StringBuilder()
        var streamError: String? = null

        try {
          aiService.streamResponse(userId, prompt).collect { chunkResult ->
            when (chunkResult) {
              is AppResult.Success -> {
                responseAccumulator.append(chunkResult.data)
                val currentAccumulated = responseAccumulator.toString()
                _uiState.value = _uiState.value.copy(
                  streamingContent = currentAccumulated
                )
                if (assistantMsgId != null) {
                  conversationRepository.updateMessageContent(
                    userId = userId,
                    messageId = assistantMsgId,
                    content = currentAccumulated,
                    status = MessageDeliveryStatus.SENDING,
                    isWebSearch = false,
                  )
                }
              }
              is AppResult.Error -> {
                streamError = chunkResult.error.message
              }
              is AppResult.Loading -> Unit
            }
          }
        } catch (e: Exception) {
          streamError = e.localizedMessage ?: "Unexpected error during AI generation."
        }

        val finalContent = responseAccumulator.toString().trim()

        if (streamError != null || finalContent.isEmpty()) {
          val displayError = streamError ?: "Failed to generate AI response. Please retry."
          if (assistantMsgId != null) {
            conversationRepository.updateMessageContent(
              userId = userId,
              messageId = assistantMsgId,
              content = "Sorry, I was unable to complete your request. $displayError",
              status = MessageDeliveryStatus.FAILED,
              isWebSearch = false,
            )
          }
          _uiState.value = _uiState.value.copy(
            isGenerating = false,
            streamingMessageId = null,
            streamingContent = "",
            errorMessage = displayError,
            failedPromptForRetry = textToSend,
          )
        } else {
          if (assistantMsgId != null) {
            conversationRepository.updateMessageContent(
              userId = userId,
              messageId = assistantMsgId,
              content = finalContent,
              status = MessageDeliveryStatus.DELIVERED_LOCAL,
              isWebSearch = false,
            )
          }
          _uiState.value = _uiState.value.copy(
            isGenerating = false,
            streamingMessageId = null,
            streamingContent = "",
            errorMessage = null,
            failedPromptForRetry = null,
          )
        }
      }
    }
  }

  fun confirmSaveMemory(candidateText: String? = null) {
    val textToSave = candidateText ?: _uiState.value.pendingMemoryCandidate ?: return
    val userId = _uiState.value.currentUserId
    viewModelScope.launch {
      if (memoryRepository != null) {
        val result = memoryRepository.saveMemory(userId, textToSave)
        if (result is AppResult.Success) {
          _uiState.value = _uiState.value.copy(
            pendingMemoryCandidate = null,
            memoryFeedbackMessage = "Saved to KASA Memory",
          )
        } else if (result is AppResult.Error) {
          _uiState.value = _uiState.value.copy(
            pendingMemoryCandidate = null,
            memoryFeedbackMessage = "Failed to save memory: ${result.error.message}",
          )
        }
      } else {
        _uiState.value = _uiState.value.copy(
          pendingMemoryCandidate = null,
          memoryFeedbackMessage = "Memory repository unavailable",
        )
      }
    }
  }

  fun dismissPendingMemory() {
    _uiState.value = _uiState.value.copy(pendingMemoryCandidate = null)
  }

  fun dismissMemoryFeedback() {
    _uiState.value = _uiState.value.copy(memoryFeedbackMessage = null)
  }

  fun retryLastFailedMessage() {
    val promptToRetry = _uiState.value.failedPromptForRetry ?: _uiState.value.messages.lastOrNull { it.sender == MessageSender.USER }?.content
    if (!promptToRetry.isNullOrBlank()) {
      sendMessage(promptToRetry)
    }
  }

  fun startNewConversation() {
    activeMessageJob?.cancel()
    _uiState.value = _uiState.value.copy(
      activeConversation = null,
      messages = emptyList(),
      inputText = "",
      errorMessage = null,
      failedPromptForRetry = null,
      isGenerating = false,
      streamingMessageId = null,
      streamingContent = "",
      showConversationsDrawer = false,
    )
  }

  fun selectConversation(conversation: Conversation) {
    if (_uiState.value.activeConversation?.id == conversation.id) {
      _uiState.value = _uiState.value.copy(showConversationsDrawer = false)
      return
    }
    activeMessageJob?.cancel()
    _uiState.value = _uiState.value.copy(
      activeConversation = conversation,
      errorMessage = null,
      failedPromptForRetry = null,
      isGenerating = false,
      streamingMessageId = null,
      streamingContent = "",
      showConversationsDrawer = false,
    )
    observeMessages(_uiState.value.currentUserId, conversation.id)
  }

  fun openRenameDialog(conversation: Conversation? = null) {
    val target = conversation ?: _uiState.value.activeConversation
    if (target != null) {
      _uiState.value = _uiState.value.copy(
        showRenameDialog = true,
        renameTargetConversation = target,
      )
    }
  }

  fun dismissRenameDialog() {
    _uiState.value = _uiState.value.copy(
      showRenameDialog = false,
      renameTargetConversation = null,
    )
  }

  fun confirmRename(newTitle: String) {
    val target = _uiState.value.renameTargetConversation ?: return
    viewModelScope.launch {
      val res = conversationRepository.renameConversation(
        userId = _uiState.value.currentUserId,
        conversationId = target.id,
        newTitle = newTitle,
      )
      if (res is AppResult.Success) {
        if (_uiState.value.activeConversation?.id == target.id) {
          _uiState.value = _uiState.value.copy(
            activeConversation = _uiState.value.activeConversation?.copy(title = newTitle.trim())
          )
        }
      }
      dismissRenameDialog()
    }
  }

  fun openDeleteDialog(conversation: Conversation? = null) {
    val target = conversation ?: _uiState.value.activeConversation
    if (target != null) {
      _uiState.value = _uiState.value.copy(
        showDeleteConfirmDialog = true,
        deleteTargetConversation = target,
      )
    }
  }

  fun dismissDeleteDialog() {
    _uiState.value = _uiState.value.copy(
      showDeleteConfirmDialog = false,
      deleteTargetConversation = null,
    )
  }

  fun confirmDelete() {
    val target = _uiState.value.deleteTargetConversation ?: return
    viewModelScope.launch {
      conversationRepository.deleteConversation(_uiState.value.currentUserId, target.id)
      if (_uiState.value.activeConversation?.id == target.id) {
        startNewConversation()
      }
      dismissDeleteDialog()
    }
  }

  fun toggleConversationsDrawer() {
    _uiState.value = _uiState.value.copy(
      showConversationsDrawer = !_uiState.value.showConversationsDrawer
    )
  }

  fun setConversationsDrawer(open: Boolean) {
    _uiState.value = _uiState.value.copy(showConversationsDrawer = open)
  }

  fun toggleSearchMode() {
    _uiState.value = _uiState.value.copy(
      isSearchModeEnabled = !_uiState.value.isSearchModeEnabled
    )
  }

  fun setSearchMode(enabled: Boolean) {
    _uiState.value = _uiState.value.copy(
      isSearchModeEnabled = enabled
    )
  }

  fun dismissError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }
}
