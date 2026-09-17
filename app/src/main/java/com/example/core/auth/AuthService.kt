package com.example.core.auth

import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Contract for user authentication and session lifecycle management in KASA AI.
 */
interface AuthService {

  /**
   * Flow of user authentication state.
   */
  val authState: Flow<AuthState>

  /**
   * Returns current authenticated user snapshot if available, or null.
   */
  fun getCurrentUser(): UserProfile?

  /**
   * Returns whether real Firebase Auth is configured and available in runtime.
   */
  fun isFirebaseAvailable(): Boolean

  /**
   * Creates a new user account with email and password via Firebase Auth.
   */
  suspend fun signUpWithEmail(
    email: String,
    password: String,
    displayName: String? = null,
  ): AuthActionResult<UserProfile>

  /**
   * Signs in an existing user with email and password.
   */
  suspend fun signInWithEmail(
    email: String,
    password: String,
  ): AuthActionResult<UserProfile>

  /**
   * Authenticates with Firebase using a verified Google ID Token obtained from Credential Manager.
   */
  suspend fun signInWithGoogle(idToken: String): AuthActionResult<UserProfile>

  /**
   * Dispatches a password reset email to the specified address.
   */
  suspend fun sendPasswordResetEmail(email: String): AuthActionResult<Unit>

  /**
   * Updates the user's profile display name.
   */
  suspend fun updateDisplayName(newDisplayName: String): AuthActionResult<Unit>

  /**
   * Signs out the current user and terminates the authenticated session.
   */
  suspend fun signOut(): AuthActionResult<Unit>

  /**
   * Permanently deletes the authenticated user account from Firebase.
   */
  suspend fun deleteAccount(): AuthActionResult<Unit>

  /**
   * Retrieves the current user's Firebase Auth ID token (JWT) for authenticating with KASA backend.
   * Returns null if unauthenticated or Firebase is unavailable.
   */
  suspend fun getIdToken(forceRefresh: Boolean = false): String?
}
