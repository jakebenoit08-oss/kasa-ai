package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.ai.ChatTurn
import com.example.core.ai.GenerationPrompt
import com.example.core.ai.SupportedLanguage
import com.example.core.ai.gemini.GeminiConversationalAIService
import com.example.core.config.AppConfig
import com.example.core.result.AppResult
import com.example.data.local.KasaDatabase
import com.example.data.model.MessageDeliveryStatus
import com.example.data.model.MessageSender
import com.example.data.repository.ConversationRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KasaFoundationUnitTest {

  private lateinit var db: KasaDatabase
  private lateinit var repository: ConversationRepositoryImpl

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, KasaDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = ConversationRepositoryImpl(db.conversationDao())
  }

  @After
  fun teardown() {
    db.close()
  }

  @Test
  fun `test 1 and 2 - verify real AI request construction and multi-turn context formatting`() {
    val aiService = GeminiConversationalAIService(
      modelName = AppConfig.GEMINI_DEFAULT_MODEL,
      customApiKeyProvider = { "mock-key" },
    )

    val prompt = GenerationPrompt(
      userPrompt = "What is my name?",
      history = listOf(
        ChatTurn("user", "My name is Kojo."),
        ChatTurn("model", "Nice to meet you, Kojo!"),
      ),
      systemInstruction = AppConfig.KASA_SYSTEM_INSTRUCTION,
    )

    val geminiRequest = aiService.buildGeminiRequest(prompt)

    // Verify 3 content blocks: turn 1 user, turn 2 model, turn 3 current user prompt
    assertEquals(3, geminiRequest.contents.size)
    assertEquals("user", geminiRequest.contents[0].role)
    assertEquals("My name is Kojo.", geminiRequest.contents[0].parts.first().text)

    assertEquals("model", geminiRequest.contents[1].role)
    assertEquals("Nice to meet you, Kojo!", geminiRequest.contents[1].parts.first().text)

    assertEquals("user", geminiRequest.contents[2].role)
    assertEquals("What is my name?", geminiRequest.contents[2].parts.first().text)

    // Verify system instruction is set
    assertNotNull(geminiRequest.systemInstruction)
    assertTrue(geminiRequest.systemInstruction?.parts?.first()?.text?.contains("KASA AI") == true)
  }

  @Test
  fun `test 3 and 4 - verify persistence across repository sessions in Room database`() = runTest {
    val userId = "usr_kofi"
    val convResult = repository.createConversation(userId, "Kumasi Market Trip")
    assertTrue(convResult is AppResult.Success)
    val conv = (convResult as AppResult.Success).data

    // Send user message and assistant message
    repository.appendUserMessage(userId, conv.id, "Where is Kejetia market?")
    repository.appendAssistantMessage(userId, conv.id, "Kejetia market is in Kumasi, Ghana.")

    // Simulate reopening / new repository instance pointing to the same database
    val newRepoInstance = ConversationRepositoryImpl(db.conversationDao())
    val messages = newRepoInstance.getMessages(userId, conv.id).first()

    assertEquals(2, messages.size)
    assertEquals("Where is Kejetia market?", messages[0].content)
    assertEquals(MessageSender.USER, messages[0].sender)
    assertEquals("Kejetia market is in Kumasi, Ghana.", messages[1].content)
    assertEquals(MessageSender.ASSISTANT, messages[1].sender)
  }

  @Test
  fun `test 5 - verify strict account and session data isolation`() = runTest {
    val userA = "usr_tenant_alice"
    val userB = "usr_tenant_bob"

    val convAResult = repository.createConversation(userA, "Alice Private Notes")
    assertTrue(convAResult is AppResult.Success)
    val convA = (convAResult as AppResult.Success).data

    repository.appendUserMessage(userA, convA.id, "Confidential note for Alice")

    // User B queries conversations
    val userBConvs = repository.getConversations(userB).first()
    assertTrue("User B must have empty conversation list", userBConvs.isEmpty())

    // User B queries messages from User A's conversation
    val userBMessages = repository.getMessages(userB, convA.id).first()
    assertTrue("User B must not see User A messages", userBMessages.isEmpty())

    // User A can see their own messages
    val userAMessages = repository.getMessages(userA, convA.id).first()
    assertEquals(1, userAMessages.size)
    assertEquals("Confidential note for Alice", userAMessages.first().content)
  }

  @Test
  fun `test 7 - verify empty message prevention`() = runTest {
    val userId = "usr_kwame"
    val convResult = repository.createConversation(userId, "Test Empty")
    val conv = (convResult as AppResult.Success).data

    val blankResult = repository.appendUserMessage(userId, conv.id, "   ")
    assertTrue(blankResult is AppResult.Error)

    val msgs = repository.getMessages(userId, conv.id).first()
    assertTrue(msgs.isEmpty())
  }

  @Test
  fun `test 8 - verify multiple conversations separation and deletion`() = runTest {
    val userId = "usr_ama"
    val conv1 = (repository.createConversation(userId, "Conversation One") as AppResult.Success).data
    val conv2 = (repository.createConversation(userId, "Conversation Two") as AppResult.Success).data

    repository.appendUserMessage(userId, conv1.id, "Message in 1")
    repository.appendUserMessage(userId, conv2.id, "Message in 2")

    val msgs1 = repository.getMessages(userId, conv1.id).first()
    val msgs2 = repository.getMessages(userId, conv2.id).first()

    assertEquals(1, msgs1.size)
    assertEquals("Message in 1", msgs1.first().content)
    assertEquals(1, msgs2.size)
    assertEquals("Message in 2", msgs2.first().content)

    // Rename conv1
    repository.renameConversation(userId, conv1.id, "Renamed One")
    val convsAfterRename = repository.getConversations(userId).first()
    assertEquals("Renamed One", convsAfterRename.first { it.id == conv1.id }.title)

    // Delete conv1
    repository.deleteConversation(userId, conv1.id)
    val convsAfterDelete = repository.getConversations(userId).first()
    assertEquals(1, convsAfterDelete.size)
    assertEquals(conv2.id, convsAfterDelete.first().id)
    assertTrue(repository.getMessages(userId, conv1.id).first().isEmpty())
  }

  @Test
  fun `test 9 - verify supported languages configuration`() {
    val languages = SupportedLanguage.entries
    assertTrue(languages.any { it == SupportedLanguage.ENGLISH })
    assertTrue(languages.any { it == SupportedLanguage.TWI })
    assertTrue(languages.any { it == SupportedLanguage.GA })
    assertTrue(languages.any { it == SupportedLanguage.EWE })
    assertTrue(languages.any { it == SupportedLanguage.FANTE })
    assertTrue(languages.any { it == SupportedLanguage.DAGBANI })
    assertTrue(languages.any { it == SupportedLanguage.HAUSA })
  }
}
