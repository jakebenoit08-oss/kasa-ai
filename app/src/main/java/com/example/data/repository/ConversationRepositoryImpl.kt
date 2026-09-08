package com.example.data.repository

import com.example.core.error.AppError
import com.example.core.result.AppResult
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MessageDeliveryStatus
import com.example.data.model.MessageSender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ConversationRepositoryImpl(
  private val conversationDao: ConversationDao,
) : ConversationRepository {

  override fun getConversations(userId: String): Flow<List<Conversation>> {
    return conversationDao.getConversationsByUser(userId).map { list ->
      list.map { it.toModel() }
    }
  }

  override fun getMessages(userId: String, conversationId: String): Flow<List<ChatMessage>> {
    return conversationDao.getMessages(userId, conversationId).map { list ->
      list.map { it.toModel() }
    }
  }

  override suspend fun getMessagesList(userId: String, conversationId: String): List<ChatMessage> {
    return conversationDao.getMessagesList(userId, conversationId).map { it.toModel() }
  }

  override suspend fun createConversation(userId: String, title: String): AppResult<Conversation> {
    return try {
      val now = System.currentTimeMillis()
      val newConv = Conversation(
        id = "conv_" + UUID.randomUUID().toString().take(8),
        userId = userId,
        title = title.ifBlank { "New Conversation" },
        createdAt = now,
        updatedAt = now,
      )
      conversationDao.insertConversation(ConversationEntity.fromModel(newConv))
      AppResult.Success(newConv)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to create conversation in database.", cause = e))
    }
  }

  override suspend fun appendUserMessage(
    userId: String,
    conversationId: String,
    content: String,
  ): AppResult<ChatMessage> {
    if (content.isBlank()) {
      return AppResult.Error(AppError.ValidationError("Message content cannot be empty."))
    }
    return try {
      val now = System.currentTimeMillis()
      val message = ChatMessage(
        id = "msg_" + UUID.randomUUID().toString().take(8),
        conversationId = conversationId,
        userId = userId,
        content = content.trim(),
        sender = MessageSender.USER,
        timestamp = now,
        deliveryStatus = MessageDeliveryStatus.DELIVERED_LOCAL,
      )

      conversationDao.insertMessage(ChatMessageEntity.fromModel(message))

      // Update snippet and updated timestamp
      val existingConv = conversationDao.getConversationById(userId, conversationId)
      if (existingConv != null) {
        val updated = existingConv.copy(
          updatedAt = now,
          lastMessageSnippet = content.take(60),
        )
        conversationDao.updateConversation(updated)
      }

      AppResult.Success(message)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to persist user message.", cause = e))
    }
  }

  override suspend fun appendAssistantMessage(
    userId: String,
    conversationId: String,
    content: String,
    status: MessageDeliveryStatus,
    isWebSearch: Boolean,
    searchSources: List<com.example.data.model.search.SearchSource>,
  ): AppResult<ChatMessage> {
    return try {
      val now = System.currentTimeMillis()
      val message = ChatMessage(
        id = "msg_" + UUID.randomUUID().toString().take(8),
        conversationId = conversationId,
        userId = userId,
        content = content,
        sender = MessageSender.ASSISTANT,
        timestamp = now,
        deliveryStatus = status,
        isWebSearch = isWebSearch,
        searchSources = searchSources,
      )

      conversationDao.insertMessage(ChatMessageEntity.fromModel(message))

      val existingConv = conversationDao.getConversationById(userId, conversationId)
      if (existingConv != null) {
        val updated = existingConv.copy(
          updatedAt = now,
          lastMessageSnippet = content.take(60),
        )
        conversationDao.updateConversation(updated)
      }

      AppResult.Success(message)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to persist assistant response.", cause = e))
    }
  }

  override suspend fun updateMessageContent(
    userId: String,
    messageId: String,
    content: String,
    status: MessageDeliveryStatus,
    isWebSearch: Boolean,
    searchSources: List<com.example.data.model.search.SearchSource>,
  ): AppResult<Unit> {
    return try {
      val sourcesJson = if (searchSources.isNotEmpty()) {
        try {
          val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
          val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.model.search.SearchSource::class.java)
          val adapter = moshi.adapter<List<com.example.data.model.search.SearchSource>>(listType)
          adapter.toJson(searchSources)
        } catch (e: Exception) {
          null
        }
      } else {
        null
      }

      conversationDao.updateMessageContentWithSearch(
        userId = userId,
        messageId = messageId,
        content = content,
        status = status.name,
        isWebSearch = isWebSearch,
        searchSourcesJson = sourcesJson,
      )
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to update message content.", cause = e))
    }
  }

  override suspend fun renameConversation(
    userId: String,
    conversationId: String,
    newTitle: String,
  ): AppResult<Unit> {
    if (newTitle.isBlank()) {
      return AppResult.Error(AppError.ValidationError("Conversation title cannot be empty."))
    }
    return try {
      conversationDao.renameConversation(
        userId = userId,
        conversationId = conversationId,
        newTitle = newTitle.trim(),
        updatedAt = System.currentTimeMillis(),
      )
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to rename conversation.", cause = e))
    }
  }

  override suspend fun deleteConversation(
    userId: String,
    conversationId: String,
  ): AppResult<Unit> {
    return try {
      conversationDao.deleteFullConversation(userId, conversationId)
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to delete conversation.", cause = e))
    }
  }

  override suspend fun clearConversation(userId: String, conversationId: String): AppResult<Unit> {
    return deleteConversation(userId, conversationId)
  }

  override suspend fun deleteAllConversationsForUser(userId: String): AppResult<Unit> {
    return try {
      conversationDao.deleteAllDataForUser(userId)
      AppResult.Success(Unit)
    } catch (e: Exception) {
      AppResult.Error(AppError.StorageError("Failed to delete user conversations.", cause = e))
    }
  }
}

