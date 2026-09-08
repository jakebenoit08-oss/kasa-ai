package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.ai.image.ImageGenerationService
import com.example.core.config.AppConfig
import com.example.core.result.AppResult
import com.example.data.local.GeneratedImageDao
import com.example.data.local.GeneratedImageEntity
import com.example.data.model.GeneratedImage
import com.example.data.model.UserProfile
import com.example.data.repository.GeneratedImageRepositoryImpl
import com.example.data.repository.UserRepository
import com.example.ui.screens.create.AspectRatioOption
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
class KasaCreateStudioTest {

  private class FakeGeneratedImageDao : GeneratedImageDao {
    private val images = mutableListOf<GeneratedImageEntity>()
    private val flow = MutableStateFlow<List<GeneratedImageEntity>>(emptyList())

    override suspend fun insert(image: GeneratedImageEntity) {
      images.removeAll { it.id == image.id }
      images.add(0, image)
      flow.value = images.toList()
    }

    override fun getImagesForUser(userId: String): Flow<List<GeneratedImageEntity>> {
      val userImagesFlow = MutableStateFlow(images.filter { it.userId == userId })
      return userImagesFlow
    }

    override suspend fun getImageById(id: String): GeneratedImageEntity? {
      return images.firstOrNull { it.id == id }
    }

    override suspend fun deleteById(id: String) {
      images.removeAll { it.id == id }
      flow.value = images.toList()
    }

    override suspend fun deleteForUser(userId: String) {
      images.removeAll { it.userId == userId }
      flow.value = images.toList()
    }
  }

