package com.example.core.auth

import android.content.Context
import com.example.data.model.UserProfile
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseAuthServiceImpl(
  private val context: Context,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AuthService {

  private val firebaseAuth: FirebaseAuth? by lazy {
    try {
      if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(context)
      }
      FirebaseAuth.getInstance()
    } catch (e: Throwable) {
      null
    }
  }

  override val authState: Flow<AuthState> = callbackFlow {
    val auth = firebaseAuth
    if (auth == null) {
      trySend(AuthState.Unauthenticated)
      awaitClose { }
      return@callbackFlow
    }

    val listener = FirebaseAuth.AuthStateListener { firebaseAuthInstance ->
      val user = firebaseAuthInstance.currentUser
      if (user != null) {
        trySend(AuthState.Authenticated(user.toUserProfile()))
      } else {
        trySend(AuthState.Unauthenticated)
      }
    }

    auth.addAuthStateListener(listener)
    awaitClose {
      auth.removeAuthStateListener(listener)
    }
  }

  override fun isFirebaseAvailable(): Boolean {
    return firebaseAuth != null
  }

  override fun getCurrentUser(): UserProfile? {
    return firebaseAuth?.currentUser?.toUserProfile()
  }

  override suspend fun signUpWithEmail(
    email: String,
    password: String,
    displayName: String?,
  ): AuthActionResult<UserProfile> = withContext(ioDispatcher) {
    val auth = firebaseAuth
      ?: return@withContext AuthActionResult.Error("Firebase is not initialized. Please ensure Firebase is properly configured.")

    val cleanEmail = email.trim()
    val cleanPassword = password.trim()

    if (cleanEmail.isBlank()) {
      return@withContext AuthActionResult.Error("Enter a valid email address.")
    }
    if (cleanPassword.length < 6) {
      return@withContext AuthActionResult.Error("Choose a stronger password (at least 6 characters).")
    }

    try {
      val authResult = auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword).awaitTask()
      val user = authResult.user ?: return@withContext AuthActionResult.Error("Failed to create user session.")

      if (!displayName.isNullOrBlank()) {
        try {
          val changeRequest = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName.trim())
            .build()
          user.updateProfile(changeRequest).awaitTask()
        } catch (_: Exception) {
          // Profile name update is secondary; account creation already succeeded
        }
      }

      AuthActionResult.Success(user.toUserProfile())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      AuthActionResult.Error(AuthErrorTranslator.translate(e), cause = e)
    }
  }

  override suspend fun signInWithEmail(
    email: String,
    password: String,
  ): AuthActionResult<UserProfile> = withContext(ioDispatcher) {
    val auth = firebaseAuth
      ?: return@withContext AuthActionResult.Error("Firebase is not initialized. Please ensure Firebase is properly configured.")

    val cleanEmail = email.trim()
    val cleanPassword = password.trim()

    if (cleanEmail.isBlank()) {
      return@withContext AuthActionResult.Error("Enter a valid email address.")
    }
    if (cleanPassword.isBlank()) {
      return@withContext AuthActionResult.Error("Please enter your password.")
    }

    try {
      val authResult = auth.signInWithEmailAndPassword(cleanEmail, cleanPassword).awaitTask()
      val user = authResult.user ?: return@withContext AuthActionResult.Error("Failed to resolve user session.")
      AuthActionResult.Success(user.toUserProfile())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      AuthActionResult.Error(AuthErrorTranslator.translate(e), cause = e)
    }
  }

  override suspend fun signInWithGoogle(idToken: String): AuthActionResult<UserProfile> = withContext(ioDispatcher) {
    val auth = firebaseAuth
      ?: return@withContext AuthActionResult.Error("Firebase is not initialized. Please ensure Firebase is properly configured.")

    val cleanToken = idToken.trim()
    if (cleanToken.isBlank()) {
      return@withContext AuthActionResult.Error("Invalid Google authentication credential.")
    }

    try {
      val credential = GoogleAuthProvider.getCredential(cleanToken, null)
      val authResult = auth.signInWithCredential(credential).awaitTask()
      val user = authResult.user ?: return@withContext AuthActionResult.Error("Failed to resolve Google user session.")
      AuthActionResult.Success(user.toUserProfile())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      AuthActionResult.Error(AuthErrorTranslator.translate(e), cause = e)
    }
  }

  override suspend fun sendPasswordResetEmail(email: String): AuthActionResult<Unit> = withContext(ioDispatcher) {
    val auth = firebaseAuth
      ?: return@withContext AuthActionResult.Error("Firebase is not initialized. Please ensure Firebase is properly configured.")

    val cleanEmail = email.trim()
    if (cleanEmail.isBlank()) {
      return@withContext AuthActionResult.Error("Enter a valid email address.")
    }

    try {
      auth.sendPasswordResetEmail(cleanEmail).awaitTask()
      AuthActionResult.Success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      AuthActionResult.Error(AuthErrorTranslator.translate(e), cause = e)
    }
  }

  override suspend fun updateDisplayName(newDisplayName: String): AuthActionResult<Unit> = withContext(ioDispatcher) {
    val auth = firebaseAuth
      ?: return@withContext AuthActionResult.Error("Firebase is not initialized.")

    val user = auth.currentUser
      ?: return@withContext AuthActionResult.Error("No authenticated user session found.")

    val cleanName = newDisplayName.trim()
    if (cleanName.isBlank()) {
      return@withContext AuthActionResult.Error("Display name cannot be empty.")
    }

    try {
      val changeRequest = UserProfileChangeRequest.Builder()
        .setDisplayName(cleanName)
        .build()
      user.updateProfile(changeRequest).awaitTask()
      AuthActionResult.Success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      AuthActionResult.Error(AuthErrorTranslator.translate(e), cause = e)
    }
  }

  override suspend fun signOut(): AuthActionResult<Unit> = withContext(ioDispatcher) {
    val auth = firebaseAuth
    try {
      auth?.signOut()
      AuthActionResult.Success(Unit)
    } catch (e: Throwable) {
      AuthActionResult.Error(AuthErrorTranslator.translate(e), cause = e)
    }
  }

  override suspend fun deleteAccount(): AuthActionResult<Unit> = withContext(ioDispatcher) {
    val auth = firebaseAuth
      ?: return@withContext AuthActionResult.Error("Firebase is not initialized.")

    val user = auth.currentUser
      ?: return@withContext AuthActionResult.Error("No authenticated user session found.")

    try {
      user.delete().awaitTask()
      AuthActionResult.Success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      AuthActionResult.Error(AuthErrorTranslator.translate(e), cause = e)
    }
  }

  override suspend fun getIdToken(forceRefresh: Boolean): String? = withContext(ioDispatcher) {
    val auth = firebaseAuth ?: return@withContext null
    val user = auth.currentUser ?: return@withContext null
    try {
      val result = user.getIdToken(forceRefresh).awaitTask()
      result.token
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      null
    }
  }

  private fun FirebaseUser.toUserProfile(): UserProfile {
    val emailPrefix = this.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
    val display = this.displayName?.takeIf { it.isNotBlank() } ?: emailPrefix ?: "KASA Member"
    return UserProfile(
      id = this.uid,
      displayName = display,
      email = this.email,
      createdAt = this.metadata?.creationTimestamp ?: System.currentTimeMillis(),
      avatarUrl = this.photoUrl?.toString(),
      isLocalGuest = false,
    )
  }
}

/**
 * Extension to await Google Tasks in Kotlin coroutines safely without crashing on cancellations.
 */
internal suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
  addOnSuccessListener { result ->
    cont.resume(result)
  }
  addOnFailureListener { exception ->
    cont.resumeWithException(exception)
  }
  addOnCanceledListener {
    cont.cancel()
  }
}
