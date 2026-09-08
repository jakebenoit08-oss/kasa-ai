package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Conversation

@Entity(
  tableName = "conversations",
  indices = [Index(value = ["userId"])]
)
data class ConversationEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val title: String,
  val createdAt: Long,
  val updatedAt: Long,
  val lastMessageSnippet: String? = null,
) {
  fun toModel(): Conversation = Conversation(
    id = id,
    userId = userId,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastMessageSnippet = lastMessageSnippet,
  )

  companion object {
    fun fromModel(model: Conversation): ConversationEntity = ConversationEntity(
      id = model.id,
      userId = model.userId,
      title = model.title,
      createdAt = model.createdAt,
      updatedAt = model.updatedAt,
      lastMessageSnippet = model.lastMessageSnippet,
    )
  }
}
