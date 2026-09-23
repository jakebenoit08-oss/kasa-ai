package com.example.core.ai.study

import com.example.core.ai.gemini.GeminiApiService
import com.example.core.ai.gemini.GeminiContent
import com.example.core.ai.gemini.GeminiGenerateContentRequest
import com.example.core.ai.gemini.GeminiGenerationConfig
import com.example.core.ai.gemini.GeminiPart
import com.example.core.config.AppConfig
import com.example.core.error.AppError
import com.example.core.result.AppResult
import com.example.data.model.study.EducationLevel
import com.example.data.model.study.QuestionType
import com.example.data.model.study.QuizDifficulty
import com.example.data.model.study.QuizQuestion
import com.example.data.model.study.StudyChatMessage
import com.example.data.model.study.StudyLesson
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

interface StudyAIService {
  suspend fun generateLesson(
    subject: String,
    topic: String,
    level: EducationLevel,
  ): AppResult<StudyLesson>

  suspend fun answerStudyFollowUp(
    lesson: StudyLesson,
    conversationHistory: List<StudyChatMessage>,
    userQuestion: String,
  ): AppResult<String>

  suspend fun generateQuiz(
    subject: String,
    topic: String,
    level: EducationLevel,
    difficulty: QuizDifficulty,
    questionCount: Int,
  ): AppResult<List<QuizQuestion>>
}

// DTOs for JSON parsing of Gemini output
internal data class LessonJsonDto(
  val summary: String?,
  val detailedExplanation: String?,
  val examples: List<String>?,
  val keyPoints: List<String>?,
  val examPointers: List<String>?,
  val suggestedFollowUps: List<String>?,
)

internal data class QuizQuestionJsonDto(
  val questionNumber: Int?,
  val questionText: String?,
  val questionType: String?,
  val options: List<String>?,
  val correctAnswer: String?,
  val explanation: String?,
  val hint: String?,
)

internal data class QuizResponseJsonDto(
  val questions: List<QuizQuestionJsonDto>?,
)

