package com.example.data.model

import com.example.core.ai.SupportedLanguage
import com.example.ui.theme.ThemeMode

/**
 * User profile entity.
 * 
 * All user data in KASA AI is strictly isolated by [id].
 * No global shared state or cross-account data leaking is permitted.
 */
data class UserProfile(
  val id: String,
  val displayName: String,
  val email: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val avatarUrl: String? = null,
  val isLocalGuest: Boolean = true,
)

/**
 * Preferred length of AI textual responses.
 */
enum class ResponseLength(val id: String, val label: String, val promptDirective: String) {
  SHORT("short", "Short", "Keep responses concise, crisp, and direct without unnecessary filler."),
  BALANCED("balanced", "Balanced", "Provide balanced, clear, and comprehensive responses."),
  DETAILED("detailed", "Detailed", "Provide thorough, in-depth breakdowns with rich contextual details."),
  ;

  companion object {
    fun fromId(id: String?): ResponseLength = entries.firstOrNull { it.id == id } ?: BALANCED
  }
}

/**
 * Preferred conversational tone for AI interactions.
 */
enum class ConversationalTone(val id: String, val label: String, val promptDirective: String) {
  FRIENDLY("friendly", "Friendly", "Warm, conversational, empathetic, and encouraging tone."),
  NEUTRAL("neutral", "Neutral", "Objective, straightforward, and direct tone."),
  PROFESSIONAL("professional", "Professional", "Polished, formal, articulate, and academically rigorous tone."),
  ;

  companion object {
    fun fromId(id: String?): ConversationalTone = entries.firstOrNull { it.id == id } ?: FRIENDLY
  }
}

/**
 * Preferred pedagogical style for learning and tutoring interactions.
 */
enum class LearningStyle(val id: String, val label: String, val promptDirective: String) {
  SIMPLE("simple", "Simple", "Break concepts down using intuitive, high-level analogies and plain terms."),
  STEP_BY_STEP("step_by_step", "Step-by-step", "Teach methodically in sequential, structured steps."),
  EXAMPLES_FIRST("examples_first", "Examples first", "Lead with concrete Ghanaian and real-world examples before theoretical definitions."),
  ;

  companion object {
    fun fromId(id: String?): LearningStyle = entries.firstOrNull { it.id == id } ?: STEP_BY_STEP
  }
}

data class UserSettings(
  val userId: String,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val preferredLanguage: SupportedLanguage = SupportedLanguage.ENGLISH,
  val responseLength: ResponseLength = ResponseLength.BALANCED,
  val conversationalTone: ConversationalTone = ConversationalTone.FRIENDLY,
  val learningStyle: LearningStyle = LearningStyle.STEP_BY_STEP,
  val memoryEnabled: Boolean = true,
  val hapticFeedbackEnabled: Boolean = true,
  val notificationsEnabled: Boolean = false,
  val localSessionIsolationActive: Boolean = true,
)
