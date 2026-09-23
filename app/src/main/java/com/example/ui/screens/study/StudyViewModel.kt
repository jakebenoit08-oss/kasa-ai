package com.example.ui.screens.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.study.GeminiStudyAIService
import com.example.core.ai.study.StudyAIService
import com.example.core.result.AppResult
import com.example.data.model.UserProfile
import com.example.data.model.study.EducationLevel
import com.example.data.model.study.QuestionSubmission
import com.example.data.model.study.QuestionType
import com.example.data.model.study.QuizDifficulty
import com.example.data.model.study.StudyChatMessage
import com.example.data.model.study.StudyLesson
import com.example.data.model.study.StudyProgressSummary
import com.example.data.model.study.StudySession
import com.example.data.model.study.StudySubject
import com.example.data.model.study.StudySubjectCatalog
import com.example.data.repository.StudyRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class StudyViewModel(
  private val userRepository: UserRepository,
  private val studyRepository: StudyRepository,
  private val studyAIService: StudyAIService = GeminiStudyAIService(),
) : ViewModel() {

  private val _uiState = MutableStateFlow(StudyUiState())
  val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

  private var currentUser: UserProfile? = null
  private var progressCollectJob: Job? = null
  private var lastFailedOperation: (() -> Unit)? = null

  init {
    viewModelScope.launch {
      userRepository.getCurrentUser().collectLatest { user ->
        val userChanged = currentUser?.id != user.id
        currentUser = user
        if (userChanged || progressCollectJob == null) {
          observeUserProgress(user.id)
        }
      }
    }
  }

  private fun observeUserProgress(userId: String) {
    progressCollectJob?.cancel()
    progressCollectJob = viewModelScope.launch {
      studyRepository.getProgressSummaryForUser(userId).collectLatest { summary ->
        _uiState.update { it.copy(progressSummary = summary) }
      }
    }
  }

  fun selectTab(tab: StudyTab) {
    _uiState.update { it.copy(currentTab = tab) }
  }

  fun selectLevel(level: EducationLevel) {
    _uiState.update { 
      it.copy(
        selectedLevel = level,
        quizLevel = level,
      ) 
    }
  }

  fun selectSubject(subject: StudySubject) {
    val defaultTopic = subject.sampleTopics.firstOrNull() ?: ""
    _uiState.update { 
      it.copy(
        selectedSubject = subject,
        topicInput = defaultTopic,
      ) 
    }
  }

  fun updateTopicInput(topic: String) {
    _uiState.update { it.copy(topicInput = topic) }
  }

  fun selectSampleTopic(topic: String) {
    _uiState.update { it.copy(topicInput = topic) }
    teachTopic(topic)
  }

  fun teachTopic(customTopic: String? = null) {
    lastFailedOperation = { teachTopic(customTopic) }
    val topic = (customTopic ?: _uiState.value.topicInput).trim()
    if (topic.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Please enter or select a topic to study.") }
      return
    }

    val subject = _uiState.value.selectedSubject.name
    val level = _uiState.value.selectedLevel

    _uiState.update {
      it.copy(
        isLoadingLesson = true,
        errorMessage = null,
        followUpHistory = emptyList(),
        followUpInput = "",
      )
    }

    viewModelScope.launch {
      when (val result = studyAIService.generateLesson(subject, topic, level)) {
        is AppResult.Success -> {
          _uiState.update {
            it.copy(
              isLoadingLesson = false,
              currentLesson = result.data,
              errorMessage = null,
            )
          }
        }
        is AppResult.Error -> {
          _uiState.update {
            it.copy(
              isLoadingLesson = false,
              errorMessage = result.error.message,
            )
          }
        }
        is AppResult.Loading -> {
          _uiState.update { it.copy(isLoadingLesson = true) }
        }
      }
    }
  }

  fun updateFollowUpInput(text: String) {
    _uiState.update { it.copy(followUpInput = text) }
  }

  fun sendFollowUp(prefilledQuery: String? = null) {
    lastFailedOperation = { sendFollowUp(prefilledQuery) }
    val query = (prefilledQuery ?: _uiState.value.followUpInput).trim()
    val lesson = _uiState.value.currentLesson
    if (query.isBlank() || lesson == null) return

    val userMessage = StudyChatMessage(
      id = "msg_" + UUID.randomUUID().toString().take(8),
      role = "user",
      content = query,
    )

    val currentHistory = _uiState.value.followUpHistory + userMessage

    _uiState.update {
      it.copy(
        followUpHistory = currentHistory,
        followUpInput = "",
        isSendingFollowUp = true,
        errorMessage = null,
      )
    }

    viewModelScope.launch {
      when (val result = studyAIService.answerStudyFollowUp(lesson, currentHistory, query)) {
        is AppResult.Success -> {
          val aiMessage = StudyChatMessage(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            role = "assistant",
            content = result.data,
          )
          _uiState.update {
            it.copy(
              followUpHistory = it.followUpHistory + aiMessage,
              isSendingFollowUp = false,
            )
          }
        }
        is AppResult.Error -> {
          _uiState.update {
            it.copy(
              isSendingFollowUp = false,
              errorMessage = result.error.message,
            )
          }
        }
        is AppResult.Loading -> {
          _uiState.update { it.copy(isSendingFollowUp = true) }
        }
      }
    }
  }

  fun clearLesson() {
    _uiState.update {
      it.copy(
        currentLesson = null,
        followUpHistory = emptyList(),
        followUpInput = "",
      )
    }
  }

  fun launchQuizFromLesson() {
    val lesson = _uiState.value.currentLesson ?: return
    val subject = StudySubjectCatalog.findSubject(lesson.subject) ?: _uiState.value.selectedSubject

    _uiState.update {
      it.copy(
        currentTab = StudyTab.QUIZ,
        quizSubject = subject,
        quizTopic = lesson.topic,
        quizLevel = lesson.educationLevel,
        completedSession = null,
        isReviewingQuiz = false,
        selectedAnswers = emptyMap(),
        revealedHints = emptySet(),
        currentQuestionIndex = 0,
      )
    }

    startQuiz()
  }

  // Quiz Setup & Execution
  fun updateQuizSubject(subject: StudySubject) {
    _uiState.update { 
      it.copy(
        quizSubject = subject,
        quizTopic = subject.sampleTopics.firstOrNull() ?: "",
      ) 
    }
  }

  fun updateQuizTopic(topic: String) {
    _uiState.update { it.copy(quizTopic = topic) }
  }

  fun updateQuizLevel(level: EducationLevel) {
    _uiState.update { it.copy(quizLevel = level) }
  }

  fun updateQuizDifficulty(difficulty: QuizDifficulty) {
    _uiState.update { it.copy(quizDifficulty = difficulty) }
  }

  fun updateQuizQuestionCount(count: Int) {
    _uiState.update { it.copy(quizQuestionCount = count.coerceIn(3, 10)) }
  }

  fun startQuiz() {
    lastFailedOperation = { startQuiz() }
    val topic = _uiState.value.quizTopic.trim()
    if (topic.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Please specify a topic for the quiz.") }
      return
    }

    val subject = _uiState.value.quizSubject.name
    val level = _uiState.value.quizLevel
    val difficulty = _uiState.value.quizDifficulty
    val count = _uiState.value.quizQuestionCount

    _uiState.update {
      it.copy(
        isGeneratingQuiz = true,
        errorMessage = null,
        completedSession = null,
        isReviewingQuiz = false,
        selectedAnswers = emptyMap(),
        revealedHints = emptySet(),
        currentQuestionIndex = 0,
      )
    }

    viewModelScope.launch {
      when (val result = studyAIService.generateQuiz(subject, topic, level, difficulty, count)) {
        is AppResult.Success -> {
          _uiState.update {
            it.copy(
              isGeneratingQuiz = false,
              activeQuizQuestions = result.data,
              currentQuestionIndex = 0,
              errorMessage = null,
            )
          }
        }
        is AppResult.Error -> {
          _uiState.update {
            it.copy(
              isGeneratingQuiz = false,
              errorMessage = result.error.message,
            )
          }
        }
        is AppResult.Loading -> {
          _uiState.update { it.copy(isGeneratingQuiz = true) }
        }
      }
    }
  }

  fun selectQuizAnswer(questionIndex: Int, answer: String) {
    _uiState.update {
      val updated = it.selectedAnswers.toMutableMap()
      updated[questionIndex] = answer
      it.copy(selectedAnswers = updated)
    }
  }

  fun nextQuestion() {
    val maxIdx = _uiState.value.activeQuizQuestions.size - 1
    if (_uiState.value.currentQuestionIndex < maxIdx) {
      _uiState.update { it.copy(currentQuestionIndex = it.currentQuestionIndex + 1) }
    }
  }

  fun previousQuestion() {
    if (_uiState.value.currentQuestionIndex > 0) {
      _uiState.update { it.copy(currentQuestionIndex = it.currentQuestionIndex - 1) }
    }
  }

  fun revealHint(questionIndex: Int) {
    _uiState.update {
      it.copy(revealedHints = it.revealedHints + questionIndex)
    }
  }

  fun submitQuiz() {
    val questions = _uiState.value.activeQuizQuestions
    if (questions.isEmpty()) return

    val answers = _uiState.value.selectedAnswers
    val userId = currentUser?.id ?: "anonymous_user"

    var correctCount = 0
    val submissions = mutableListOf<QuestionSubmission>()

    for ((index, q) in questions.withIndex()) {
      val userAns = answers[index].orEmpty().trim()
      val correctAns = q.correctAnswer.trim()

      // Normalize comparison (e.g. check "B" against "B) ...", or exact string match)
      val isCorrect = if (userAns.isBlank()) {
        false
      } else if (q.questionType == QuestionType.MULTIPLE_CHOICE) {
        val userLetter = userAns.take(2).trimEnd(')', '.', ' ').uppercase()
        val correctLetter = correctAns.take(2).trimEnd(')', '.', ' ').uppercase()
        userLetter == correctLetter || userAns.equals(correctAns, ignoreCase = true) || userAns.startsWith(correctAns, ignoreCase = true)
      } else {
        userAns.equals(correctAns, ignoreCase = true)
      }

      if (isCorrect) {
        correctCount++
      }

      submissions.add(
        QuestionSubmission(
          questionId = q.id,
          questionNumber = q.questionNumber,
          questionText = q.questionText,
          selectedAnswer = if (userAns.isBlank()) "No answer selected" else userAns,
          correctAnswer = correctAns,
          isCorrect = isCorrect,
          explanation = q.explanation,
        )
      )
    }

    val total = questions.size
    val percentage = ((correctCount.toDouble() / total.toDouble()) * 100).toInt()

    val session = StudySession(
      id = "study_" + UUID.randomUUID().toString().take(8),
      userId = userId,
      subject = _uiState.value.quizSubject.name,
      topic = _uiState.value.quizTopic,
      educationLevel = _uiState.value.quizLevel,
      difficulty = _uiState.value.quizDifficulty,
      totalQuestions = total,
      correctQuestions = correctCount,
      scorePercentage = percentage,
      createdAt = System.currentTimeMillis(),
      submissions = submissions,
    )

    _uiState.update {
      it.copy(
        completedSession = session,
        isReviewingQuiz = true,
      )
    }

    // Persist to Room
    viewModelScope.launch {
      studyRepository.saveStudySession(session)
    }
  }

  fun retakeCurrentQuiz() {
    _uiState.update {
      it.copy(
        completedSession = null,
        isReviewingQuiz = false,
        selectedAnswers = emptyMap(),
        revealedHints = emptySet(),
        currentQuestionIndex = 0,
      )
    }
  }

  fun clearQuiz() {
    _uiState.update {
      it.copy(
        activeQuizQuestions = emptyList(),
        completedSession = null,
        isReviewingQuiz = false,
        selectedAnswers = emptyMap(),
        revealedHints = emptySet(),
        currentQuestionIndex = 0,
      )
    }
  }

  fun viewHistoricalSession(session: StudySession) {
    _uiState.update { it.copy(selectedHistoricalSession = session) }
  }

  fun clearHistoricalSession() {
    _uiState.update { it.copy(selectedHistoricalSession = null) }
  }

  fun deleteSession(sessionId: String) {
    viewModelScope.launch {
      studyRepository.deleteStudySession(sessionId)
      if (_uiState.value.selectedHistoricalSession?.id == sessionId) {
        _uiState.update { it.copy(selectedHistoricalSession = null) }
      }
    }
  }

  fun retry() {
    _uiState.update { it.copy(errorMessage = null) }
    lastFailedOperation?.invoke()
  }

  fun clearErrorMessage() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun clearSnackbarMessage() {
    _uiState.update { it.copy(snackbarMessage = null) }
  }
}