class GeminiStudyAIService(
  private val modelName: String = AppConfig.GEMINI_DEFAULT_MODEL,
  private val customApiKeyProvider: (() -> String)? = null,
) : StudyAIService {

  private val moshi: Moshi by lazy {
    Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .build()
  }

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(60, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(60, TimeUnit.SECONDS)
      .build()
  }

  private val apiService: GeminiApiService by lazy {
    Retrofit.Builder()
      .baseUrl(AppConfig.GEMINI_API_BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(GeminiApiService::class.java)
  }

  private fun resolveApiKey(): String {
    val key = customApiKeyProvider?.invoke() ?: AppConfig.getGeminiApiKey()
    return key.trim()
  }

  private fun extractJsonPayload(raw: String): String {
    var cleaned = raw.trim()
    val codeBlockRegex = Regex("```(?:json)?([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
    val match = codeBlockRegex.find(cleaned)
    if (match != null) {
      cleaned = match.groupValues[1].trim()
    } else {
      val firstBrace = cleaned.indexOf('{')
      val lastBrace = cleaned.lastIndexOf('}')
      if (firstBrace != -1 && lastBrace > firstBrace) {
        cleaned = cleaned.substring(firstBrace, lastBrace + 1).trim()
      }
    }
    return cleaned
  }

  override suspend fun generateLesson(
    subject: String,
    topic: String,
    level: EducationLevel,
  ): AppResult<StudyLesson> = withContext(Dispatchers.IO) {
    val apiKey = resolveApiKey()
    if (apiKey.isBlank()) {
      return@withContext AppResult.Error(
        AppError.ConfigurationError("Gemini API key is missing. Please configure your API key in Google AI Studio Secrets.")
      )
    }

    try {
      val systemPrompt = StudyAIConfig.buildStudySystemInstruction(level)
      val userPrompt = StudyAIConfig.buildLessonPrompt(subject, topic, level)

      val request = GeminiGenerateContentRequest(
        contents = listOf(
          GeminiContent(
            role = "user",
            parts = listOf(GeminiPart(text = userPrompt)),
          )
        ),
        generationConfig = GeminiGenerationConfig(
          temperature = 0.5f,
          responseMimeType = "application/json",
        ),
        systemInstruction = GeminiContent(
          parts = listOf(GeminiPart(text = systemPrompt)),
        ),
      )

      val response = apiService.generateContent(
        model = modelName,
        apiKey = apiKey,
        request = request,
      )

      val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
      if (text.isNullOrBlank()) {
        return@withContext AppResult.Error(AppError.AiEngineError("No lesson content received from Gemini."))
      }

      val parsed: LessonJsonDto? = try {
        val json = extractJsonPayload(text)
        val adapter = moshi.adapter(LessonJsonDto::class.java)
        adapter.fromJson(json)
      } catch (e: Exception) {
        null
      }

      val lesson = if (parsed != null && (!parsed.detailedExplanation.isNullOrBlank() || !parsed.summary.isNullOrBlank())) {
        StudyLesson(
          id = "lesson_" + UUID.randomUUID().toString().take(8),
          subject = subject,
          topic = topic,
          educationLevel = level,
          summary = parsed.summary.orEmpty().ifBlank { "Core concepts and principles of $topic." },
          detailedExplanation = parsed.detailedExplanation.orEmpty().ifBlank { "Detailed explanation for $topic." },
          examples = parsed.examples.orEmpty().ifEmpty { listOf("Key practical application of $topic in Ghanaian context.") },
          keyPoints = parsed.keyPoints.orEmpty().ifEmpty { listOf("Master the fundamental definitions and core formulas.") },
          examPointers = parsed.examPointers.orEmpty().ifEmpty { listOf("Carefully read question requirements and verify units in final answers.") },
          suggestedFollowUps = parsed.suggestedFollowUps.orEmpty().ifEmpty {
            listOf("Make it simpler", "Give another example", "Explain step-by-step", "Test my understanding")
          },
        )
      } else {
        // Resilient fallback: Gemini returned conversational text or non-standard format
        val cleanText = text.replace("```json", "").replace("```", "").trim()
        val paragraphs = cleanText.split("\n\n").filter { it.isNotBlank() }
        val summaryText = paragraphs.firstOrNull()?.take(280) ?: "Key educational concepts for $topic in $subject."
        StudyLesson(
          id = "lesson_" + UUID.randomUUID().toString().take(8),
          subject = subject,
          topic = topic,
          educationLevel = level,
          summary = summaryText,
          detailedExplanation = cleanText,
          examples = listOf("Practical application of $topic in everyday life and problem solving."),
          keyPoints = listOf("Understand core definitions and underlying principles.", "Practice problem-solving step-by-step."),
          examPointers = listOf("Pay close attention to keywords, formulas, and units required in WAEC/BECE/WASSCE exams."),
          suggestedFollowUps = listOf("Make it simpler", "Give another example", "Explain step-by-step", "Test my understanding"),
        )
      }

      AppResult.Success(lesson)
    } catch (e: HttpException) {
      val errorMsg = when (e.code()) {
        429 -> "Gemini API rate limit reached. Please wait a moment before requesting another lesson."
        401, 403 -> "Authentication error with Gemini API. Check your API key in Secrets."
        else -> "Study AI service communication error (HTTP ${e.code()})."
      }
      AppResult.Error(AppError.AiEngineError(errorMsg, e))
    } catch (e: IOException) {
      AppResult.Error(AppError.NetworkError("Network connection error. Please check your connection.", e))
    } catch (e: Exception) {
      AppResult.Error(AppError.AiEngineError("Failed to generate lesson: ${e.localizedMessage}", e))
    }
  }

  override suspend fun answerStudyFollowUp(
    lesson: StudyLesson,
    conversationHistory: List<StudyChatMessage>,
    userQuestion: String,
  ): AppResult<String> = withContext(Dispatchers.IO) {
    val apiKey = resolveApiKey()
    if (apiKey.isBlank()) {
      return@withContext AppResult.Error(
        AppError.ConfigurationError("Gemini API key is missing. Please configure your API key in Secrets.")
      )
    }

    try {
      val systemPrompt = """
        ${StudyAIConfig.buildStudySystemInstruction(lesson.educationLevel)}
        CURRENT LESSON CONTEXT:
        - Subject: ${lesson.subject}
        - Topic: ${lesson.topic}
        - Level: ${lesson.educationLevel.displayName}
        - Lesson Summary: ${lesson.summary}

        Answer the student's follow-up question concisely, accurately, and encouragingly.
        If they ask for a simpler explanation, use an intuitive everyday analogy.
        If they ask for an example, show complete step-by-step working.
      """.trimIndent()

      val contentsList = mutableListOf<GeminiContent>()

      // Add conversation history
      for (turn in conversationHistory.takeLast(6)) {
        val role = if (turn.role == "assistant" || turn.role == "model") "model" else "user"
        contentsList.add(
          GeminiContent(
            role = role,
            parts = listOf(GeminiPart(text = turn.content)),
          )
        )
      }

      // Add current question
      contentsList.add(
        GeminiContent(
          role = "user",
          parts = listOf(GeminiPart(text = userQuestion)),
        )
      )

      val request = GeminiGenerateContentRequest(
        contents = contentsList,
        generationConfig = GeminiGenerationConfig(
          temperature = 0.6f,
        ),
        systemInstruction = GeminiContent(
          parts = listOf(GeminiPart(text = systemPrompt)),
        ),
      )

      val response = apiService.generateContent(
        model = modelName,
        apiKey = apiKey,
        request = request,
      )

      val answer = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
      if (answer.isNullOrBlank()) {
        return@withContext AppResult.Error(AppError.AiEngineError("No answer returned for your question."))
      }

      AppResult.Success(answer.trim())
    } catch (e: HttpException) {
      val errorMsg = when (e.code()) {
        429 -> "Gemini API rate limit reached. Please wait a moment."
        else -> "Study AI service communication error (HTTP ${e.code()})."
      }
      AppResult.Error(AppError.AiEngineError(errorMsg, e))
    } catch (e: IOException) {
      AppResult.Error(AppError.NetworkError("Network connection error. Please check your connection.", e))
    } catch (e: Exception) {
      AppResult.Error(AppError.AiEngineError("Error answering follow-up: ${e.localizedMessage}", e))
    }
  }

  override suspend fun generateQuiz(
    subject: String,
    topic: String,
    level: EducationLevel,
    difficulty: QuizDifficulty,
    questionCount: Int,
  ): AppResult<List<QuizQuestion>> = withContext(Dispatchers.IO) {
    val apiKey = resolveApiKey()
    if (apiKey.isBlank()) {
      return@withContext AppResult.Error(
        AppError.ConfigurationError("Gemini API key is missing. Please configure your API key in Secrets.")
      )
    }

    try {
      val systemPrompt = StudyAIConfig.buildStudySystemInstruction(level)
      val userPrompt = StudyAIConfig.buildQuizPrompt(subject, topic, level, difficulty, questionCount)

      val request = GeminiGenerateContentRequest(
        contents = listOf(
          GeminiContent(
            role = "user",
            parts = listOf(GeminiPart(text = userPrompt)),
          )
        ),
        generationConfig = GeminiGenerationConfig(
          temperature = 0.4f,
          responseMimeType = "application/json",
        ),
        systemInstruction = GeminiContent(
          parts = listOf(GeminiPart(text = systemPrompt)),
        ),
      )

      val response = apiService.generateContent(
        model = modelName,
        apiKey = apiKey,
        request = request,
      )

      val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
      if (text.isNullOrBlank()) {
        return@withContext AppResult.Error(AppError.AiEngineError("No quiz questions generated by the AI model."))
      }

      val parsed: QuizResponseJsonDto? = try {
        val json = extractJsonPayload(text)
        val adapter = moshi.adapter(QuizResponseJsonDto::class.java)
        adapter.fromJson(json)
      } catch (e: Exception) {
        null
      }
      if (parsed == null) {
        return@withContext AppResult.Error(AppError.AiEngineError("Failed to parse quiz questions from AI response."))
      }

      val rawQuestions = parsed.questions.orEmpty()
      if (rawQuestions.isEmpty()) {
        return@withContext AppResult.Error(AppError.AiEngineError("AI produced an empty set of quiz questions."))
      }

      // Validate questions structure
      val validQuestions = mutableListOf<QuizQuestion>()
      for ((idx, q) in rawQuestions.withIndex()) {
        val qText = q.questionText.orEmpty().trim()
        if (qText.isBlank()) continue

        val options = q.options.orEmpty().filter { it.isNotBlank() }
        val correct = q.correctAnswer.orEmpty().trim().ifBlank { "A" }
        val explanation = q.explanation.orEmpty().trim().ifBlank { "Correct answer is $correct." }

        validQuestions.add(
          QuizQuestion(
            id = "q_${idx + 1}_" + UUID.randomUUID().toString().take(6),
            questionNumber = idx + 1,
            questionText = qText,
            questionType = if (options.isNotEmpty()) QuestionType.MULTIPLE_CHOICE else QuestionType.SHORT_ANSWER,
            options = options,
            correctAnswer = correct,
            explanation = explanation,
            hint = q.hint?.takeIf { it.isNotBlank() },
          )
        )
      }

      if (validQuestions.isEmpty()) {
        return@withContext AppResult.Error(AppError.AiEngineError("No valid questions could be extracted."))
      }

      AppResult.Success(validQuestions)
    } catch (e: HttpException) {
      val errorMsg = when (e.code()) {
        429 -> "Gemini API rate limit reached. Please wait a moment before generating a quiz."
        else -> "Quiz generation error (HTTP ${e.code()})."
      }
      AppResult.Error(AppError.AiEngineError(errorMsg, e))
    } catch (e: IOException) {
      AppResult.Error(AppError.NetworkError("Network connection error while generating quiz.", e))
    } catch (e: Exception) {
      AppResult.Error(AppError.AiEngineError("Failed to generate quiz: ${e.localizedMessage}", e))
    }
  }
}
