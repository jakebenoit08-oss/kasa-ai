package com.example.data.repository

import com.example.core.error.AppError
import com.example.core.result.AppResult
import com.example.data.local.StudySessionDao
import com.example.data.local.StudySessionEntity
import com.example.data.model.study.StudyProgressSummary
import com.example.data.model.study.StudySession
import com.example.data.model.study.SubjectPerformance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class StudyRepositoryImpl(
  private val studySessionDao: StudySessionDao,
) : StudyRepository {

  override fun getStudySessionsForUser(userId: String): Flow<List<StudySession>> {
    return studySessionDao.getSessionsForUser(userId).map { entities ->
      entities.map { it.toDomain() }
    }
  }

  override fun getProgressSummaryForUser(userId: String): Flow<StudyProgressSummary> {
    return studySessionDao.getSessionsForUser(userId).map { entities ->
      val sessions = entities.map { it.toDomain() }
      if (sessions.isEmpty()) {
        StudyProgressSummary(
          totalSessions = 0,
          averageScorePercentage = 0,
          subjectPerformances = emptyList(),
          weakTopics = emptyList(),
          recentSessions = emptyList(),
        )
      } else {
        val totalSessions = sessions.size
        val avgScore = sessions.map { it.scorePercentage }.average().toInt()

        // Calculate subject performance
        val subjectGroups = sessions.groupBy { it.subject }
        val subjectPerformances = subjectGroups.map { (subj, list) ->
          SubjectPerformance(
            subjectName = subj,
            totalSessions = list.size,
            averageScorePercentage = list.map { it.scorePercentage }.average().toInt(),
            lastStudiedAt = list.maxOf { it.createdAt },
          )
        }.sortedByDescending { it.totalSessions }

        // Find topics with average score < 60%
        val topicGroups = sessions.groupBy { it.topic }
        val weakTopics = topicGroups.filter { (_, topicSessions) ->
          val topicAvg = topicSessions.map { it.scorePercentage }.average()
          topicAvg < 60.0
        }.keys.toList()

        StudyProgressSummary(
          totalSessions = totalSessions,
          averageScorePercentage = avgScore,
          subjectPerformances = subjectPerformances,
          weakTopics = weakTopics,
          recentSessions = sessions.take(10),
        )
      }
    }
  }

  override suspend fun getSessionById(id: String): StudySession? = withContext(Dispatchers.IO) {
    studySessionDao.getSessionById(id)?.toDomain()
  }

  override suspend fun saveStudySession(session: StudySession): AppResult<Unit> = withContext(Dispatchers.IO) {
    try {
      studySessionDao.insert(StudySessionEntity.fromDomain(session))
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to save study session", e))
    }
  }

  override suspend fun deleteStudySession(id: String): AppResult<Unit> = withContext(Dispatchers.IO) {
    try {
      studySessionDao.deleteById(id)
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to delete study session", e))
    }
  }

  override suspend fun clearHistoryForUser(userId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
    try {
      studySessionDao.deleteForUser(userId)
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to clear study history", e))
    }
  }

  override suspend fun deleteSessionsForUser(userId: String): AppResult<Unit> {
    return clearHistoryForUser(userId)
  }
}
