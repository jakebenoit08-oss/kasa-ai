package com.example.data.repository

import com.example.core.error.AppError
import com.example.core.result.AppResult
import com.example.data.local.GeneratedImageDao
import com.example.data.local.GeneratedImageEntity
import com.example.data.model.GeneratedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class GeneratedImageRepositoryImpl(
  private val generatedImageDao: GeneratedImageDao,
) : GeneratedImageRepository {

  override fun getImagesForUser(userId: String): Flow<List<GeneratedImage>> {
    return generatedImageDao.getImagesForUser(userId).map { entities ->
      entities.map { it.toDomain() }
    }
  }

  override suspend fun getImageById(id: String): GeneratedImage? = withContext(Dispatchers.IO) {
    generatedImageDao.getImageById(id)?.toDomain()
  }

  override suspend fun saveImage(image: GeneratedImage): AppResult<Unit> = withContext(Dispatchers.IO) {
    try {
      generatedImageDao.insert(GeneratedImageEntity.fromDomain(image))
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to save generated image record", e))
    }
  }

  override suspend fun deleteImage(id: String): AppResult<Unit> = withContext(Dispatchers.IO) {
    try {
      val existing = generatedImageDao.getImageById(id)
      if (existing != null) {
        val file = File(existing.imagePath)
        if (file.exists()) {
          file.delete()
        }
        generatedImageDao.deleteById(id)
      }
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to delete generated image record", e))
    }
  }

  override suspend fun clearHistoryForUser(userId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
    try {
      generatedImageDao.deleteForUser(userId)
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to clear generation history", e))
    }
  }

  override suspend fun deleteHistoryForUser(userId: String): AppResult<Unit> {
    return clearHistoryForUser(userId)
  }
}
