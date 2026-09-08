package com.example.data.model.memory

/**
 * Domain model representing an explicit user-approved memory in KASA AI.
 * 
 * Every memory is strictly isolated by [userId].
 */
data class MemoryItem(
  val id: String,
  val userId: String,
  val memoryText: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val isActive: Boolean = true,
)
