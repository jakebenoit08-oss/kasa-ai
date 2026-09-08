package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedSongDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(song: GeneratedSongEntity)

  @Query("SELECT * FROM generated_songs WHERE userId = :userId ORDER BY createdAt DESC")
  fun getSongsForUser(userId: String): Flow<List<GeneratedSongEntity>>

  @Query("SELECT * FROM generated_songs WHERE id = :id")
  suspend fun getSongById(id: String): GeneratedSongEntity?

  @Query("DELETE FROM generated_songs WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM generated_songs WHERE userId = :userId")
  suspend fun deleteForUser(userId: String)
}
