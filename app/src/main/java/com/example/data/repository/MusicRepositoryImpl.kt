package com.example.data.repository

import com.example.core.error.AppError
import com.example.core.result.AppResult
import com.example.data.local.GeneratedSongDao
import com.example.data.local.GeneratedSongEntity
import com.example.data.model.GeneratedSong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MusicRepositoryImpl(
  private val songDao: GeneratedSongDao,
) : MusicRepository {

  override fun getSongsForUser(userId: String): Flow<List<GeneratedSong>> {
    return songDao.getSongsForUser(userId).map { entities ->
      entities.map { it.toDomain() }
    }
  }

  override suspend fun getSongById(id: String): GeneratedSong? {
    return songDao.getSongById(id)?.toDomain()
  }

  override suspend fun saveSong(song: GeneratedSong): AppResult<Unit> {
    return try {
      songDao.insert(GeneratedSongEntity.fromDomain(song))
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError(e.message ?: "Failed to save song", e))
    }
  }

  override suspend fun deleteSong(id: String): AppResult<Unit> {
    return try {
      songDao.deleteById(id)
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError(e.message ?: "Failed to delete song", e))
    }
  }

  override suspend fun deleteHistoryForUser(userId: String): AppResult<Unit> {
    return try {
      songDao.deleteForUser(userId)
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError(e.message ?: "Failed to clear history", e))
    }
  }
}

