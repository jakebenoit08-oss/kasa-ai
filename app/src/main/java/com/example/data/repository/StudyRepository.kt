package com.example.data.repository

import com.example.core.result.AppResult
import com.example.data.model.study.StudyProgressSummary
import com.example.data.model.study.StudySession
import kotlinx.coroutines.flow.Flow

interface StudyRepository {
  fun getStudySessionsForUser(userId: String): Flow<List<StudySession>>
  fun getProgressSummaryForUser(userId: String): Flow<StudyProgressSummary>
  suspend fun getSessionById(id: String): StudySession?
  suspend fun saveStudySession(session: StudySession): AppResult<Unit>
  suspend fun deleteStudySession(id: String): AppResult<Unit>
  suspend fun clearHistoryForUser(userId: String): AppResult<Unit>
  suspend fun deleteSessionsForUser(userId: String): AppResult<Unit>
}
