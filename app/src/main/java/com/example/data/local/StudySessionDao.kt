package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

  @Query("SELECT * FROM study_sessions WHERE user_id = :userId ORDER BY created_at DESC")
  fun getSessionsForUser(userId: String): Flow<List<StudySessionEntity>>

  @Query("SELECT * FROM study_sessions WHERE id = :id LIMIT 1")
  suspend fun getSessionById(id: String): StudySessionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(session: StudySessionEntity)

  @Query("DELETE FROM study_sessions WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM study_sessions WHERE user_id = :userId")
  suspend fun deleteForUser(userId: String)
}
