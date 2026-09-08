package com.example.data.repository

import com.example.core.result.AppResult
import com.example.data.model.memory.MemoryItem
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
  fun getMemories(userId: String): Flow<List<MemoryItem>>
  suspend fun getActiveMemories(userId: String): AppResult<List<MemoryItem>>
  suspend fun saveMemory(userId: String, text: String): AppResult<MemoryItem>
  suspend fun deleteMemory(userId: String, memoryId: String): AppResult<Unit>
  suspend fun deleteAllMemories(userId: String): AppResult<Unit>
  suspend fun countMemories(userId: String): AppResult<Int>
}
