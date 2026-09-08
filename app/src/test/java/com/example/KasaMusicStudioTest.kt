package com.example

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
    ): AppResult<GeneratedSong> {
      if (remainingCredits <= 0) {
        return AppResult.Error(AppError.ServiceUnavailable("You have used your music generation allowance for this subscription period."))
      }
      onStatusUpdate("Creating your song...")
      onStatusUpdate("Your song is being generated...")
      onStatusUpdate("Song ready 🎵")
      if (shouldSucceed) {
        val song = GeneratedSong(
          id = "song_123",
          userId = userId,
          title = title ?: "Accra Vibes",
          prompt = prompt,
          audioUrl = "https://example.com/audio/accra_vibes.mp3",
          duration = 125.0f,
          imageUrl = "https://example.com/img/accra.jpg",
          genre = genre ?: "Ghanaian Afrobeats",
          language = language ?: "English",
          isInstrumental = isInstrumental,
        )
        return AppResult.Success(song)
      } else {
        return AppResult.Error(AppError.AiEngineError("Generation failed on music backend."))
      }
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
    assertEquals("https://example.com/audio/accra_vibes.mp3", active?.audioUrl)
    assertEquals("Song ready 🎵", viewModel.uiState.value.musicStatusMessage)
    assertFalse(viewModel.uiState.value.isMusicGenerating)

    // Verify stored in repository
    val songInRepo = musicRepo.getSongById("song_123")
    assertNotNull(songInRepo)
    assertEquals("song_123", songInRepo?.id)
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
}
