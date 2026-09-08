package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

  @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY updatedAt DESC")
  fun getConversationsByUser(userId: String): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE id = :conversationId AND userId = :userId LIMIT 1")
  suspend fun getConversationById(userId: String, conversationId: String): ConversationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConversation(conversation: ConversationEntity)

  @Update
  suspend fun updateConversation(conversation: ConversationEntity)

  @Query("UPDATE conversations SET title = :newTitle, updatedAt = :updatedAt WHERE id = :conversationId AND userId = :userId")
  suspend fun renameConversation(userId: String, conversationId: String, newTitle: String, updatedAt: Long)

  @Query("DELETE FROM conversations WHERE id = :conversationId AND userId = :userId")
  suspend fun deleteConversationOnly(userId: String, conversationId: String)

  @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId AND userId = :userId ORDER BY timestamp ASC")
  fun getMessages(userId: String, conversationId: String): Flow<List<ChatMessageEntity>>

  @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId AND userId = :userId ORDER BY timestamp ASC")
  suspend fun getMessagesList(userId: String, conversationId: String): List<ChatMessageEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: ChatMessageEntity)

  @Query("UPDATE chat_messages SET content = :content, deliveryStatus = :status WHERE id = :messageId AND userId = :userId")
  suspend fun updateMessageContent(userId: String, messageId: String, content: String, status: String)

  @Query("UPDATE chat_messages SET content = :content, deliveryStatus = :status, isWebSearch = :isWebSearch, searchSourcesJson = :searchSourcesJson WHERE id = :messageId AND userId = :userId")
  suspend fun updateMessageContentWithSearch(
    userId: String,
    messageId: String,
    content: String,
    status: String,
    isWebSearch: Boolean,
    searchSourcesJson: String?,
  )

  @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId AND userId = :userId")
  suspend fun deleteMessagesForConversation(userId: String, conversationId: String)

  @Query("DELETE FROM chat_messages WHERE userId = :userId")
  suspend fun deleteAllMessagesForUser(userId: String)

  @Query("DELETE FROM conversations WHERE userId = :userId")
  suspend fun deleteAllConversationsForUser(userId: String)

  @Transaction
  suspend fun deleteAllDataForUser(userId: String) {
    deleteAllMessagesForUser(userId)
    deleteAllConversationsForUser(userId)
  }

  @Transaction
  suspend fun deleteFullConversation(userId: String, conversationId: String) {
    deleteMessagesForConversation(userId, conversationId)
    deleteConversationOnly(userId, conversationId)
  }
}
