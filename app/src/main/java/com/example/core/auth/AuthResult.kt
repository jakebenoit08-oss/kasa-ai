package com.example.core.auth

import com.example.data.model.UserProfile

/**
 * Authentication state observed across the application lifecycle.
 */
sealed class AuthState {
  data object Initializing : AuthState()
  data class Authenticated(val user: UserProfile) : AuthState()
  data object Unauthenticated : AuthState()
}

/**
 * Result wrapper for authentication actions.
 */
sealed class AuthActionResult<out T> {
  data class Success<out T>(val data: T) : AuthActionResult<T>()
  data class Error(val message: String, val cause: Throwable? = null) : AuthActionResult<Nothing>()
}
