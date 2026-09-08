package com.example.data.model

data class GeneratedImage(
  val id: String,
  val userId: String,
  val prompt: String,
  val refinedPrompt: String? = null,
  val imagePath: String,
  val mimeType: String = "image/jpeg",
  val aspectRatio: String = "1:1",
  val createdAt: Long = System.currentTimeMillis(),
  val width: Int = 1024,
  val height: Int = 1024,
)
