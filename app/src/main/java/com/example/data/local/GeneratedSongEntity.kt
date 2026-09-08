package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.GeneratedSong

@Entity(
  tableName = "generated_songs",
  indices = [Index(value = ["userId"]), Index(value = ["createdAt"])]
)
data class GeneratedSongEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val title: String,
  val prompt: String,
  val audioUrl: String,
  val duration: Float,
  val imageUrl: String?,
  val tags: String?,
  val genre: String?,
  val language: String?,
  val isInstrumental: Boolean,
  val createdAt: Long,
) {
  fun toDomain(): GeneratedSong {
    return GeneratedSong(
      id = id,
      userId = userId,
      title = title,
      prompt = prompt,
      audioUrl = audioUrl,
      duration = duration,
      imageUrl = imageUrl,
      tags = tags,
      genre = genre,
      language = language,
      isInstrumental = isInstrumental,
      createdAt = createdAt,
    )
  }

  companion object {
    fun fromDomain(song: GeneratedSong): GeneratedSongEntity {
      return GeneratedSongEntity(
        id = song.id,
        userId = song.userId,
        title = song.title,
        prompt = song.prompt,
        audioUrl = song.audioUrl,
        duration = song.duration,
        imageUrl = song.imageUrl,
        tags = song.tags,
        genre = song.genre,
        language = song.language,
        isInstrumental = song.isInstrumental,
        createdAt = song.createdAt,
      )
    }
  }
}
