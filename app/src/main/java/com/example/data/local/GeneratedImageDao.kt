package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedImageDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(image: GeneratedImageEntity)

  @Query("SELECT * FROM generated_images WHERE userId = :userId ORDER BY createdAt DESC")
  fun getImagesForUser(userId: String): Flow<List<GeneratedImageEntity>>

  @Query("SELECT * FROM generated_images WHERE id = :id")
  suspend fun getImageById(id: String): GeneratedImageEntity?

  @Query("DELETE FROM generated_images WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM generated_images WHERE userId = :userId")
  suspend fun deleteForUser(userId: String)
}
