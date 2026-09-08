package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.ChatMessage
import com.example.data.model.MessageDeliveryStatus
import com.example.data.model.MessageSender
import com.example.data.model.search.SearchSource
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(
  tableName = "chat_messages",
  indices = [
    Index(value = ["userId", "conversationId"]),
    Index(value = ["conversationId"])
  ]
)
data class ChatMessageEntity(
  @PrimaryKey val id: String,
  val conversationId: String,
  val userId: String,
  val content: String,
  val sender: String,
  val timestamp: Long,
  val deliveryStatus: String,
  @ColumnInfo(name = "isWebSearch", defaultValue = "0")
  val isWebSearch: Boolean = false,
  @ColumnInfo(name = "searchSourcesJson", defaultValue = "NULL")
  val searchSourcesJson: String? = null,
) {
  fun toModel(): ChatMessage {
    val parsedSources: List<SearchSource> = if (!searchSourcesJson.isNullOrBlank()) {
      try {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val listType = Types.newParameterizedType(List::class.java, SearchSource::class.java)
        val adapter = moshi.adapter<List<SearchSource>>(listType)
        adapter.fromJson(searchSourcesJson) ?: emptyList()
      } catch (e: Exception) {
        emptyList()
      }
    } else {
      emptyList()
    }

    return ChatMessage(
      id = id,
      conversationId = conversationId,
      userId = userId,
      content = content,
      sender = try { MessageSender.valueOf(sender) } catch (e: Exception) { MessageSender.USER },
      timestamp = timestamp,
      deliveryStatus = try { MessageDeliveryStatus.valueOf(deliveryStatus) } catch (e: Exception) { MessageDeliveryStatus.DELIVERED_LOCAL },
      isWebSearch = isWebSearch,
      searchSources = parsedSources,
    )
  }

  companion object {
    fun fromModel(model: ChatMessage): ChatMessageEntity {
      val sourcesJson = if (model.searchSources.isNotEmpty()) {
        try {
          val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
          val listType = Types.newParameterizedType(List::class.java, SearchSource::class.java)
          val adapter = moshi.adapter<List<SearchSource>>(listType)
          adapter.toJson(model.searchSources)
        } catch (e: Exception) {
          null
        }
      } else {
        null
      }

      return ChatMessageEntity(
        id = model.id,
        conversationId = model.conversationId,
        userId = model.userId,
        content = model.content,
        sender = model.sender.name,
        timestamp = model.timestamp,
        deliveryStatus = model.deliveryStatus.name,
        isWebSearch = model.isWebSearch,
        searchSourcesJson = sourcesJson,
      )
    }
  }
}
