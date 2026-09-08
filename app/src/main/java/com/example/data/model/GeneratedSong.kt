package com.example.data.model

data class GeneratedSong(
  val id: String,
  val userId: String,
  val title: String,
  val prompt: String,
  val audioUrl: String,
  val duration: Float = 120.0f,
  val imageUrl: String? = null,
  val tags: String? = null,
  val genre: String? = null,
  val language: String? = null,
  val isInstrumental: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
)

data class UserMusicCredits(
  val userId: String,
  val tier: String = "free",
  val used: Int = 0,
  val limit: Int = 1,
  val remaining: Int = 1,
  val periodStart: Long = 0L,
  val periodEnd: Long = 0L,
  val isUnlimitedDev: Boolean = false,
)
