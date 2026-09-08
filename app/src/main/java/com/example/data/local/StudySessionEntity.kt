package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.study.EducationLevel
import com.example.data.model.study.QuestionSubmission
import com.example.data.model.study.QuizDifficulty
import com.example.data.model.study.StudySession
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(
  tableName = "study_sessions",
  indices = [
    Index(value = ["user_id"]),
    Index(value = ["created_at"]),
    Index(value = ["subject"]),
  ]
)
data class StudySessionEntity(
  @PrimaryKey
  @ColumnInfo(name = "id")
  val id: String,

  @ColumnInfo(name = "user_id")
  val userId: String,

  @ColumnInfo(name = "subject")
  val subject: String,

  @ColumnInfo(name = "topic")
  val topic: String,

  @ColumnInfo(name = "education_level")
  val educationLevel: String,

  @ColumnInfo(name = "difficulty")
  val difficulty: String,

  @ColumnInfo(name = "total_questions")
  val totalQuestions: Int,

  @ColumnInfo(name = "correct_questions")
  val correctQuestions: Int,

  @ColumnInfo(name = "score_percentage")
  val scorePercentage: Int,

  @ColumnInfo(name = "created_at")
  val createdAt: Long,

  @ColumnInfo(name = "submissions_json")
  val submissionsJson: String = "[]",
) {
  fun toDomain(): StudySession {
    val submissions = try {
      val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
      val listType = Types.newParameterizedType(List::class.java, QuestionSubmission::class.java)
      val adapter = moshi.adapter<List<QuestionSubmission>>(listType)
      adapter.fromJson(submissionsJson) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }

    return StudySession(
      id = id,
      userId = userId,
      subject = subject,
      topic = topic,
      educationLevel = EducationLevel.fromId(educationLevel),
      difficulty = QuizDifficulty.fromId(difficulty),
      totalQuestions = totalQuestions,
      correctQuestions = correctQuestions,
      scorePercentage = scorePercentage,
      createdAt = createdAt,
      submissions = submissions,
    )
  }

  companion object {
    fun fromDomain(domain: StudySession): StudySessionEntity {
      val submissionsJson = try {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val listType = Types.newParameterizedType(List::class.java, QuestionSubmission::class.java)
        val adapter = moshi.adapter<List<QuestionSubmission>>(listType)
        adapter.toJson(domain.submissions)
      } catch (e: Exception) {
        "[]"
      }

      return StudySessionEntity(
        id = domain.id,
        userId = domain.userId,
        subject = domain.subject,
        topic = domain.topic,
        educationLevel = domain.educationLevel.id,
        difficulty = domain.difficulty.id,
        totalQuestions = domain.totalQuestions,
        correctQuestions = domain.correctQuestions,
        scorePercentage = domain.scorePercentage,
        createdAt = domain.createdAt,
        submissionsJson = submissionsJson,
      )
    }
  }
}
