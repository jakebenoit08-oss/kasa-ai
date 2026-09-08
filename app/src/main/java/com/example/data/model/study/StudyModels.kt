package com.example.data.model.study

enum class QuizDifficulty(
  val id: String,
  val displayName: String,
  val description: String,
) {
  EASY(
    id = "easy",
    displayName = "Easy",
    description = "Fundamental recall and basic definition questions.",
  ),
  MEDIUM(
    id = "medium",
    displayName = "Medium",
    description = "Standard academic rigor with application and problem-solving.",
  ),
  HARD(
    id = "hard",
    displayName = "Hard",
    description = "Challenging analytical and multi-step questions.",
  );

  companion object {
    fun fromId(id: String): QuizDifficulty {
      return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MEDIUM
    }
  }
}

enum class QuestionType {
  MULTIPLE_CHOICE,
  SHORT_ANSWER,
  CALCULATION,
}

data class QuizQuestion(
  val id: String,
  val questionNumber: Int,
  val questionText: String,
  val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
  val options: List<String> = emptyList(), // e.g. ["A) 4", "B) 8", "C) 12", "D) 16"]
  val correctAnswer: String, // e.g. "B" or "8" or the key option
  val explanation: String,
  val hint: String? = null,
)

data class QuestionSubmission(
  val questionId: String,
  val questionNumber: Int,
  val questionText: String,
  val selectedAnswer: String,
  val correctAnswer: String,
  val isCorrect: Boolean,
  val explanation: String,
)

data class StudyLesson(
  val id: String,
  val subject: String,
  val topic: String,
  val educationLevel: EducationLevel,
  val summary: String,
  val detailedExplanation: String,
  val examples: List<String>,
  val keyPoints: List<String>,
  val examPointers: List<String>,
  val suggestedFollowUps: List<String> = emptyList(),
)

data class StudyChatMessage(
  val id: String,
  val role: String, // "user" or "assistant"
  val content: String,
  val timestamp: Long = System.currentTimeMillis(),
)

data class StudySession(
  val id: String,
  val userId: String,
  val subject: String,
  val topic: String,
  val educationLevel: EducationLevel,
  val difficulty: QuizDifficulty,
  val totalQuestions: Int,
  val correctQuestions: Int,
  val scorePercentage: Int,
  val createdAt: Long,
  val submissions: List<QuestionSubmission> = emptyList(),
)

data class SubjectPerformance(
  val subjectName: String,
  val totalSessions: Int,
  val averageScorePercentage: Int,
  val lastStudiedAt: Long,
)

data class StudyProgressSummary(
  val totalSessions: Int,
  val averageScorePercentage: Int,
  val subjectPerformances: List<SubjectPerformance>,
  val weakTopics: List<String>, // Topics where score < 60%
  val recentSessions: List<StudySession>,
)