  private class FakeUserRepository(
    initialUser: UserProfile = UserProfile(id = "user_a", displayName = "Kwame Mensah", email = "kwame@example.com")
  ) : UserRepository {
    val userFlow = MutableStateFlow(initialUser)
    override fun getCurrentUser(): Flow<UserProfile> = userFlow
    override fun getUserSettings(): Flow<com.example.data.model.UserSettings> = flowOf(com.example.data.model.UserSettings(userId = userFlow.value.id))
    override suspend fun updateDisplayName(name: String): AppResult<Unit> {
      userFlow.value = userFlow.value.copy(displayName = name)
      return AppResult.Success(Unit)
    }
    override suspend fun updateThemeMode(themeMode: com.example.ui.theme.ThemeMode): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updatePreferredLanguage(language: com.example.core.ai.SupportedLanguage): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateResponseLength(length: com.example.data.model.ResponseLength): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateConversationalTone(tone: com.example.data.model.ConversationalTone): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateLearningStyle(style: com.example.data.model.LearningStyle): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateMemoryEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateHapticFeedback(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun switchUserAccount(newUserId: String): AppResult<Unit> {
      userFlow.value = UserProfile(id = newUserId, displayName = "User $newUserId", email = "$newUserId@example.com")
      return AppResult.Success(Unit)
    }
    override fun isOnboardingCompleted(): Flow<Boolean> = flowOf(true)
    override suspend fun setOnboardingCompleted(completed: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun onUserAuthenticated(user: UserProfile): AppResult<Unit> {
      userFlow.value = user
      return AppResult.Success(Unit)
    }
    override suspend fun onUserSignedOut(): AppResult<Unit> {
      userFlow.value = UserProfile(id = "guest_default", displayName = "KASA Explorer", isLocalGuest = true)
      return AppResult.Success(Unit)
    }
    override suspend fun deleteUserData(userId: String): AppResult<Unit> {
      userFlow.value = UserProfile(id = "guest_default", displayName = "KASA Explorer", isLocalGuest = true)
      return AppResult.Success(Unit)
    }
  }

  private class FakeImageGenerationService : ImageGenerationService {
    var generateCallCount = 0
    var lastPromptReceived: String? = null
    var shouldFail = false

    override suspend fun generateImage(
      userId: String,
      prompt: String,
      aspectRatio: String,
    ): AppResult<GeneratedImage> {
      generateCallCount++
      lastPromptReceived = prompt

      if (shouldFail) {
        return AppResult.Error(com.example.core.error.AppError.AiEngineError("API Quota exceeded"))
      }

      return AppResult.Success(
        GeneratedImage(
          id = "gen_${System.currentTimeMillis()}",
          userId = userId,
          prompt = prompt,
          imagePath = "/dummy/path/img.jpg",
          aspectRatio = aspectRatio,
        )
      )
    }
  }

  @Test
  fun `test 1 - empty prompt does not trigger generation`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeGeneratedImageDao()
    val fakeImageRepo = GeneratedImageRepositoryImpl(fakeDao)
    val fakeService = FakeImageGenerationService()

    val viewModel = CreateViewModel(fakeUserRepo, fakeImageRepo, fakeService)
    viewModel.updatePrompt("   ")
    viewModel.generateImage()

    assertEquals(0, fakeService.generateCallCount)
    assertNotNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `test 2 - real image generation flow and state transition`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeGeneratedImageDao()
    val fakeImageRepo = GeneratedImageRepositoryImpl(fakeDao)
    val fakeService = FakeImageGenerationService()

    val viewModel = CreateViewModel(fakeUserRepo, fakeImageRepo, fakeService)
    viewModel.updatePrompt("Create a simple landscape at sunset.")
    viewModel.selectAspectRatio(AspectRatioOption.LANDSCAPE)
    viewModel.generateImage()

    assertEquals(1, fakeService.generateCallCount)
    assertEquals("Create a simple landscape at sunset.", fakeService.lastPromptReceived)
    assertNotNull(viewModel.uiState.value.activeImage)
    assertEquals("16:9", viewModel.uiState.value.activeImage?.aspectRatio)
    assertFalse(viewModel.uiState.value.isGenerating)
    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `test 3 - starter ideas populate prompt input`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeGeneratedImageDao()
    val fakeImageRepo = GeneratedImageRepositoryImpl(fakeDao)

    val viewModel = CreateViewModel(fakeUserRepo, fakeImageRepo)
    val starter = viewModel.starterPrompts.first()
    viewModel.selectStarterPrompt(starter)

    assertEquals(starter.prompt, viewModel.uiState.value.prompt)
    assertTrue(viewModel.uiState.value.isPromptValid)
  }

  @Test
  fun `test 4 - user isolation partitions image history`() = runTest {
    val fakeDao = FakeGeneratedImageDao()
    val fakeImageRepo = GeneratedImageRepositoryImpl(fakeDao)

    // Save image for User A
    val imageA = GeneratedImage(
      id = "img_1",
      userId = "user_a",
      prompt = "Futuristic Accra",
      imagePath = "/data/user_a_img1.jpg",
    )
    fakeImageRepo.saveImage(imageA)

    // Save image for User B
    val imageB = GeneratedImage(
      id = "img_2",
      userId = "user_b",
      prompt = "Kumasi Market",
      imagePath = "/data/user_b_img2.jpg",
    )
    fakeImageRepo.saveImage(imageB)

    // Verify User A only retrieves imageA
    val listA = fakeImageRepo.getImagesForUser("user_a").first()
    assertEquals(1, listA.size)
    assertEquals("img_1", listA.first().id)
    assertEquals("user_a", listA.first().userId)

    // Verify User B only retrieves imageB
    val listB = fakeImageRepo.getImagesForUser("user_b").first()
    assertEquals(1, listB.size)
    assertEquals("img_2", listB.first().id)
    assertEquals("user_b", listB.first().userId)
  }

  @Test
  fun `test 5 - api failure error state handling`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeGeneratedImageDao()
    val fakeImageRepo = GeneratedImageRepositoryImpl(fakeDao)
    val fakeService = FakeImageGenerationService().apply { shouldFail = true }

    val viewModel = CreateViewModel(fakeUserRepo, fakeImageRepo, fakeService)
    viewModel.updatePrompt("Cinematic sunset over Accra")
    viewModel.generateImage()

    assertFalse(viewModel.uiState.value.isGenerating)
    assertNull(viewModel.uiState.value.activeImage)
    assertEquals("API Quota exceeded", viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `test 6 - verify AppConfig constants for Phase 5`() {
    assertEquals("KASA AI", AppConfig.APP_NAME)
    assertEquals("AI that speaks your world.", AppConfig.APP_TAGLINE)
    assertTrue(AppConfig.PHASE_IDENTIFIER.contains("PHASE"))
  }

  @Test
  fun `test 7 - duplicate generation attempts are blocked by in-flight guard`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeGeneratedImageDao()
    val fakeImageRepo = GeneratedImageRepositoryImpl(fakeDao)
    val fakeService = FakeImageGenerationService()

    val viewModel = CreateViewModel(fakeUserRepo, fakeImageRepo, fakeService)
    viewModel.updatePrompt("Sunset over Cape Coast castle")

    // First generation succeeds
    viewModel.generateImage()
    assertEquals(1, fakeService.generateCallCount)
    assertNotNull(viewModel.uiState.value.activeImage)

    // Verify in-flight guard blocks redundant execution
    val field = CreateViewModel::class.java.getDeclaredField("isRequestInFlight")
    field.isAccessible = true
    field.set(viewModel, true)

    viewModel.generateImage()
    // Must remain 1 call
    assertEquals(1, fakeService.generateCallCount)

    // Reset lock
    field.set(viewModel, false)
  }

  @Test
  fun `test 8 - quota exceeded error is surfaced cleanly without automatic retry`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeGeneratedImageDao()
    val fakeImageRepo = GeneratedImageRepositoryImpl(fakeDao)
    val fakeService = FakeImageGenerationService().apply {
      shouldFail = true
    }

    val viewModel = CreateViewModel(fakeUserRepo, fakeImageRepo, fakeService)
    viewModel.updatePrompt("Futuristic African architecture")
    viewModel.generateImage()

    // Must fail cleanly with 1 attempt and clear message
    assertEquals(1, fakeService.generateCallCount)
    assertFalse(viewModel.uiState.value.isGenerating)
    assertEquals("API Quota exceeded", viewModel.uiState.value.errorMessage)
  }
}
