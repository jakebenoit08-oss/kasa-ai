package com.example.ui.screens.auth

import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.auth.AuthActionResult
import com.example.core.auth.AuthService
import com.example.core.auth.AuthState
import com.example.core.config.AppConfig
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AuthMode {
  SIGN_IN,
  SIGN_UP,
  FORGOT_PASSWORD,
}

data class AuthUiState(
  val mode: AuthMode = AuthMode.SIGN_IN,
  val email: String = "",
  val emailError: String? = null,
  val password: String = "",
  val passwordError: String? = null,
  val confirmPassword: String = "",
  val confirmPasswordError: String? = null,
  val displayName: String = "",
  val displayNameError: String? = null,
  val isPasswordVisible: Boolean = false,
  val isConfirmPasswordVisible: Boolean = false,
  val isLoading: Boolean = false,
  val generalError: String? = null,
  val successMessage: String? = null,
  val isFirebaseAvailable: Boolean = true,
)

class AuthViewModel(
  private val authService: AuthService,
  private val userRepository: UserRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  init {
    _uiState.value = _uiState.value.copy(
      isFirebaseAvailable = authService.isFirebaseAvailable()
    )

    viewModelScope.launch {
      authService.authState.collectLatest { state ->
        if (state is AuthState.Authenticated) {
          userRepository.onUserAuthenticated(state.user)
        }
      }
    }
  }

  fun setMode(mode: AuthMode) {
    _uiState.value = _uiState.value.copy(
      mode = mode,
      emailError = null,
      passwordError = null,
      confirmPasswordError = null,
      displayNameError = null,
      generalError = null,
      successMessage = null
    )
  }

  fun onEmailChanged(value: String) {
    _uiState.value = _uiState.value.copy(
      email = value,
      emailError = null,
      generalError = null
    )
  }

  fun onPasswordChanged(value: String) {
    _uiState.value = _uiState.value.copy(
      password = value,
      passwordError = null,
      generalError = null
    )
  }

  fun onConfirmPasswordChanged(value: String) {
    _uiState.value = _uiState.value.copy(
      confirmPassword = value,
      confirmPasswordError = null,
      generalError = null
    )
  }

  fun onDisplayNameChanged(value: String) {
    _uiState.value = _uiState.value.copy(
      displayName = value,
      displayNameError = null,
      generalError = null
    )
  }

  fun togglePasswordVisibility() {
    _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
  }

  fun toggleConfirmPasswordVisibility() {
    _uiState.value = _uiState.value.copy(isConfirmPasswordVisible = !_uiState.value.isConfirmPasswordVisible)
  }

  fun clearMessages() {
    _uiState.value = _uiState.value.copy(generalError = null, successMessage = null)
  }

  fun submit(onSuccess: () -> Unit) {
    when (_uiState.value.mode) {
      AuthMode.SIGN_IN -> submitSignIn(onSuccess)
      AuthMode.SIGN_UP -> submitSignUp(onSuccess)
      AuthMode.FORGOT_PASSWORD -> submitForgotPassword()
    }
  }

  private fun submitSignIn(onSuccess: () -> Unit) {
    val state = _uiState.value
    var hasError = false

    val email = state.email.trim()
    val password = state.password.trim()

    var emailErr: String? = null
    var passErr: String? = null

    if (email.isBlank()) {
      emailErr = "Enter your email address."
      hasError = true
    } else if (!isValidEmail(email)) {
      emailErr = "Enter a valid email address."
      hasError = true
    }

    if (password.isBlank()) {
      passErr = "Enter your password."
      hasError = true
    }

    if (hasError) {
      _uiState.value = state.copy(emailError = emailErr, passwordError = passErr)
      return
    }

    _uiState.value = state.copy(isLoading = true, generalError = null, successMessage = null)

    viewModelScope.launch {
      when (val result = authService.signInWithEmail(email, password)) {
        is AuthActionResult.Success -> {
          userRepository.onUserAuthenticated(result.data)
          _uiState.value = _uiState.value.copy(isLoading = false)
          onSuccess()
        }
        is AuthActionResult.Error -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            generalError = result.message
          )
        }
      }
    }
  }

  private fun submitSignUp(onSuccess: () -> Unit) {
    val state = _uiState.value
    var hasError = false

    val email = state.email.trim()
    val password = state.password.trim()
    val confirmPassword = state.confirmPassword.trim()
    val displayName = state.displayName.trim()

    var emailErr: String? = null
    var passErr: String? = null
    var confirmPassErr: String? = null
    var nameErr: String? = null

    if (email.isBlank()) {
      emailErr = "Enter your email address."
      hasError = true
    } else if (!isValidEmail(email)) {
      emailErr = "Enter a valid email address."
      hasError = true
    }

    if (displayName.isBlank()) {
      nameErr = "Enter your full name or nickname."
      hasError = true
    }

    if (password.isBlank()) {
      passErr = "Enter a password."
      hasError = true
    } else if (password.length < 6) {
      passErr = "Password must be at least 6 characters."
      hasError = true
    }

    if (confirmPassword != password) {
      confirmPassErr = "Passwords do not match."
      hasError = true
    }

    if (hasError) {
      _uiState.value = state.copy(
        emailError = emailErr,
        passwordError = passErr,
        confirmPasswordError = confirmPassErr,
        displayNameError = nameErr
      )
      return
    }

    _uiState.value = state.copy(isLoading = true, generalError = null, successMessage = null)

    viewModelScope.launch {
      when (val result = authService.signUpWithEmail(email, password, displayName)) {
        is AuthActionResult.Success -> {
          userRepository.onUserAuthenticated(result.data)
          _uiState.value = _uiState.value.copy(isLoading = false)
          onSuccess()
        }
        is AuthActionResult.Error -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            generalError = result.message
          )
        }
      }
    }
  }

  private fun submitForgotPassword() {
    val state = _uiState.value
    val email = state.email.trim()

    if (email.isBlank() || !isValidEmail(email)) {
      _uiState.value = state.copy(emailError = "Enter a valid email address.")
      return
    }

    _uiState.value = state.copy(isLoading = true, generalError = null, successMessage = null)

    viewModelScope.launch {
      when (val result = authService.sendPasswordResetEmail(email)) {
        is AuthActionResult.Success -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            successMessage = "Password reset instructions sent to $email."
          )
        }
        is AuthActionResult.Error -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            generalError = result.message
          )
        }
      }
    }
  }

  private fun isValidEmail(email: String): Boolean {
    return email.isNotBlank() && email.contains("@") && email.contains(".") && !email.contains(" ")
  }

  fun continueWithGoogle(context: Context, onSuccess: () -> Unit) {
    _uiState.value = _uiState.value.copy(
      isLoading = true,
      generalError = null,
      successMessage = null
    )

    viewModelScope.launch {
      try {
        val googleIdOption = GetGoogleIdOption.Builder()
          .setFilterByAuthorizedAccounts(false)
          .setServerClientId(AppConfig.GOOGLE_WEB_CLIENT_ID)
          .setAutoSelectEnabled(false)
          .build()

        val request = GetCredentialRequest.Builder()
          .addCredentialOption(googleIdOption)
          .build()

        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(
          request = request,
          context = context
        )

        val credential = result.credential
        val idToken = when {
          credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
            try {
              val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
              googleIdTokenCredential.idToken
            } catch (e: GoogleIdTokenParsingException) {
              Log.e("AuthViewModel", "Failed to parse Google ID token credential", e)
              null
            }
          }
          else -> null
        }

        if (idToken.isNullOrBlank()) {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            generalError = "Unable to retrieve valid Google sign-in credentials."
          )
          return@launch
        }

        when (val authResult = authService.signInWithGoogle(idToken)) {
          is AuthActionResult.Success -> {
            _uiState.value = _uiState.value.copy(isLoading = false, generalError = null)
            userRepository.onUserAuthenticated(authResult.data)
            onSuccess()
          }
          is AuthActionResult.Error -> {
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              generalError = authResult.message
            )
          }
        }
      } catch (e: GetCredentialCancellationException) {
        // User dismissed the Google sign-in prompt; cancel quietly without error banner
        _uiState.value = _uiState.value.copy(isLoading = false)
      } catch (e: CancellationException) {
        throw e
      } catch (e: GetCredentialException) {
        Log.w("AuthViewModel", "Google Credential Manager error: ${e.message}", e)
        _uiState.value = _uiState.value.copy(
          isLoading = false,
          generalError = e.message?.takeIf { it.isNotBlank() } ?: "Google sign-in could not be completed."
        )
      } catch (e: Throwable) {
        Log.e("AuthViewModel", "Google sign-in unexpected error", e)
        _uiState.value = _uiState.value.copy(
          isLoading = false,
          generalError = "Google sign-in could not be completed. Please try again or use Email & Password."
        )
      }
    }
  }

  fun continueAsGuest(onSuccess: () -> Unit) {
    viewModelScope.launch {
      val guestUser = UserProfile(
        id = "usr_guest_" + System.currentTimeMillis().toString().takeLast(6),
        displayName = "Guest Explorer",
        email = null,
        isLocalGuest = true,
      )
      userRepository.onUserAuthenticated(guestUser)
      onSuccess()
    }
  }
}
