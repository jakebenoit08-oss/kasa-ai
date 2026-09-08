package com.example.data.repository

import com.example.core.result.AppResult
import com.example.data.model.GeneratedSong
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
  fun getSongsForUser(userId: String): Flow<List<GeneratedSong>>
  suspend fun getSongById(id: String): GeneratedSong?
  suspend fun saveSong(song: GeneratedSong): AppResult<Unit>
  suspend fun deleteSong(id: String): AppResult<Unit>
  suspend fun deleteHistoryForUser(userId: String): AppResult<Unit>
}
