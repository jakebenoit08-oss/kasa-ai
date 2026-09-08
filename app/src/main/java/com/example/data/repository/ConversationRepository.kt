package com.example.data.repository

import com.example.core.result.AppResult
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MessageDeliveryStatus
import com.example.data.model.search.SearchSource
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
  fun getConversations(userId: String): Flow<List<Conversation>>
  fun getMessages(userId: String, conversationId: String): Flow<List<ChatMessage>>
  suspend fun getMessagesList(userId: String, conversationId: String): List<ChatMessage>
  suspend fun createConversation(userId: String, title: String): AppResult<Conversation>
  suspend fun appendUserMessage(
    userId: String,
    conversationId: String,
    content: String,
  ): AppResult<ChatMessage>
  suspend fun appendAssistantMessage(
    userId: String,
    conversationId: String,
    content: String,
    status: MessageDeliveryStatus = MessageDeliveryStatus.DELIVERED_LOCAL,
    isWebSearch: Boolean = false,
    searchSources: List<SearchSource> = emptyList(),
  ): AppResult<ChatMessage>
  suspend fun updateMessageContent(
    userId: String,
    messageId: String,
    content: String,
    status: MessageDeliveryStatus,
    isWebSearch: Boolean = false,
    searchSources: List<SearchSource> = emptyList(),
  ): AppResult<Unit>
  suspend fun renameConversation(
    userId: String,
    conversationId: String,
    newTitle: String,
  ): AppResult<Unit>
  suspend fun deleteConversation(
    userId: String,
    conversationId: String,
  ): AppResult<Unit>
  suspend fun clearConversation(userId: String, conversationId: String): AppResult<Unit>
  suspend fun deleteAllConversationsForUser(userId: String): AppResult<Unit>
}

