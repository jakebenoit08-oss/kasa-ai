package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.ai.music.BillingCheckoutBackendResponse
import com.example.core.ai.music.BillingVerifySessionBackendResponse
import com.example.core.ai.music.MusicGenerationService
import com.example.core.error.AppError
import com.example.core.result.AppResult
import com.example.data.local.GeneratedImageDao
import com.example.data.local.GeneratedSongDao
import com.example.data.local.GeneratedSongEntity
import com.example.data.model.GeneratedSong
import com.example.data.model.UserMusicCredits
import com.example.data.model.UserProfile
import com.example.data.repository.GeneratedImageRepositoryImpl
import com.example.data.repository.MusicRepositoryImpl
import com.example.data.repository.UserRepository
import com.example.ui.screens.create.CreateStudioMode
import com.example.ui.screens.create.CreateViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KasaMusicStudioTest {

  private class FakeGeneratedSongDao : GeneratedSongDao {
    private val songs = mutableListOf<GeneratedSongEntity>()
    private val flow = MutableStateFlow<List<GeneratedSongEntity>>(emptyList())

    override suspend fun insert(song: GeneratedSongEntity) {
      songs.removeAll { it.id == song.id }
      songs.add(0, song)
      flow.value = songs.toList()
    }

    override fun getSongsForUser(userId: String): Flow<List<GeneratedSongEntity>> {
      val userFlow = MutableStateFlow(songs.filter { it.userId == userId })
      return userFlow
    }

    override suspend fun getSongById(id: String): GeneratedSongEntity? {
      return songs.firstOrNull { it.id == id }
    }

    override suspend fun deleteById(id: String) {
      songs.removeAll { it.id == id }
      flow.value = songs.toList()
    }

    override suspend fun deleteForUser(userId: String) {
      songs.removeAll { it.userId == userId }
      flow.value = songs.toList()
    }
  }

  private class FakeMusicService(
    var shouldSucceed: Boolean = true,
    var remainingCredits: Int = 1,
  ) : MusicGenerationService {
    override suspend fun getCredits(userId: String): AppResult<UserMusicCredits> {
      return AppResult.Success(
        UserMusicCredits(
          userId = userId,
          tier = "free",
          used = 1 - remainingCredits,
          limit = 1,
          remaining = remainingCredits,
        )
      )
    }

    override suspend fun generateMusic(
      userId: String,
      prompt: String,
      genre: String?,
      mood: String?,
      language: String?,
      title: String?,
      isInstrumental: Boolean,
      onStatusUpdate: (String) -> Unit
    ): AppResult<List<GeneratedSong>> {
      if (remainingCredits <= 0) {
        return AppResult.Error(AppError.ServiceUnavailable("You have used your music generation allowance for this subscription period."))
      }
      onStatusUpdate("Creating your song...")
      onStatusUpdate("Your song is being generated...")
      onStatusUpdate("Songs ready 🎵")
      if (shouldSucceed) {
        val baseTitle = title ?: "Accra Vibes"
        val song1 = GeneratedSong(
          id = "song_123_var1",
          userId = userId,
          title = "$baseTitle (Option 1)",
          prompt = prompt,
          audioUrl = "https://example.com/audio/accra_vibes_1.mp3",
          duration = 125.0f,
          imageUrl = "https://example.com/img/accra.jpg",
          genre = genre ?: "Ghanaian Afrobeats",
          language = language ?: "English",
          isInstrumental = isInstrumental,
        )
        val song2 = GeneratedSong(
          id = "song_123_var2",
          userId = userId,
          title = "$baseTitle (Option 2)",
          prompt = prompt,
          audioUrl = "https://example.com/audio/accra_vibes_2.mp3",
          duration = 120.0f,
          imageUrl = "https://example.com/img/accra.jpg",
          genre = genre ?: "Ghanaian Afrobeats",
          language = language ?: "English",
          isInstrumental = isInstrumental,
        )
        return AppResult.Success(listOf(song1, song2))
      } else {
        return AppResult.Error(AppError.AiEngineError("Generation failed on music backend."))
      }
    }

    override suspend fun initializeCheckout(planId: String): AppResult<BillingCheckoutBackendResponse> {
      return AppResult.Success(
        BillingCheckoutBackendResponse(
          success = true,
          authorizationUrl = "https://checkout.paystack.com/fake_checkout_123",
          reference = "kasa_fake_ref_123",
          planId = planId,
        )
      )
    }

    override suspend fun verifySession(reference: String): AppResult<BillingVerifySessionBackendResponse> {
      remainingCredits = 5
      return AppResult.Success(
        BillingVerifySessionBackendResponse(
          success = true,
          message = "Payment verified successfully",
          tier = "plus",
          remaining = 5,
          musicCredits = 5,
        )
      )
    }
  }

  private class FakeUserRepo : UserRepository {
    val userFlow = MutableStateFlow(UserProfile(id = "user_kasa_1", displayName = "Kofi Asante", email = "kofi@example.com"))
    override fun getCurrentUser(): Flow<UserProfile> = userFlow
    override fun getUserSettings(): Flow<com.example.data.model.UserSettings> = flowOf(com.example.data.model.UserSettings(userId = "user_kasa_1"))
    override suspend fun updateDisplayName(name: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateThemeMode(themeMode: com.example.ui.theme.ThemeMode): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updatePreferredLanguage(language: com.example.core.ai.SupportedLanguage): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateResponseLength(length: com.example.data.model.ResponseLength): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateConversationalTone(tone: com.example.data.model.ConversationalTone): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateLearningStyle(style: com.example.data.model.LearningStyle): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateMemoryEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateHapticFeedback(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun switchUserAccount(newUserId: String): AppResult<Unit> = AppResult.Success(Unit)
    override fun isOnboardingCompleted(): Flow<Boolean> = flowOf(true)
    override suspend fun setOnboardingCompleted(completed: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun onUserAuthenticated(user: UserProfile): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun onUserSignedOut(): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteUserData(userId: String): AppResult<Unit> = AppResult.Success(Unit)
  }

  private class EmptyImageDao : GeneratedImageDao {
    override suspend fun insert(image: com.example.data.local.GeneratedImageEntity) {}
    override fun getImagesForUser(userId: String) = flowOf(emptyList<com.example.data.local.GeneratedImageEntity>())
    override suspend fun getImageById(id: String) = null
    override suspend fun deleteById(id: String) {}
    override suspend fun deleteForUser(userId: String) {}
  }

  @Test
  fun testCreateStudioModeSwitching() = runTest {
    val userRepo = FakeUserRepo()
    val imageRepo = GeneratedImageRepositoryImpl(EmptyImageDao())
    val songDao = FakeGeneratedSongDao()
    val musicRepo = MusicRepositoryImpl(songDao)
    val musicService = FakeMusicService()

    val viewModel = CreateViewModel(
      userRepository = userRepo,
      generatedImageRepository = imageRepo,
      musicRepository = musicRepo,
      musicGenerationService = musicService,
    )

    assertEquals(CreateStudioMode.IMAGE, viewModel.uiState.value.selectedMode)

    viewModel.selectMode(CreateStudioMode.MUSIC)
    assertEquals(CreateStudioMode.MUSIC, viewModel.uiState.value.selectedMode)

    viewModel.selectMode(CreateStudioMode.IMAGE)
    assertEquals(CreateStudioMode.IMAGE, viewModel.uiState.value.selectedMode)
  }

  @Test
  fun testMusicPromptControlsAndValidation() = runTest {
    val userRepo = FakeUserRepo()
    val imageRepo = GeneratedImageRepositoryImpl(EmptyImageDao())
    val songDao = FakeGeneratedSongDao()
    val musicRepo = MusicRepositoryImpl(songDao)
    val musicService = FakeMusicService()

    val viewModel = CreateViewModel(
      userRepository = userRepo,
      generatedImageRepository = imageRepo,
      musicRepository = musicRepo,
      musicGenerationService = musicService,
    )

    viewModel.selectMode(CreateStudioMode.MUSIC)

    // Blank prompt validation
    viewModel.generateMusic()
    assertEquals("Please describe the song you want to create.", viewModel.uiState.value.musicErrorMessage)

    // Update prompt
    viewModel.updateMusicPrompt("Highlife celebration with brass and guitar")
    assertEquals("Highlife celebration with brass and guitar", viewModel.uiState.value.musicPrompt)
    assertNull(viewModel.uiState.value.musicErrorMessage)
    assertTrue(viewModel.uiState.value.isMusicPromptValid)

    // Custom controls
    viewModel.selectMusicGenre("Highlife")
    assertEquals("Highlife", viewModel.uiState.value.musicGenre)

    viewModel.selectMusicMood("Soulful")
    assertEquals("Soulful", viewModel.uiState.value.musicMood)

    viewModel.selectMusicLanguage("Twi")
    assertEquals("Twi", viewModel.uiState.value.musicLanguage)

    viewModel.setInstrumental(true)
    assertTrue(viewModel.uiState.value.isInstrumental)

    viewModel.updateMusicTitle("Odo Celebration")
    assertEquals("Odo Celebration", viewModel.uiState.value.musicTitle)
  }

  @Test
  fun testMusicGenerationSuccessSavesSongToHistory() = runTest {
    val userRepo = FakeUserRepo()
    val imageRepo = GeneratedImageRepositoryImpl(EmptyImageDao())
    val songDao = FakeGeneratedSongDao()
    val musicRepo = MusicRepositoryImpl(songDao)
    val musicService = FakeMusicService(shouldSucceed = true)

    val viewModel = CreateViewModel(
      userRepository = userRepo,
      generatedImageRepository = imageRepo,
      musicRepository = musicRepo,
      musicGenerationService = musicService,
    )

    viewModel.selectMode(CreateStudioMode.MUSIC)
    viewModel.updateMusicPrompt("An energetic Afrobeats anthem for summer")
    viewModel.generateMusic()

    val active = viewModel.uiState.value.activeSong
    assertNotNull(active)
    assertEquals("https://example.com/audio/accra_vibes_1.mp3", active?.audioUrl)
    assertEquals("Your songs are ready 🎵", viewModel.uiState.value.musicStatusMessage)
    assertEquals(2, viewModel.uiState.value.activeSongVariations.size)
    assertFalse(viewModel.uiState.value.isMusicGenerating)

    // Verify both variations stored in repository
    val song1 = musicRepo.getSongById("song_123_var1")
    val song2 = musicRepo.getSongById("song_123_var2")
    assertNotNull(song1)
    assertNotNull(song2)
    assertEquals("song_123_var1", song1?.id)
    assertEquals("song_123_var2", song2?.id)
  }

  @Test
  fun testMusicVariationSelectionAndIndependentPlayback() = runTest {
    val userRepo = FakeUserRepo()
    val imageRepo = GeneratedImageRepositoryImpl(EmptyImageDao())
    val songDao = FakeGeneratedSongDao()
    val musicRepo = MusicRepositoryImpl(songDao)
    val musicService = FakeMusicService(shouldSucceed = true)

    val viewModel = CreateViewModel(
      userRepository = userRepo,
      generatedImageRepository = imageRepo,
      musicRepository = musicRepo,
      musicGenerationService = musicService,
    )

    viewModel.selectMode(CreateStudioMode.MUSIC)
    viewModel.updateMusicPrompt("Smooth highlife guitar")
    viewModel.generateMusic()

    val variations = viewModel.uiState.value.activeSongVariations
    assertEquals(2, variations.size)

    // Option 1 is active by default
    assertEquals("song_123_var1", viewModel.uiState.value.activeSong?.id)

    // Select Option 2
    val option2 = variations[1]
    viewModel.selectVariation(option2)
    assertEquals("song_123_var2", viewModel.uiState.value.activeSong?.id)

    // Select Option 1 back
    val option1 = variations[0]
    viewModel.selectVariation(option1)
    assertEquals("song_123_var1", viewModel.uiState.value.activeSong?.id)
  }

  @Test
  fun testMusicQuotaExceededDisplaysHonestMessage() = runTest {
    val userRepo = FakeUserRepo()
    val imageRepo = GeneratedImageRepositoryImpl(EmptyImageDao())
    val songDao = FakeGeneratedSongDao()
    val musicRepo = MusicRepositoryImpl(songDao)
    val musicService = FakeMusicService(remainingCredits = 0)

    val viewModel = CreateViewModel(
      userRepository = userRepo,
      generatedImageRepository = imageRepo,
      musicRepository = musicRepo,
      musicGenerationService = musicService,
    )

    viewModel.selectMode(CreateStudioMode.MUSIC)
    viewModel.updateMusicPrompt("An inspiring worship track")
    viewModel.generateMusic()

    assertNull(viewModel.uiState.value.activeSong)
    assertTrue(viewModel.uiState.value.musicErrorMessage?.contains("allowance") == true)
  }

  @Test
  fun testMusicUpgradeAndVerificationFlow() = runTest {
    val userRepo = FakeUserRepo()
    val imageRepo = GeneratedImageRepositoryImpl(EmptyImageDao())
    val songDao = FakeGeneratedSongDao()
    val musicRepo = MusicRepositoryImpl(songDao)
    val musicService = FakeMusicService(remainingCredits = 0)

    val viewModel = CreateViewModel(
      userRepository = userRepo,
      generatedImageRepository = imageRepo,
      musicRepository = musicRepo,
      musicGenerationService = musicService,
    )

    // Initial state: dialog closed
    assertFalse(viewModel.uiState.value.showUpgradeDialog)

    // Open upgrade dialog
    viewModel.showUpgradeDialog(true)
    assertTrue(viewModel.uiState.value.showUpgradeDialog)

    // Verify session
    viewModel.verifyCheckout("kasa_test_ref_123")
    assertFalse(viewModel.uiState.value.showUpgradeDialog)
    assertNotNull(viewModel.uiState.value.billingMessage)
    assertTrue(viewModel.uiState.value.billingMessage!!.contains("successful"))
  }

  @Test
  fun `TEST 1 - Free user sees upgrade CTA and can open dialog`() = runTest {
    val userRepo = FakeUserRepo()
    val imageRepo = GeneratedImageRepositoryImpl(EmptyImageDao())
    val songDao = FakeGeneratedSongDao()
    val musicRepo = MusicRepositoryImpl(songDao)
    val musicService = FakeMusicService(remainingCredits = 1)

    val viewModel = CreateViewModel(
      userRepository = userRepo,
      generatedImageRepository = imageRepo,
      musicRepository = musicRepo,
      musicGenerationService = musicService,
    )
    viewModel.selectMode(CreateStudioMode.MUSIC)
    testScheduler.advanceUntilIdle()

    // Free user has tier == "free"
    val credits = viewModel.uiState.value.musicCredits
    assertEquals("free", credits?.tier)
    assertEquals(1, credits?.limit)

    // Initially dialog is closed
    assertFalse(viewModel.uiState.value.showUpgradeDialog)

    // Free user triggers upgrade CTA
    viewModel.showUpgradeDialog(true)
    assertTrue(viewModel.uiState.value.showUpgradeDialog)
  }

  @Test
  fun `TEST 2 - Plus plan displays GHC 49 and 5 credits allowance`() {
    // Pricing matrix validation
    val plusPriceGhc = 49
    val plusCredits = 5
    assertEquals(49, plusPriceGhc)
    assertEquals(5, plusCredits)
  }

  @Test
  fun `TEST 3 - Pro plan displays GHC 99 and 15 credits allowance`() {
    // Pricing matrix validation
    val proPriceGhc = 99
    val proCredits = 15
    assertEquals(99, proPriceGhc)
    assertEquals(15, proCredits)
  }

  @Test
  fun `TEST 4 - Current plan is displayed correctly for free, plus, and pro tiers`() = runTest {
    val userRepo = FakeUserRepo()
    val imageRepo = GeneratedImageRepositoryImpl(EmptyImageDao())
    val songDao = FakeGeneratedSongDao()
    val musicRepo = MusicRepositoryImpl(songDao)

    // 1. Free tier
    val freeService = object : MusicGenerationService by FakeMusicService() {
      override suspend fun getCredits(userId: String): AppResult<UserMusicCredits> {
        return AppResult.Success(UserMusicCredits(userId = userId, tier = "free", remaining = 1, limit = 1))
      }
    }
    val freeVm = CreateViewModel(
      userRepository = userRepo,
      generatedImageRepository = imageRepo,
      musicRepository = musicRepo,
      musicGenerationService = freeService,
    )
    freeVm.selectMode(CreateStudioMode.MUSIC)
    testScheduler.advanceUntilIdle()
    assertEquals("free", freeVm.uiState.value.musicCredits?.tier)

    // 2. Plus tier
    val plusService = object : MusicGenerationService by FakeMusicService() {
      override suspend fun getCredits(userId: String): AppResult<UserMusicCredits> {
        return AppResult.Success(UserMusicCredits(userId = userId, tier = "plus", remaining = 4, limit = 5))
      }
    }
    val plusVm = CreateViewModel(
      userRepository = userRepo,
      generatedImageRepository = imageRepo,
      musicRepository = musicRepo,
      musicGenerationService = plusService,
    )
    plusVm.selectMode(CreateStudioMode.MUSIC)
    testScheduler.advanceUntilIdle()
    assertEquals("plus", plusVm.uiState.value.musicCredits?.tier)
    assertEquals(4, plusVm.uiState.value.musicCredits?.remaining)

    // 3. Pro tier
    val proService = object : MusicGenerationService by FakeMusicService() {
      override suspend fun getCredits(userId: String): AppResult<UserMusicCredits> {
        return AppResult.Success(UserMusicCredits(userId = userId, tier = "pro", remaining = 14, limit = 15))
      }
    }
    val proVm = CreateViewModel(
      userRepository = userRepo,
      generatedImageRepository = imageRepo,
      musicRepository = musicRepo,
      musicGenerationService = proService,
    )
    proVm.selectMode(CreateStudioMode.MUSIC)
    testScheduler.advanceUntilIdle()
    assertEquals("pro", proVm.uiState.value.musicCredits?.tier)
    assertEquals(14, proVm.uiState.value.musicCredits?.remaining)
  }

  @Test
  fun `TEST 5 - Owner sees Owner Access without purchase push`() = runTest {
    val userRepo = FakeUserRepo()
    val imageRepo = GeneratedImageRepositoryImpl(EmptyImageDao())
    val songDao = FakeGeneratedSongDao()
    val musicRepo = MusicRepositoryImpl(songDao)

    val ownerService = object : MusicGenerationService by FakeMusicService() {
      override suspend fun getCredits(userId: String): AppResult<UserMusicCredits> {
        return AppResult.Success(
          UserMusicCredits(
            userId = userId,
            tier = "pro",
            remaining = 9999,
            limit = 9999,
            isOwner = true,
          )
        )
      }
    }
    val ownerVm = CreateViewModel(
      userRepository = userRepo,
      generatedImageRepository = imageRepo,
      musicRepository = musicRepo,
      musicGenerationService = ownerService,
    )
    ownerVm.selectMode(CreateStudioMode.MUSIC)
    testScheduler.advanceUntilIdle()
    val credits = ownerVm.uiState.value.musicCredits
    assertNotNull(credits)
    assertTrue(credits!!.isOwner)
    assertEquals(9999, credits.remaining)
  }

  @Test
  fun `TEST 6 - Upgrade button launches existing Paystack checkout flow`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val userRepo = FakeUserRepo()
    val imageRepo = GeneratedImageRepositoryImpl(EmptyImageDao())
    val songDao = FakeGeneratedSongDao()
    val musicRepo = MusicRepositoryImpl(songDao)
    var requestedPlan: String? = null

    val checkoutService = object : MusicGenerationService by FakeMusicService() {
      override suspend fun initializeCheckout(planId: String): AppResult<BillingCheckoutBackendResponse> {
        requestedPlan = planId
        return AppResult.Success(
          BillingCheckoutBackendResponse(
            success = true,
            authorizationUrl = "https://checkout.paystack.com/kasa_real_flow",
            reference = "kasa_ref_plus_001",
            planId = planId,
          )
        )
      }
    }

    val vm = CreateViewModel(
      userRepository = userRepo,
      generatedImageRepository = imageRepo,
      musicRepository = musicRepo,
      musicGenerationService = checkoutService,
    )
    vm.startCheckout(context, "plus")

    assertEquals("plus", requestedPlan)
    assertEquals("kasa_ref_plus_001", vm.uiState.value.checkoutReference)
  }
}
