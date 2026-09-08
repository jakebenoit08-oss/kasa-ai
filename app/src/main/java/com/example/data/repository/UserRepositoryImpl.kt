package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.ai.SupportedLanguage
import com.example.core.error.AppError
import com.example.core.result.AppResult
import com.example.data.model.ConversationalTone
import com.example.data.model.LearningStyle
import com.example.data.model.ResponseLength
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "kasa_user_prefs")

class UserRepositoryImpl(
  private val context: Context,
) : UserRepository {

  companion object {
    private val KEY_ACTIVE_USER_ID = stringPreferencesKey("active_user_id")
    private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed_v1")

    private fun keyDisplayName(userId: String) = stringPreferencesKey("user_${userId}_display_name")
    private fun keyEmail(userId: String) = stringPreferencesKey("user_${userId}_email")
    private fun keyIsGuest(userId: String) = booleanPreferencesKey("user_${userId}_is_guest")
    private fun keyCreatedAt(userId: String) = longPreferencesKey("user_${userId}_created_at")
    private fun keyThemeMode(userId: String) = stringPreferencesKey("user_${userId}_theme_mode")
    private fun keyLanguage(userId: String) = stringPreferencesKey("user_${userId}_language")
    private fun keyResponseLength(userId: String) = stringPreferencesKey("user_${userId}_response_length")
    private fun keyConversationalTone(userId: String) = stringPreferencesKey("user_${userId}_conversational_tone")
    private fun keyLearningStyle(userId: String) = stringPreferencesKey("user_${userId}_learning_style")
    private fun keyMemoryEnabled(userId: String) = booleanPreferencesKey("user_${userId}_memory_enabled")
    private fun keyHaptics(userId: String) = booleanPreferencesKey("user_${userId}_haptics")
  }

  override fun isOnboardingCompleted(): Flow<Boolean> {
    return context.userDataStore.data.map { prefs ->
      prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }
  }

  override suspend fun setOnboardingCompleted(completed: Boolean): AppResult<Unit> {
    return try {
      context.userDataStore.edit { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] = completed
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to update onboarding state", e))
    }
  }

  private suspend fun getOrCreateActiveUserId(prefs: Preferences): String {
    val existing = prefs[KEY_ACTIVE_USER_ID]
    if (!existing.isNullOrBlank()) return existing

    val newId = "usr_" + UUID.randomUUID().toString().take(8)
    context.userDataStore.edit { mutablePrefs ->
      mutablePrefs[KEY_ACTIVE_USER_ID] = newId
      mutablePrefs[keyDisplayName(newId)] = "KASA Explorer"
      mutablePrefs[keyIsGuest(newId)] = true
      mutablePrefs[keyCreatedAt(newId)] = System.currentTimeMillis()
      mutablePrefs[keyMemoryEnabled(newId)] = true
    }
    return newId
  }

  override suspend fun onUserAuthenticated(user: UserProfile): AppResult<Unit> {
    return try {
      context.userDataStore.edit { prefs ->
        prefs[KEY_ACTIVE_USER_ID] = user.id
        prefs[keyDisplayName(user.id)] = user.displayName
        if (user.email != null) {
          prefs[keyEmail(user.id)] = user.email
        }
        prefs[keyIsGuest(user.id)] = user.isLocalGuest
        if (prefs[keyCreatedAt(user.id)] == null) {
          prefs[keyCreatedAt(user.id)] = user.createdAt
        }
        if (prefs[keyMemoryEnabled(user.id)] == null) {
          prefs[keyMemoryEnabled(user.id)] = true
        }
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to register authenticated user session", e))
    }
  }

  override suspend fun onUserSignedOut(): AppResult<Unit> {
    return try {
      context.userDataStore.edit { prefs ->
        prefs.remove(KEY_ACTIVE_USER_ID)
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to clear user session", e))
    }
  }

  override fun getCurrentUser(): Flow<UserProfile> {
    return context.userDataStore.data.map { prefs ->
      val userId = prefs[KEY_ACTIVE_USER_ID] ?: "usr_default"
      val displayName = prefs[keyDisplayName(userId)] ?: "KASA Member"
      val email = prefs[keyEmail(userId)]
      val isGuest = prefs[keyIsGuest(userId)] ?: false
      val createdAt = prefs[keyCreatedAt(userId)] ?: System.currentTimeMillis()

      UserProfile(
        id = userId,
        displayName = displayName,
        email = email,
        createdAt = createdAt,
        isLocalGuest = isGuest,
      )
    }
  }

  override fun getUserSettings(): Flow<UserSettings> {
    return context.userDataStore.data.map { prefs ->
      val userId = prefs[KEY_ACTIVE_USER_ID] ?: "usr_default"
      val themeStr = prefs[keyThemeMode(userId)] ?: ThemeMode.SYSTEM.name
      val langCode = prefs[keyLanguage(userId)] ?: SupportedLanguage.ENGLISH.code
      val responseLengthStr = prefs[keyResponseLength(userId)] ?: ResponseLength.BALANCED.id
      val toneStr = prefs[keyConversationalTone(userId)] ?: ConversationalTone.FRIENDLY.id
      val learningStyleStr = prefs[keyLearningStyle(userId)] ?: LearningStyle.STEP_BY_STEP.id
      val memoryEnabled = prefs[keyMemoryEnabled(userId)] ?: true
      val haptics = prefs[keyHaptics(userId)] ?: true

      val theme = try {
        ThemeMode.valueOf(themeStr)
      } catch (_: Exception) {
        ThemeMode.SYSTEM
      }

      val language = SupportedLanguage.entries.find { it.code == langCode } ?: SupportedLanguage.ENGLISH
      val responseLength = ResponseLength.fromId(responseLengthStr)
      val conversationalTone = ConversationalTone.fromId(toneStr)
      val learningStyle = LearningStyle.fromId(learningStyleStr)

      UserSettings(
        userId = userId,
        themeMode = theme,
        preferredLanguage = language,
        responseLength = responseLength,
        conversationalTone = conversationalTone,
        learningStyle = learningStyle,
        memoryEnabled = memoryEnabled,
        hapticFeedbackEnabled = haptics,
        notificationsEnabled = false,
        localSessionIsolationActive = true,
      )
    }
  }

  override suspend fun updateDisplayName(name: String): AppResult<Unit> {
    if (name.isBlank()) {
      return AppResult.Error(AppError.ValidationError("Display name cannot be empty."))
    }
    return try {
      val prefs = context.userDataStore.data.first()
      val userId = getOrCreateActiveUserId(prefs)
      context.userDataStore.edit { mutablePrefs ->
        mutablePrefs[keyDisplayName(userId)] = name.trim()
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.UnknownError(cause = e))
    }
  }

  override suspend fun updateThemeMode(themeMode: ThemeMode): AppResult<Unit> {
    return try {
      val prefs = context.userDataStore.data.first()
      val userId = getOrCreateActiveUserId(prefs)
      context.userDataStore.edit { mutablePrefs ->
        mutablePrefs[keyThemeMode(userId)] = themeMode.name
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.UnknownError(cause = e))
    }
  }

  override suspend fun updatePreferredLanguage(language: SupportedLanguage): AppResult<Unit> {
    return try {
      val prefs = context.userDataStore.data.first()
      val userId = getOrCreateActiveUserId(prefs)
      context.userDataStore.edit { mutablePrefs ->
        mutablePrefs[keyLanguage(userId)] = language.code
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.UnknownError(cause = e))
    }
  }

  override suspend fun updateResponseLength(length: ResponseLength): AppResult<Unit> {
    return try {
      val prefs = context.userDataStore.data.first()
      val userId = getOrCreateActiveUserId(prefs)
      context.userDataStore.edit { mutablePrefs ->
        mutablePrefs[keyResponseLength(userId)] = length.id
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.UnknownError(cause = e))
    }
  }

  override suspend fun updateConversationalTone(tone: ConversationalTone): AppResult<Unit> {
    return try {
      val prefs = context.userDataStore.data.first()
      val userId = getOrCreateActiveUserId(prefs)
      context.userDataStore.edit { mutablePrefs ->
        mutablePrefs[keyConversationalTone(userId)] = tone.id
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.UnknownError(cause = e))
    }
  }

  override suspend fun updateLearningStyle(style: LearningStyle): AppResult<Unit> {
    return try {
      val prefs = context.userDataStore.data.first()
      val userId = getOrCreateActiveUserId(prefs)
      context.userDataStore.edit { mutablePrefs ->
        mutablePrefs[keyLearningStyle(userId)] = style.id
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.UnknownError(cause = e))
    }
  }

  override suspend fun updateMemoryEnabled(enabled: Boolean): AppResult<Unit> {
    return try {
      val prefs = context.userDataStore.data.first()
      val userId = getOrCreateActiveUserId(prefs)
      context.userDataStore.edit { mutablePrefs ->
        mutablePrefs[keyMemoryEnabled(userId)] = enabled
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.UnknownError(cause = e))
    }
  }

  override suspend fun updateHapticFeedback(enabled: Boolean): AppResult<Unit> {
    return try {
      val prefs = context.userDataStore.data.first()
      val userId = getOrCreateActiveUserId(prefs)
      context.userDataStore.edit { mutablePrefs ->
        mutablePrefs[keyHaptics(userId)] = enabled
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.UnknownError(cause = e))
    }
  }

  override suspend fun switchUserAccount(newUserId: String): AppResult<Unit> {
    return try {
      context.userDataStore.edit { mutablePrefs ->
        mutablePrefs[KEY_ACTIVE_USER_ID] = newUserId
        if (mutablePrefs[keyDisplayName(newUserId)] == null) {
          mutablePrefs[keyDisplayName(newUserId)] = "KASA Member"
          mutablePrefs[keyCreatedAt(newUserId)] = System.currentTimeMillis()
        }
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.UnknownError(cause = e))
    }
  }

  override suspend fun deleteUserData(userId: String): AppResult<Unit> {
    return try {
      context.userDataStore.edit { mutablePrefs ->
        mutablePrefs.remove(keyDisplayName(userId))
        mutablePrefs.remove(keyEmail(userId))
        mutablePrefs.remove(keyIsGuest(userId))
        mutablePrefs.remove(keyCreatedAt(userId))
        mutablePrefs.remove(keyThemeMode(userId))
        mutablePrefs.remove(keyLanguage(userId))
        mutablePrefs.remove(keyResponseLength(userId))
        mutablePrefs.remove(keyConversationalTone(userId))
        mutablePrefs.remove(keyLearningStyle(userId))
        mutablePrefs.remove(keyMemoryEnabled(userId))
        mutablePrefs.remove(keyHaptics(userId))
        if (mutablePrefs[KEY_ACTIVE_USER_ID] == userId) {
          mutablePrefs.remove(KEY_ACTIVE_USER_ID)
        }
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to delete user preferences", e))
    }
  }
}
