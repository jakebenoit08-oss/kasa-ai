package com.example

import com.example.core.auth.AuthActionResult
import com.example.core.auth.AuthErrorTranslator
import com.example.core.auth.AuthService
import com.example.core.auth.AuthState
import com.example.core.result.AppResult
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.example.ui.screens.auth.AuthViewModel
import com.example.ui.screens.onboarding.OnboardingViewModel
import com.example.ui.screens.splash.SplashDestination
import com.example.ui.screens.splash.SplashViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KasaPhase8AuthOnboardingTest {

  private class FakeAuthService(
    var user: UserProfile? = null
  ) : AuthService {
    private val authStateFlow = MutableStateFlow<AuthState>(
      user?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
    )

    override val authState: Flow<AuthState> = authStateFlow.asStateFlow()

    var signInCalledWith: Pair<String, String>? = null
    var signUpCalledWith: Triple<String, String, String>? = null
    var signOutCalled = false
    var shouldFailAuth = false
    var failMessage: String? = null

    override fun getCurrentUser(): UserProfile? = user

    override fun isFirebaseAvailable(): Boolean = true

    override suspend fun signInWithEmail(email: String, password: String): AuthActionResult<UserProfile> {
      signInCalledWith = Pair(email, password)
      if (shouldFailAuth) {
        return AuthActionResult.Error(failMessage ?: "Invalid email or password")
      }
      val loggedIn = UserProfile(id = "firebase_u123", email = email, displayName = "Kofi Mensah", isLocalGuest = false)
      user = loggedIn
      authStateFlow.value = AuthState.Authenticated(loggedIn)
      return AuthActionResult.Success(loggedIn)
    }

    override suspend fun signUpWithEmail(
      email: String,
      password: String,
      displayName: String?,
    ): AuthActionResult<UserProfile> {
      signUpCalledWith = Triple(email, password, displayName ?: "")
      if (shouldFailAuth) {
        return AuthActionResult.Error(failMessage ?: "User already exists")
      }
      val newUser = UserProfile(id = "firebase_u456", email = email, displayName = displayName ?: "User", isLocalGuest = false)
      user = newUser
      authStateFlow.value = AuthState.Authenticated(newUser)
      return AuthActionResult.Success(newUser)
    }

    override suspend fun signInWithGoogle(idToken: String): AuthActionResult<UserProfile> {
      if (shouldFailAuth) {
        return AuthActionResult.Error(failMessage ?: "Google sign-in failed")
      }
      val googleUser = UserProfile(id = "firebase_g789", email = "google@kasa.ai", displayName = "Google User", isLocalGuest = false)
      user = googleUser
      authStateFlow.value = AuthState.Authenticated(googleUser)
      return AuthActionResult.Success(googleUser)
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthActionResult<Unit> {
      if (shouldFailAuth) {
        return AuthActionResult.Error(failMessage ?: "Reset failed")
      }
      return AuthActionResult.Success(Unit)
    }

    override suspend fun updateDisplayName(newDisplayName: String): AuthActionResult<Unit> {
      user = user?.copy(displayName = newDisplayName)
      return AuthActionResult.Success(Unit)
    }

    override suspend fun signOut(): AuthActionResult<Unit> {
      signOutCalled = true
      user = null
      authStateFlow.value = AuthState.Unauthenticated
      return AuthActionResult.Success(Unit)
    }

    override suspend fun deleteAccount(): AuthActionResult<Unit> {
      user = null
      authStateFlow.value = AuthState.Unauthenticated
      return AuthActionResult.Success(Unit)
    }

    override suspend fun getIdToken(forceRefresh: Boolean): String? {
      return user?.id?.let { "fake_token_$it" }
    }
  }

  private class FakeUserRepository(
    var onboardingDone: Boolean = false,
    var activeUser: UserProfile = UserProfile(id = "local_guest_1", displayName = "KASA Explorer", isLocalGuest = true)
  ) : UserRepository {
    private val userFlow = MutableStateFlow(activeUser)
    private val onboardingFlow = MutableStateFlow(onboardingDone)

    override fun getCurrentUser(): Flow<UserProfile> = userFlow
    override fun getUserSettings(): Flow<com.example.data.model.UserSettings> = flowOf(com.example.data.model.UserSettings(userId = activeUser.id))
    override fun isOnboardingCompleted(): Flow<Boolean> = onboardingFlow

    override suspend fun setOnboardingCompleted(completed: Boolean): AppResult<Unit> {
      onboardingDone = completed
      onboardingFlow.value = completed
      return AppResult.Success(Unit)
    }

    override suspend fun onUserAuthenticated(user: UserProfile): AppResult<Unit> {
      activeUser = user
      userFlow.value = activeUser
      return AppResult.Success(Unit)
    }

    override suspend fun onUserSignedOut(): AppResult<Unit> {
      activeUser = UserProfile(id = "guest_default", displayName = "KASA Explorer", isLocalGuest = true)
      userFlow.value = activeUser
      return AppResult.Success(Unit)
    }

    override suspend fun deleteUserData(userId: String): AppResult<Unit> {
      activeUser = UserProfile(id = "guest_default", displayName = "KASA Explorer", isLocalGuest = true)
      userFlow.value = activeUser
      return AppResult.Success(Unit)
    }

    override suspend fun updateDisplayName(name: String): AppResult<Unit> {
      activeUser = activeUser.copy(displayName = name)
      userFlow.value = activeUser
      return AppResult.Success(Unit)
    }

    override suspend fun updateThemeMode(themeMode: com.example.ui.theme.ThemeMode): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updatePreferredLanguage(language: com.example.core.ai.SupportedLanguage): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateResponseLength(length: com.example.data.model.ResponseLength): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateConversationalTone(tone: com.example.data.model.ConversationalTone): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateLearningStyle(style: com.example.data.model.LearningStyle): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateMemoryEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateHapticFeedback(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun switchUserAccount(newUserId: String): AppResult<Unit> = AppResult.Success(Unit)
  }

  @Test
  fun `test 1 - OnboardingViewModel advances pages and completes`() = runTest {
    val userRepo = FakeUserRepository(onboardingDone = false)
    val viewModel = OnboardingViewModel(userRepo)

    assertEquals(0, viewModel.uiState.value.currentPage)
    assertFalse(viewModel.uiState.value.isCompleted)

    viewModel.onPageChanged(1)
    assertEquals(1, viewModel.uiState.value.currentPage)

    var finished = false
    viewModel.completeOnboarding {
      finished = true
    }
    advanceUntilIdle()

    assertTrue(finished)
    assertTrue(userRepo.onboardingDone)
  }

  @Test
  fun `test 2 - AuthViewModel signs in successfully and syncs user profile`() = runTest {
    val authService = FakeAuthService()
    val userRepo = FakeUserRepository(onboardingDone = true)
    val viewModel = AuthViewModel(authService, userRepo)

    viewModel.onEmailChanged("kojo@kasa.ai")
    viewModel.onPasswordChanged("SecurePass123")

    var callbackCalled = false
    viewModel.submit {
      callbackCalled = true
    }
    advanceUntilIdle()

    assertTrue(callbackCalled)
    assertEquals("kojo@kasa.ai", authService.signInCalledWith?.first)
    assertEquals("firebase_u123", userRepo.getCurrentUser().first().id)
    assertFalse(userRepo.getCurrentUser().first().isLocalGuest)
  }

  @Test
  fun `test 3 - AuthViewModel guest mode bypasses credentials cleanly`() = runTest {
    val authService = FakeAuthService()
    val userRepo = FakeUserRepository(onboardingDone = true)
    val viewModel = AuthViewModel(authService, userRepo)

    var callbackCalled = false
    viewModel.continueAsGuest {
      callbackCalled = true
    }
    advanceUntilIdle()

    assertTrue(callbackCalled)
    assertTrue(userRepo.getCurrentUser().first().isLocalGuest)
  }

  @Test
  fun `test 4 - AuthErrorTranslator provides helpful human-readable messages`() {
    val networkEx = Exception("Network timeout occurred")
    val networkMsg = AuthErrorTranslator.translate(networkEx)
    assertTrue(networkMsg.contains("internet connection"))

    val passEx = Exception("Password is too short")
    val passMsg = AuthErrorTranslator.translate(passEx)
    assertTrue(passMsg.contains("password"))

    val genericMsg = AuthErrorTranslator.translate(null)
    assertTrue(genericMsg.contains("unexpected error"))
  }

  @Test
  fun `test 5 - SplashViewModel routes to Onboarding when first launch`() = runTest {
    val authService = FakeAuthService()
    val userRepo = FakeUserRepository(onboardingDone = false)
    val viewModel = SplashViewModel(userRepo, authService)

    val dest = viewModel.destination.first { it is SplashDestination.Navigate }
    assertEquals(SplashDestination.Navigate("onboarding"), dest)
  }

  @Test
  fun `test 6 - SplashViewModel routes to Home when already authenticated`() = runTest {
    val authService = FakeAuthService(user = UserProfile("u_existing", "Ama", "test@kasa.ai", isLocalGuest = false))
    val userRepo = FakeUserRepository(
      onboardingDone = true,
      activeUser = UserProfile("u_existing", "Ama", "test@kasa.ai", isLocalGuest = false)
    )
    val viewModel = SplashViewModel(userRepo, authService)

    val dest = viewModel.destination.first { it is SplashDestination.Navigate }
    assertEquals(SplashDestination.Navigate("home"), dest)
  }
}
