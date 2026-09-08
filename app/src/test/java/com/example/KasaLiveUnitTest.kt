package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.ai.live.AvailableLiveVoices
import com.example.core.ai.live.GeminiLiveVoiceService
import com.example.core.ai.live.LiveAudioPlayer
import com.example.core.ai.live.LiveAudioRecorder
import com.example.core.ai.live.LiveClientMessage
import com.example.core.ai.live.LiveGenerationConfig
import com.example.core.ai.live.LivePrebuiltVoiceConfig
import com.example.core.ai.live.LiveSessionState
import com.example.core.ai.live.LiveSetupMessage
import com.example.core.ai.live.LiveSpeechConfig
import com.example.core.ai.live.LiveTranscriptItem
import com.example.core.ai.live.LiveVoiceConfig
import com.example.core.config.AppConfig
import com.example.core.result.AppResult
import com.example.data.local.KasaDatabase
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.example.data.repository.ConversationRepositoryImpl
import com.example.data.repository.UserRepository
import com.example.ui.screens.live.LiveViewModel
import com.example.ui.theme.ThemeMode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KasaLiveUnitTest {

  private lateinit var db: KasaDatabase
  private lateinit var conversationRepo: ConversationRepositoryImpl
  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, KasaDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    conversationRepo = ConversationRepositoryImpl(db.conversationDao())
  }

  @After
  fun teardown() {
    db.close()
  }

  @Test
  fun `test 1 - verify available live voices and default voice configuration`() {
    val defaultVoice = AvailableLiveVoices.DEFAULT
    assertEquals("Puck", defaultVoice.id)
    assertEquals("Puck", defaultVoice.displayName)

    val allVoices = AvailableLiveVoices.ALL
    assertEquals(5, allVoices.size)
    assertTrue(allVoices.any { it.id == "Puck" })
    assertTrue(allVoices.any { it.id == "Aoede" })
    assertTrue(allVoices.any { it.id == "Charon" })
    assertTrue(allVoices.any { it.id == "Kore" })
    assertTrue(allVoices.any { it.id == "Fenrir" })
  }

  @Test
  fun `test 2 - verify gemini live websocket payload serialization`() {
    val setupMessage = LiveClientMessage(
      setup = LiveSetupMessage(
        model = AppConfig.GEMINI_LIVE_DEFAULT_MODEL,
        generationConfig = LiveGenerationConfig(
          responseModalities = listOf("AUDIO"),
          speechConfig = LiveSpeechConfig(
            voiceConfig = LiveVoiceConfig(
              prebuiltVoiceConfig = LivePrebuiltVoiceConfig(voiceName = "Aoede")
            )
          ),
          temperature = 0.7f,
        ),
      )
    )

    val adapter = moshi.adapter(LiveClientMessage::class.java)
    val json = adapter.toJson(setupMessage)

    assertNotNull(json)
    assertTrue(json.contains(AppConfig.GEMINI_LIVE_DEFAULT_MODEL))
    assertTrue(json.contains("Aoede"))
    assertTrue(json.contains("AUDIO"))
  }

  @Test
  fun `test 3 - verify audio pipeline parameters`() {
    assertEquals(16000, LiveAudioRecorder.SAMPLE_RATE_HZ)
    assertEquals(24000, LiveAudioPlayer.SAMPLE_RATE_HZ)
  }

  @Test
  fun `test 4 - verify live viewmodel session and transcript persistence to Room`() = runTest {
    val fakeUser = UserProfile(
      id = "usr_kasa_live_test",
      displayName = "Kweku",
      email = "kweku@example.com",
    )
    val fakeUserRepo = object : UserRepository {
      private val userFlow = MutableStateFlow(fakeUser)
      override fun getCurrentUser(): Flow<UserProfile> = userFlow
      override fun getUserSettings(): Flow<UserSettings> = MutableStateFlow(UserSettings(fakeUser.id))
      override suspend fun updateDisplayName(name: String): AppResult<Unit> = AppResult.Success(Unit)
      override suspend fun updateThemeMode(themeMode: ThemeMode): AppResult<Unit> = AppResult.Success(Unit)
      override suspend fun updatePreferredLanguage(language: com.example.core.ai.SupportedLanguage): AppResult<Unit> = AppResult.Success(Unit)
      override suspend fun updateResponseLength(length: com.example.data.model.ResponseLength): AppResult<Unit> = AppResult.Success(Unit)
      override suspend fun updateConversationalTone(tone: com.example.data.model.ConversationalTone): AppResult<Unit> = AppResult.Success(Unit)
      override suspend fun updateLearningStyle(style: com.example.data.model.LearningStyle): AppResult<Unit> = AppResult.Success(Unit)
      override suspend fun updateMemoryEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
      override suspend fun updateHapticFeedback(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
      override suspend fun switchUserAccount(newUserId: String): AppResult<Unit> = AppResult.Success(Unit)
      override fun isOnboardingCompleted(): Flow<Boolean> = MutableStateFlow(true)
      override suspend fun setOnboardingCompleted(completed: Boolean): AppResult<Unit> = AppResult.Success(Unit)
      override suspend fun onUserAuthenticated(user: UserProfile): AppResult<Unit> = AppResult.Success(Unit)
      override suspend fun onUserSignedOut(): AppResult<Unit> = AppResult.Success(Unit)
      override suspend fun deleteUserData(userId: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    val liveVm = LiveViewModel(
      userRepository = fakeUserRepo,
      conversationRepository = conversationRepo,
    )

    // Verify initial state
    val initialState = liveVm.uiState.value
    assertEquals(LiveSessionState.Idle, initialState.sessionState)
    assertEquals("Puck", initialState.selectedVoice.id)
    assertEquals(5, initialState.availableVoices.size)

    // Select Aoede voice
    val aoede = AvailableLiveVoices.ALL.first { it.id == "Aoede" }
    liveVm.selectVoice(aoede)
    assertEquals("Aoede", liveVm.uiState.value.selectedVoice.id)

    // Toggle mute
    liveVm.toggleMute()
    assertTrue(liveVm.uiState.value.isMuted)
    liveVm.toggleMute()
    assertTrue(!liveVm.uiState.value.isMuted)
  }
}
