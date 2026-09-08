package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.GeneratedImage

@Entity(
  tableName = "generated_images",
  indices = [Index(value = ["userId"]), Index(value = ["createdAt"])]
)
data class GeneratedImageEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val prompt: String,
  val refinedPrompt: String?,
  val imagePath: String,
  val mimeType: String,
  val aspectRatio: String,
  val createdAt: Long,
  val width: Int,
  val height: Int,
) {
  fun toDomain(): GeneratedImage {
    return GeneratedImage(
      id = id,
      userId = userId,
      prompt = prompt,
      refinedPrompt = refinedPrompt,
      imagePath = imagePath,
      mimeType = mimeType,
      aspectRatio = aspectRatio,
      createdAt = createdAt,
      width = width,
      height = height,
    )
  }

  companion object {
    fun fromDomain(image: GeneratedImage): GeneratedImageEntity {
      return GeneratedImageEntity(
        id = image.id,
        userId = image.userId,
        prompt = image.prompt,
        refinedPrompt = image.refinedPrompt,
        imagePath = image.imagePath,
        mimeType = image.mimeType,
        aspectRatio = image.aspectRatio,
        createdAt = image.createdAt,
        width = image.width,
        height = image.height,
      )
    }
  }
}
