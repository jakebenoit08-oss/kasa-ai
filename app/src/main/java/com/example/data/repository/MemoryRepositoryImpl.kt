package com.example.data.repository

import com.example.core.error.AppError
import com.example.core.result.AppResult
import com.example.data.local.MemoryDao
import com.example.data.local.MemoryEntity
import com.example.data.model.memory.MemoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MemoryRepositoryImpl(
  private val memoryDao: MemoryDao,
) : MemoryRepository {

  override fun getMemories(userId: String): Flow<List<MemoryItem>> {
    return memoryDao.getMemoriesForUser(userId).map { entities ->
      entities.map { it.toDomain() }
    }
  }

  override suspend fun getActiveMemories(userId: String): AppResult<List<MemoryItem>> {
    return try {
      val entities = memoryDao.getActiveMemoriesForUserList(userId)
      AppResult.Success(entities.map { it.toDomain() })
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to fetch memories for user: ${e.message}", cause = e))
    }
  }

  override suspend fun saveMemory(userId: String, text: String): AppResult<MemoryItem> {
    val cleanText = text.trim()
    if (cleanText.isBlank()) {
      return AppResult.Error(AppError.ValidationError("Memory text cannot be empty."))
    }

    val memoryId = "mem_" + UUID.randomUUID().toString().take(12)
    val now = System.currentTimeMillis()
    val item = MemoryItem(
      id = memoryId,
      userId = userId,
      memoryText = cleanText,
      createdAt = now,
      updatedAt = now,
      isActive = true,
    )

    return try {
      memoryDao.insertMemory(MemoryEntity.fromDomain(item))
      AppResult.Success(item)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to save memory: ${e.message}", cause = e))
    }
  }

  override suspend fun deleteMemory(userId: String, memoryId: String): AppResult<Unit> {
    return try {
      val deleted = memoryDao.deleteMemoryById(userId, memoryId)
      if (deleted > 0) {
        AppResult.Success(Unit)
      } else {
        AppResult.Error(AppError.StorageError("Memory not found or already deleted."))
      }
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to delete memory: ${e.message}", cause = e))
    }
  }

  override suspend fun deleteAllMemories(userId: String): AppResult<Unit> {
    return try {
      memoryDao.deleteAllMemoriesForUser(userId)
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to delete all memories: ${e.message}", cause = e))
    }
  }

  override suspend fun countMemories(userId: String): AppResult<Int> {
    return try {
      val count = memoryDao.countMemoriesForUser(userId)
      AppResult.Success(count)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to count memories: ${e.message}", cause = e))
    }
  }
}
