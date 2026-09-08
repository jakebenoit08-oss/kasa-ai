package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.memory.MemoryItem

/**
 * Room entity representing a saved memory item.
 * 
 * Every record is strictly indexed and partitioned by [userId].
 */
@Entity(
  tableName = "memories",
  indices = [
    Index(value = ["user_id"]),
    Index(value = ["created_at"]),
  ]
)
data class MemoryEntity(
  @PrimaryKey
  @ColumnInfo(name = "id")
  val id: String,

  @ColumnInfo(name = "user_id")
  val userId: String,

  @ColumnInfo(name = "memory_text")
  val memoryText: String,

  @ColumnInfo(name = "created_at")
  val createdAt: Long,

  @ColumnInfo(name = "updated_at")
  val updatedAt: Long = createdAt,

  @ColumnInfo(name = "is_active")
  val isActive: Boolean = true,
) {
  fun toDomain(): MemoryItem = MemoryItem(
    id = id,
    userId = userId,
    memoryText = memoryText,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isActive = isActive,
  )

  companion object {
    fun fromDomain(domain: MemoryItem): MemoryEntity = MemoryEntity(
      id = domain.id,
      userId = domain.userId,
      memoryText = domain.memoryText,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt,
      isActive = domain.isActive,
    )
  }
}
