package com.example.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.auth.AuthService
import com.example.core.auth.AuthState
import com.example.data.repository.UserRepository
import com.example.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class SplashDestination {
  data object Loading : SplashDestination()
  data class Navigate(val route: String) : SplashDestination()
}

class SplashViewModel(
  private val userRepository: UserRepository,
  private val authService: AuthService,
) : ViewModel() {

  private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
  val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

  init {
    determineStartupRoute()
  }

  fun determineStartupRoute() {
    viewModelScope.launch {
      val isOnboardingDone = userRepository.isOnboardingCompleted().first()

      if (!isOnboardingDone) {
        _destination.value = SplashDestination.Navigate(Screen.Onboarding.route)
        return@launch
      }

      val currentFirebaseUser = authService.getCurrentUser()
      val currentUserProfile = userRepository.getCurrentUser().first()

      if (currentFirebaseUser != null) {
        userRepository.onUserAuthenticated(currentFirebaseUser)
        _destination.value = SplashDestination.Navigate(Screen.Home.route)
      } else if (currentUserProfile.isLocalGuest && currentUserProfile.id.isNotBlank()) {
        _destination.value = SplashDestination.Navigate(Screen.Home.route)
      } else {
        _destination.value = SplashDestination.Navigate(Screen.Auth.route)
      }
    }
  }
}
