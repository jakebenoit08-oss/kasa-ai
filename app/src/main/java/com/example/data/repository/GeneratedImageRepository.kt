package com.example.data.repository

import com.example.core.result.AppResult
import com.example.data.model.GeneratedImage
import kotlinx.coroutines.flow.Flow

interface GeneratedImageRepository {
  fun getImagesForUser(userId: String): Flow<List<GeneratedImage>>
  suspend fun getImageById(id: String): GeneratedImage?
  suspend fun saveImage(image: GeneratedImage): AppResult<Unit>
  suspend fun deleteImage(id: String): AppResult<Unit>
  suspend fun clearHistoryForUser(userId: String): AppResult<Unit>
  suspend fun deleteHistoryForUser(userId: String): AppResult<Unit>
}
