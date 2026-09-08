package com.example.data.repository

import com.example.core.ai.SupportedLanguage
import com.example.core.result.AppResult
import com.example.data.model.ConversationalTone
import com.example.data.model.LearningStyle
import com.example.data.model.ResponseLength
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserRepository {
  fun getCurrentUser(): Flow<UserProfile>
  fun getUserSettings(): Flow<UserSettings>
  fun isOnboardingCompleted(): Flow<Boolean>
  suspend fun setOnboardingCompleted(completed: Boolean): AppResult<Unit>
  suspend fun onUserAuthenticated(user: UserProfile): AppResult<Unit>
  suspend fun onUserSignedOut(): AppResult<Unit>
  suspend fun updateDisplayName(name: String): AppResult<Unit>
  suspend fun updateThemeMode(themeMode: ThemeMode): AppResult<Unit>
  suspend fun updatePreferredLanguage(language: SupportedLanguage): AppResult<Unit>
  suspend fun updateResponseLength(length: ResponseLength): AppResult<Unit>
  suspend fun updateConversationalTone(tone: ConversationalTone): AppResult<Unit>
  suspend fun updateLearningStyle(style: LearningStyle): AppResult<Unit>
  suspend fun updateMemoryEnabled(enabled: Boolean): AppResult<Unit>
  suspend fun updateHapticFeedback(enabled: Boolean): AppResult<Unit>
  suspend fun switchUserAccount(newUserId: String): AppResult<Unit>
  suspend fun deleteUserData(userId: String): AppResult<Unit>
}
