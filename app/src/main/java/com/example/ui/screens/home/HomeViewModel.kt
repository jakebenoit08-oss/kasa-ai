package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.example.data.repository.ConversationRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
  val user: UserProfile? = null,
  val greeting: String = "Welcome",
  val recentConversations: List<Conversation> = emptyList(),
  val isConfigured: Boolean = false,
  val isLoading: Boolean = false,
)

class HomeViewModel(
  private val userRepository: UserRepository,
  private val conversationRepository: ConversationRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  init {
    loadHomeData()
  }

  private fun loadHomeData() {
    viewModelScope.launch {
      userRepository.getCurrentUser().collect { user ->
        val greeting = calculateGreeting()
        _uiState.value = _uiState.value.copy(
          user = user,
          greeting = "$greeting, ${user.displayName}",
        )

        // Observe isolated conversations for this user
        conversationRepository.getConversations(user.id).collect { convs ->
          _uiState.value = _uiState.value.copy(
            recentConversations = convs,
          )
        }
      }
    }
  }

  private fun calculateGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
      in 5..11 -> "Good morning"
      in 12..16 -> "Good afternoon"
      in 17..21 -> "Good evening"
      else -> "Welcome back"
    }
  }
}
