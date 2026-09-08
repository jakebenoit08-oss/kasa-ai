package com.example.ui.screens.study

import com.example.data.model.study.EducationLevel
import com.example.data.model.study.QuizDifficulty
import com.example.data.model.study.QuizQuestion
import com.example.data.model.study.StudyChatMessage
import com.example.data.model.study.StudyLesson
import com.example.data.model.study.StudyProgressSummary
import com.example.data.model.study.StudySession
import com.example.data.model.study.StudySubject
import com.example.data.model.study.StudySubjectCatalog

enum class StudyTab(val displayName: String) {
  LEARN("Learn & Tutor"),
  QUIZ("Practice Quiz"),
  PROGRESS("Progress & Review"),
}

data class StudyUiState(
  val currentTab: StudyTab = StudyTab.LEARN,
  val selectedLevel: EducationLevel = EducationLevel.SHS,
  val selectedSubject: StudySubject = StudySubjectCatalog.CORE_SUBJECTS[0], // Core Math
  val topicInput: String = "",
  
  // Lesson state
  val currentLesson: StudyLesson? = null,
  val isLoadingLesson: Boolean = false,
  val followUpHistory: List<StudyChatMessage> = emptyList(),
  val followUpInput: String = "",
  val isSendingFollowUp: Boolean = false,

  // Quiz Setup & Active Quiz State
  val quizSubject: StudySubject = StudySubjectCatalog.CORE_SUBJECTS[0],
  val quizTopic: String = "",
  val quizLevel: EducationLevel = EducationLevel.SHS,
  val quizDifficulty: QuizDifficulty = QuizDifficulty.MEDIUM,
  val quizQuestionCount: Int = 5,
  val isGeneratingQuiz: Boolean = false,
  val activeQuizQuestions: List<QuizQuestion> = emptyList(),
  val currentQuestionIndex: Int = 0,
  val selectedAnswers: Map<Int, String> = emptyMap(), // questionIndex -> user's chosen option
  val revealedHints: Set<Int> = emptySet(),
  val completedSession: StudySession? = null,
  val isReviewingQuiz: Boolean = false,

  // Progress & History
  val progressSummary: StudyProgressSummary? = null,
  val selectedHistoricalSession: StudySession? = null,

  // Feedback & Status
  val errorMessage: String? = null,
  val snackbarMessage: String? = null,
  val isOffline: Boolean = false,
)
