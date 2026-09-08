package com.example.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.AppResult
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
  val currentPage: Int = 0,
  val totalPages: Int = 4,
  val isCompleted: Boolean = false,
)

class OnboardingViewModel(
  private val userRepository: UserRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(OnboardingUiState())
  val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

  fun onPageChanged(page: Int) {
    _uiState.value = _uiState.value.copy(currentPage = page)
  }

  fun completeOnboarding(onFinish: () -> Unit) {
    viewModelScope.launch {
      userRepository.setOnboardingCompleted(true)
      _uiState.value = _uiState.value.copy(isCompleted = true)
      onFinish()
    }
  }
}
