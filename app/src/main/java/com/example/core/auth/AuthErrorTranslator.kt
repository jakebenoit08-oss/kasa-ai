package com.example.core.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * Translates low-level Firebase and network authentication exceptions into
 * clear, actionable, user-friendly messages without exposing raw stack traces.
 */
object AuthErrorTranslator {

  fun translate(throwable: Throwable?): String {
    if (throwable == null) return "An unexpected error occurred. Please try again."

    return when (throwable) {
      is FirebaseAuthInvalidUserException -> {
        "No account found with this email. Please check your email or sign up."
      }
      is FirebaseAuthWeakPasswordException -> {
        "Password is too weak. Please use at least 6 characters with letters and numbers."
      }
      is FirebaseAuthUserCollisionException -> {
        "An account with this email already exists. Please sign in instead."
      }
      is FirebaseAuthInvalidCredentialsException -> {
        val msg = throwable.message?.lowercase().orEmpty()
        if (msg.contains("badly formatted") || msg.contains("email")) {
          "Enter a valid email address."
        } else {
          "Incorrect email or password. Please try again."
        }
      }
      is FirebaseAuthRecentLoginRequiredException -> {
        "This sensitive action requires recent authentication. Please sign out and sign in again."
      }
      is FirebaseNetworkException -> {
        "Check your internet connection and try again."
      }
      is FirebaseAuthActionCodeException -> {
        "The password reset link has expired or has already been used."
      }
      is FirebaseAuthException -> {
        when (throwable.errorCode) {
          "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
          "ERROR_WRONG_PASSWORD" -> "Incorrect email or password."
          "ERROR_USER_NOT_FOUND" -> "No account found with this email."
          "ERROR_USER_DISABLED" -> "This user account has been disabled."
          "ERROR_TOO_MANY_REQUESTS" -> "Too many failed attempts. Please wait a moment before trying again."
          "ERROR_OPERATION_NOT_ALLOWED" -> "Email and password sign-in is not enabled in this project."
          "ERROR_WEAK_PASSWORD" -> "Choose a stronger password (at least 6 characters)."
          "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists."
          else -> throwable.localizedMessage ?: "Authentication failed. Please check your credentials."
        }
      }
      is IllegalStateException -> {
        if (throwable.message?.contains("FirebaseApp is not initialized") == true) {
          "Firebase is not configured in this build. Please provide a valid google-services.json."
        } else {
          throwable.localizedMessage ?: "Operation failed. Please try again."
        }
      }
      else -> {
        val msg = throwable.localizedMessage ?: ""
        when {
          msg.contains("network", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) -> {
            "Check your internet connection and try again."
          }
          msg.contains("password", ignoreCase = true) -> {
            "Please check your password and try again."
          }
          msg.isNotBlank() -> msg
          else -> "Something went wrong. Please try again."
        }
      }
    }
  }
}
