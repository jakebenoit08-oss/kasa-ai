package com.example.data.model

import com.example.data.model.search.SearchSource

enum class MessageSender {
  USER,
  ASSISTANT,
  SYSTEM,
}

enum class MessageDeliveryStatus {
  SENDING,
  DELIVERED_LOCAL,
  FAILED,
}

data class ChatMessage(
  val id: String,
  val conversationId: String,
  val userId: String,
  val content: String,
  val sender: MessageSender,
  val timestamp: Long = System.currentTimeMillis(),
  val deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.DELIVERED_LOCAL,
  val isWebSearch: Boolean = false,
  val searchSources: List<SearchSource> = emptyList(),
)

data class Conversation(
  val id: String,
  val userId: String,
  val title: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val lastMessageSnippet: String? = null,
)
