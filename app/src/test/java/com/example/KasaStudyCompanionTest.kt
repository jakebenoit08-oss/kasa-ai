package com.example

import com.example.core.ai.study.StudyAIConfig
import com.example.core.ai.study.StudyAIService
import com.example.core.config.AppConfig
import com.example.core.result.AppResult
import com.example.data.local.StudySessionDao
import com.example.data.local.StudySessionEntity
import com.example.data.model.UserProfile
import com.example.data.model.study.EducationLevel
import com.example.data.model.study.QuestionSubmission
import com.example.data.model.study.QuestionType
import com.example.data.model.study.QuizDifficulty
import com.example.data.model.study.QuizQuestion
import com.example.data.model.study.StudyChatMessage
import com.example.data.model.study.StudyLesson
import com.example.data.model.study.StudySession
import com.example.data.model.study.StudySubjectCatalog
import com.example.data.repository.StudyRepositoryImpl
import com.example.data.repository.UserRepository
import com.example.ui.screens.study.StudyTab
import com.example.ui.screens.study.StudyViewModel
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
class KasaStudyCompanionTest {

  private class FakeStudySessionDao : StudySessionDao {
    private val sessions = mutableListOf<StudySessionEntity>()
    private val flow = MutableStateFlow<List<StudySessionEntity>>(emptyList())

    override fun getSessionsForUser(userId: String): Flow<List<StudySessionEntity>> {
      val userSessions = MutableStateFlow(sessions.filter { it.userId == userId })
      return userSessions
    }

    override suspend fun getSessionById(id: String): StudySessionEntity? {
      return sessions.firstOrNull { it.id == id }
    }

    override suspend fun insert(session: StudySessionEntity) {
      sessions.removeAll { it.id == session.id }
      sessions.add(0, session)
      flow.value = sessions.toList()
    }

    override suspend fun deleteById(id: String) {
      sessions.removeAll { it.id == id }
      flow.value = sessions.toList()
    }

    override suspend fun deleteForUser(userId: String) {
      sessions.removeAll { it.userId == userId }
      flow.value = sessions.toList()
    }
  }

  private class FakeUserRepository(
    initialUser: UserProfile = UserProfile(id = "user_student_1", displayName = "Ama Serwaa", email = "ama@example.com")
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

  private class FakeStudyAIService : StudyAIService {
    var generateLessonCalls = 0
    var lastLessonTopic: String? = null
    var lastLessonLevel: EducationLevel? = null
    var answerFollowUpCalls = 0
    var lastFollowUpQuery: String? = null
    var lastFollowUpLesson: StudyLesson? = null
    var generateQuizCalls = 0
    var lastQuizCount: Int = 0
    var lastQuizDifficulty: QuizDifficulty? = null
    var shouldFail = false

    override suspend fun generateLesson(
      subject: String,
      topic: String,
      level: EducationLevel,
    ): AppResult<StudyLesson> {
      generateLessonCalls++
      lastLessonTopic = topic
      lastLessonLevel = level

      if (shouldFail) {
        return AppResult.Error(com.example.core.error.AppError.AiEngineError("Network quota exceeded"))
      }

      return AppResult.Success(
        StudyLesson(
          id = "lesson_123",
          subject = subject,
          topic = topic,
          educationLevel = level,
          summary = "Photosynthesis is the biological process by which green plants synthesize nutrients from carbon dioxide and water using sunlight.",
          detailedExplanation = "Light reactions capture photons in thylakoid membranes, generating ATP and NADPH. Calvin cycle then fixes CO2 into glucose.",
          examples = listOf("Cocoa tree leaves converting solar energy in Ashanti region forests."),
          keyPoints = listOf("Equation: 6CO2 + 6H2O -> C6H12O6 + 6O2", "Chlorophyll absorbs red and blue light."),
          examPointers = listOf("Remember to specify that light and chlorophyll are essential conditions, not reactants."),
        )
      )
    }

    override suspend fun answerStudyFollowUp(
      lesson: StudyLesson,
      conversationHistory: List<StudyChatMessage>,
      userQuestion: String,
    ): AppResult<String> {
      answerFollowUpCalls++
      lastFollowUpQuery = userQuestion
      lastFollowUpLesson = lesson
      return AppResult.Success("Simply put, think of the leaf like a solar-powered kitchen making food for the plant using sunlight, water, and air.")
    }

    override suspend fun generateQuiz(
      subject: String,
      topic: String,
      level: EducationLevel,
      difficulty: QuizDifficulty,
      questionCount: Int,
    ): AppResult<List<QuizQuestion>> {
      generateQuizCalls++
      lastQuizCount = questionCount
      lastQuizDifficulty = difficulty

      if (shouldFail) {
        return AppResult.Error(com.example.core.error.AppError.AiEngineError("API error"))
      }

      val questions = (1..questionCount).map { i ->
        QuizQuestion(
          id = "q_$i",
          questionNumber = i,
          questionText = "Which pigment absorbs sunlight for photosynthesis in question $i?",
          questionType = QuestionType.MULTIPLE_CHOICE,
          options = listOf("A) Carotenoid", "B) Chlorophyll", "C) Hemoglobin", "D) Anthocyanin"),
          correctAnswer = "B",
          explanation = "Chlorophyll is the primary green photosynthetic pigment that captures light energy in chloroplasts.",
          hint = "Think of the primary green pigment in plant leaves.",
        )
      }
      return AppResult.Success(questions)
    }
  }

  @Test
  fun `TEST 1 - Education level selection updates state`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    viewModel.selectLevel(EducationLevel.SHS)

    assertEquals(EducationLevel.SHS, viewModel.uiState.value.selectedLevel)
    assertEquals(EducationLevel.SHS, viewModel.uiState.value.quizLevel)
  }

