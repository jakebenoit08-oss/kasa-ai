package com.example

import com.example.core.ai.SupportedLanguage
import com.example.core.ai.context.GhanaianContextConfig
import com.example.core.ai.memory.MemoryDetectionResult
import com.example.core.ai.memory.MemoryIntentDetector
import com.example.data.model.ConversationalTone
import com.example.data.model.LearningStyle
import com.example.data.model.ResponseLength
import com.example.data.model.UserSettings
import com.example.data.model.memory.MemoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KasaPhase6PersonalizationMemoryTest {

  @Test
  fun memoryIntentDetector_detectsExplicitRememberCommands() {
    val input = "Remember that I am a final-year high school student preparing for WASSCE in Kumasi."
    val detected = MemoryIntentDetector.scanForMemoryIntent(input)

    assertTrue("Explicit remember prompt should be detected as Candidate", detected is MemoryDetectionResult.Candidate)
    val candidate = detected as MemoryDetectionResult.Candidate
    assertTrue(candidate.cleanSnippet.contains("final-year high school student"))
  }

  @Test
  fun memoryIntentDetector_rejectsSensitiveInformation() {
    val input = "Remember that my password is SecretPassword123"
    val detected = MemoryIntentDetector.scanForMemoryIntent(input)

    assertTrue("Sensitive credentials must be rejected by memory safety filters", detected is MemoryDetectionResult.RejectedSensitive)
  }

  @Test
  fun memoryIntentDetector_ignoresStandardConversations() {
    val input = "Can you help me calculate the force on a 5kg mass?"
    val detected = MemoryIntentDetector.scanForMemoryIntent(input)

    assertTrue("Regular prompt should return None", detected is MemoryDetectionResult.None)
  }

  @Test
  fun ghanaianContextConfig_injectsPersonalizationAndMemories() {
    val settings = UserSettings(
      userId = "u1",
      preferredLanguage = SupportedLanguage.TWI,
      responseLength = ResponseLength.SHORT,
      conversationalTone = ConversationalTone.FRIENDLY,
      learningStyle = LearningStyle.STEP_BY_STEP,
      memoryEnabled = true,
    )

    val memories = listOf(
      MemoryItem(id = "1", userId = "u1", memoryText = "User is studying elective biology and chemistry."),
      MemoryItem(id = "2", userId = "u1", memoryText = "User lives in Cape Coast."),
    )

    val prompt = GhanaianContextConfig.buildSystemInstruction(
      settings = settings,
      memories = memories,
    )

    assertTrue(prompt.contains("PERSONALIZATION PREFERENCES"))
    assertTrue(prompt.contains("Keep responses concise, crisp, and direct"))
    assertTrue(prompt.contains("Teach methodically in sequential, structured steps"))
    assertTrue(prompt.contains("USER-CONFIGURED MEMORIES & PREFERENCES"))
    assertTrue(prompt.contains("User is studying elective biology and chemistry."))
    assertTrue(prompt.contains("User lives in Cape Coast."))
  }

  @Test
  fun ghanaianContextConfig_skipsMemoriesWhenDisabled() {
    val settings = UserSettings(
      userId = "u1",
      preferredLanguage = SupportedLanguage.ENGLISH,
      memoryEnabled = false,
    )

    val memories = listOf(
      MemoryItem(id = "1", userId = "u1", memoryText = "User is studying elective biology."),
    )

    val prompt = GhanaianContextConfig.buildSystemInstruction(
      settings = settings,
      memories = memories,
    )

    assertTrue("When memoryEnabled is false, memories must not be injected into the prompt", !prompt.contains("User is studying elective biology."))
  }
}
