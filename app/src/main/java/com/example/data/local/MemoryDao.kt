package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

  @Query("SELECT * FROM memories WHERE user_id = :userId ORDER BY created_at DESC")
  fun getMemoriesForUser(userId: String): Flow<List<MemoryEntity>>

  @Query("SELECT * FROM memories WHERE user_id = :userId AND is_active = 1 ORDER BY created_at DESC")
  suspend fun getActiveMemoriesForUserList(userId: String): List<MemoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMemory(memory: MemoryEntity)

  @Query("DELETE FROM memories WHERE id = :id AND user_id = :userId")
  suspend fun deleteMemoryById(userId: String, id: String): Int

  @Query("DELETE FROM memories WHERE user_id = :userId")
  suspend fun deleteAllMemoriesForUser(userId: String): Int

  @Query("SELECT COUNT(*) FROM memories WHERE user_id = :userId")
  suspend fun countMemoriesForUser(userId: String): Int
}