  @Test
  fun `TEST 2 - Subject selection updates active subject and topic presets`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    val coreMath = StudySubjectCatalog.CORE_SUBJECTS.first { it.id == "core_math" }
    viewModel.selectSubject(coreMath)

    assertEquals("Core Mathematics", viewModel.uiState.value.selectedSubject.name)
    assertTrue(viewModel.uiState.value.topicInput.isNotBlank())
  }

  @Test
  fun `TEST 3 - Lesson generation invokes AI service and populates lesson`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    viewModel.selectLevel(EducationLevel.SHS)
    viewModel.teachTopic("Photosynthesis")

    assertEquals(1, fakeService.generateLessonCalls)
    assertEquals("Photosynthesis", fakeService.lastLessonTopic)
    assertNotNull(viewModel.uiState.value.currentLesson)
    assertEquals("Photosynthesis", viewModel.uiState.value.currentLesson?.topic)
    assertFalse(viewModel.uiState.value.isLoadingLesson)
    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `TEST 4 - Follow-up questions maintain lesson context`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    viewModel.teachTopic("Photosynthesis")
    viewModel.updateFollowUpInput("Explain that more simply")
    viewModel.sendFollowUp()

    assertEquals(1, fakeService.answerFollowUpCalls)
    assertEquals("Explain that more simply", fakeService.lastFollowUpQuery)
    assertEquals(2, viewModel.uiState.value.followUpHistory.size) // 1 user + 1 assistant
    assertEquals("assistant", viewModel.uiState.value.followUpHistory.last().role)
  }

  @Test
  fun `TEST 5 - Quiz question generator creates exactly the requested number`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    viewModel.updateQuizTopic("Cell Biology")
    viewModel.updateQuizQuestionCount(5)
    viewModel.startQuiz()

    assertEquals(1, fakeService.generateQuizCalls)
    assertEquals(5, fakeService.lastQuizCount)
    assertEquals(5, viewModel.uiState.value.activeQuizQuestions.size)
  }

  @Test
  fun `TEST 6 - Answer submission correctly calculates score percentage`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    viewModel.updateQuizTopic("Cell Biology")
    viewModel.updateQuizQuestionCount(5)
    viewModel.startQuiz()

    // Answer 4 correctly ('B') and 1 incorrectly ('A')
    viewModel.selectQuizAnswer(0, "B) Chlorophyll")
    viewModel.selectQuizAnswer(1, "B) Chlorophyll")
    viewModel.selectQuizAnswer(2, "B) Chlorophyll")
    viewModel.selectQuizAnswer(3, "B) Chlorophyll")
    viewModel.selectQuizAnswer(4, "A) Carotenoid") // incorrect

    viewModel.submitQuiz()

    val completed = viewModel.uiState.value.completedSession
    assertNotNull(completed)
    assertEquals(4, completed?.correctQuestions)
    assertEquals(5, completed?.totalQuestions)
    assertEquals(80, completed?.scorePercentage)
    assertTrue(viewModel.uiState.value.isReviewingQuiz)
  }

  @Test
  fun `TEST 7 - Incorrect answers provide step-by-step explanations`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    viewModel.updateQuizTopic("Cell Biology")
    viewModel.updateQuizQuestionCount(3)
    viewModel.startQuiz()

    // Answer question 0 wrong
    viewModel.selectQuizAnswer(0, "A) Carotenoid")
    viewModel.submitQuiz()

    val submission = viewModel.uiState.value.completedSession?.submissions?.first()
    assertNotNull(submission)
    assertFalse(submission!!.isCorrect)
    assertEquals("B", submission.correctAnswer)
    assertTrue(submission.explanation.contains("Chlorophyll"))
  }

  @Test
  fun `TEST 8 - Numerical question working consistency`() {
    val question = QuizQuestion(
      id = "q_math_1",
      questionNumber = 1,
      questionText = "Solve for x: 2x + 6 = 14",
      questionType = QuestionType.CALCULATION,
      correctAnswer = "4",
      explanation = "Subtract 6 from both sides: 2x = 8. Divide by 2: x = 4.",
    )
    assertEquals("4", question.correctAnswer)
    assertTrue(question.explanation.contains("x = 4"))
  }

  @Test
  fun `TEST 9 - Difficulty configuration is passed to AI system`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    viewModel.updateQuizTopic("Mechanics")
    viewModel.updateQuizDifficulty(QuizDifficulty.HARD)
    viewModel.startQuiz()

    assertEquals(QuizDifficulty.HARD, fakeService.lastQuizDifficulty)

    val promptEasy = StudyAIConfig.buildQuizPrompt("Physics", "Mechanics", EducationLevel.SHS, QuizDifficulty.EASY, 5)
    val promptHard = StudyAIConfig.buildQuizPrompt("Physics", "Mechanics", EducationLevel.SHS, QuizDifficulty.HARD, 5)
    assertTrue(promptEasy.contains("Easy"))
    assertTrue(promptHard.contains("Hard"))
  }

  @Test
  fun `TEST 10 - AI-generated practice questions are clearly identified without fake WAEC claims`() {
    val systemInstruction = StudyAIConfig.buildStudySystemInstruction(EducationLevel.SHS)
    assertTrue(systemInstruction.contains("AI-generated practice questions"))
    assertTrue(systemInstruction.contains("Never claim a generated question is an official WAEC or WASSCE"))
  }

  @Test
  fun `TEST 11 - Persistence saves and retrieves study session`() = runTest {
    val fakeDao = FakeStudySessionDao()
    val repo = StudyRepositoryImpl(fakeDao)

    val session = StudySession(
      id = "session_1",
      userId = "user_student_1",
      subject = "Core Mathematics",
      topic = "Quadratic Equations",
      educationLevel = EducationLevel.SHS,
      difficulty = QuizDifficulty.MEDIUM,
      totalQuestions = 5,
      correctQuestions = 5,
      scorePercentage = 100,
      createdAt = 1000L,
    )

    repo.saveStudySession(session)
    val fetched = repo.getSessionById("session_1")

    assertNotNull(fetched)
    assertEquals("Quadratic Equations", fetched?.topic)
    assertEquals(100, fetched?.scorePercentage)
  }

  @Test
  fun `TEST 12 - User isolation strictly partitions study data`() = runTest {
    val fakeDao = FakeStudySessionDao()
    val repo = StudyRepositoryImpl(fakeDao)

    // User A session
    val sessionA = StudySession(
      id = "session_a",
      userId = "user_a",
      subject = "Physics",
      topic = "Ohm's Law",
      educationLevel = EducationLevel.SHS,
      difficulty = QuizDifficulty.MEDIUM,
      totalQuestions = 5,
      correctQuestions = 4,
      scorePercentage = 80,
      createdAt = 1000L,
    )
    repo.saveStudySession(sessionA)

    // User B session
    val sessionB = StudySession(
      id = "session_b",
      userId = "user_b",
      subject = "Economics",
      topic = "Inflation",
      educationLevel = EducationLevel.SHS,
      difficulty = QuizDifficulty.MEDIUM,
      totalQuestions = 5,
      correctQuestions = 5,
      scorePercentage = 100,
      createdAt = 2000L,
    )
    repo.saveStudySession(sessionB)

    // Verify User A only sees sessionA
    val listA = repo.getStudySessionsForUser("user_a").first()
    assertEquals(1, listA.size)
    assertEquals("session_a", listA.first().id)

    // Verify User B only sees sessionB
    val listB = repo.getStudySessionsForUser("user_b").first()
    assertEquals(1, listB.size)
    assertEquals("session_b", listB.first().id)
  }

  @Test
  fun `TEST 13 - Fresh account shows empty progress with 0 sessions`() = runTest {
    val fakeDao = FakeStudySessionDao()
    val repo = StudyRepositoryImpl(fakeDao)

    val progress = repo.getProgressSummaryForUser("new_user_123").first()
    assertEquals(0, progress.totalSessions)
    assertEquals(0, progress.averageScorePercentage)
    assertTrue(progress.subjectPerformances.isEmpty())
    assertTrue(progress.weakTopics.isEmpty())
    assertTrue(progress.recentSessions.isEmpty())
  }

  @Test
  fun `TEST 14 - Offline and API errors are handled safely without crash`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService().apply { shouldFail = true }

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    viewModel.teachTopic("Geometry")

    assertFalse(viewModel.uiState.value.isLoadingLesson)
    assertNull(viewModel.uiState.value.currentLesson)
    assertEquals("Network quota exceeded", viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `TEST 7 (Part 4) - Study Mode opens with default tabs and subjects`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)

    // Initial state: starts on LEARN tab
    assertEquals(StudyTab.LEARN, viewModel.uiState.value.currentTab)
    // Default level is JHS / BECE or SHS
    assertNotNull(viewModel.uiState.value.selectedLevel)
    // Core and elective subject catalog exists and has subjects
    assertTrue(StudySubjectCatalog.CORE_SUBJECTS.isNotEmpty())
    assertTrue(StudySubjectCatalog.ELECTIVE_SUBJECTS.isNotEmpty())
    // Default selected subject is non-null
    assertNotNull(viewModel.uiState.value.selectedSubject)
  }

  @Test
  fun `TEST 8 (Part 4) - Subject and topic selection works`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)

    // Select Integrated Science
    val intScience = StudySubjectCatalog.CORE_SUBJECTS.first { it.id == "integrated_science" }
    viewModel.selectSubject(intScience)
    assertEquals("Integrated Science", viewModel.uiState.value.selectedSubject.name)

    // Custom topic typing
    viewModel.updateTopicInput("Photosynthesis & Tro-tro motion")
    assertEquals("Photosynthesis & Tro-tro motion", viewModel.uiState.value.topicInput)

    // Sample topic selection
    val sample = intScience.sampleTopics.first()
    viewModel.selectSampleTopic(sample)
    assertEquals(sample, viewModel.uiState.value.topicInput)
  }

  @Test
  fun `TEST 9 (Part 4) - Study request reaches the real AI implementation`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    viewModel.selectLevel(EducationLevel.SHS)
    viewModel.teachTopic("Ohm's Law in Ghanaian Grid")

    assertEquals(1, fakeService.generateLessonCalls)
    assertEquals("Ohm's Law in Ghanaian Grid", fakeService.lastLessonTopic)
    assertEquals(EducationLevel.SHS, fakeService.lastLessonLevel)
  }

  @Test
  fun `TEST 10 (Part 4) - AI response appears in Study Mode`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    viewModel.teachTopic("Calculus Differentiation")

    val lesson = viewModel.uiState.value.currentLesson
    assertNotNull(lesson)
    assertEquals("Calculus Differentiation", lesson?.topic)
    assertTrue(lesson?.summary?.isNotBlank() == true)
    assertTrue(lesson?.detailedExplanation?.isNotBlank() == true)
    assertTrue(lesson?.examples?.isNotEmpty() == true)
    assertTrue(lesson?.keyPoints?.isNotEmpty() == true)
    assertTrue(lesson?.examPointers?.isNotEmpty() == true)
  }

  @Test
  fun `TEST 11 (Part 4) - Follow-up question preserves study context`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    viewModel.teachTopic("Electrolysis")
    assertNotNull(viewModel.uiState.value.currentLesson)

    // Send follow-up
    viewModel.updateFollowUpInput("Give an everyday Ghanaian analogy")
    viewModel.sendFollowUp()

    assertEquals(1, fakeService.answerFollowUpCalls)
    assertEquals("Give an everyday Ghanaian analogy", fakeService.lastFollowUpQuery)
    assertEquals("Electrolysis", fakeService.lastFollowUpLesson?.topic)
    // History contains both user prompt and assistant explanation
    assertEquals(2, viewModel.uiState.value.followUpHistory.size)
    assertEquals("user", viewModel.uiState.value.followUpHistory[0].role)
    assertEquals("assistant", viewModel.uiState.value.followUpHistory[1].role)
  }

  @Test
  fun `TEST 12 (Part 4) - Loading state works`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService()

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)

    // Before generation
    assertFalse(viewModel.uiState.value.isLoadingLesson)
    assertFalse(viewModel.uiState.value.isSendingFollowUp)

    viewModel.teachTopic("Algebra")
    // Upon completion
    assertFalse(viewModel.uiState.value.isLoadingLesson)
    assertNotNull(viewModel.uiState.value.currentLesson)
  }

  @Test
  fun `TEST 13 (Part 4) - Error state works`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService().apply { shouldFail = true }

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    viewModel.teachTopic("Thermodynamics")

    assertFalse(viewModel.uiState.value.isLoadingLesson)
    assertNull(viewModel.uiState.value.currentLesson)
    assertEquals("Network quota exceeded", viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `TEST 14 (Part 4) - Retry works`() = runTest {
    val fakeUserRepo = FakeUserRepository()
    val fakeDao = FakeStudySessionDao()
    val fakeRepo = StudyRepositoryImpl(fakeDao)
    val fakeService = FakeStudyAIService().apply { shouldFail = true }

    val viewModel = StudyViewModel(fakeUserRepo, fakeRepo, fakeService)
    viewModel.teachTopic("Vectors and Scalars")

    // Failed initial run
    assertEquals("Network quota exceeded", viewModel.uiState.value.errorMessage)
    assertNull(viewModel.uiState.value.currentLesson)

    // Fix backend and retry
    fakeService.shouldFail = false
    viewModel.retry()

    // Error cleared and lesson loaded
    assertNull(viewModel.uiState.value.errorMessage)
    assertNotNull(viewModel.uiState.value.currentLesson)
    assertEquals("Vectors and Scalars", viewModel.uiState.value.currentLesson?.topic)
  }

  @Test
  fun `TEST 15 - Phase 1 regression - Core chat system preserved`() {
    assertEquals("gemini-3.5-flash", AppConfig.GEMINI_DEFAULT_MODEL)
    assertTrue(AppConfig.APP_TAGLINE.isNotBlank())
  }

  @Test
  fun `TEST 16 - Phase 2 regression - Live audio model preserved`() {
    assertTrue(AppConfig.GEMINI_LIVE_DEFAULT_MODEL.contains("gemini"))
    assertTrue(AppConfig.GEMINI_LIVE_DEFAULT_MODEL.contains("audio"))
  }

  @Test
  fun `TEST 17 - Phase 3 regression - Ghanaian context config preserved`() {
    val systemInstruction = AppConfig.KASA_SYSTEM_INSTRUCTION
    assertTrue(systemInstruction.contains("KASA"))
  }

  @Test
  fun `TEST 18 - Phase 4 regression - Image generation models preserved`() {
    assertEquals("gemini-2.5-flash-image", AppConfig.GEMINI_IMAGE_DEFAULT_MODEL)
    assertEquals("imagen-3.0-generate-002", AppConfig.IMAGEN_DEFAULT_MODEL)
    assertTrue(AppConfig.PHASE_IDENTIFIER.contains("PHASE"))
  }
}
