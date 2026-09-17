package com.example.core.ai.music

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MusicCreateBackendRequest(
  @Json(name = "prompt") val prompt: String,
  @Json(name = "gpt_description_prompt") val gptDescriptionPrompt: String? = null,
  @Json(name = "title") val title: String? = null,
  @Json(name = "tags") val tags: String? = null,
  @Json(name = "instrumental") val instrumental: Boolean = false,
  @Json(name = "userId") val userId: String? = null,
)

@JsonClass(generateAdapter = true)
data class MusicCreateBackendResponse(
  @Json(name = "taskId") val taskId: String? = null,
  @Json(name = "status") val status: String? = null,
  @Json(name = "message") val message: String? = null,
  @Json(name = "remainingCredits") val remainingCredits: Int? = null,
  @Json(name = "error") val error: String? = null,
)

@JsonClass(generateAdapter = true)
data class MusicClipBackendDto(
  @Json(name = "id") val id: String? = null,
  @Json(name = "title") val title: String? = null,
  @Json(name = "audioUrl") val audioUrl: String? = null,
  @Json(name = "duration") val duration: Float? = null,
  @Json(name = "imageUrl") val imageUrl: String? = null,
  @Json(name = "prompt") val prompt: String? = null,
  @Json(name = "tags") val tags: String? = null,
)

@JsonClass(generateAdapter = true)
data class MusicTaskBackendResponse(
  @Json(name = "status") val status: String? = null,
  @Json(name = "message") val message: String? = null,
  @Json(name = "clips") val clips: List<MusicClipBackendDto>? = null,
  @Json(name = "clip") val clip: MusicClipBackendDto? = null,
  @Json(name = "error") val error: String? = null,
) {
  fun getAllValidClips(): List<MusicClipBackendDto> {
    val result = mutableListOf<MusicClipBackendDto>()
    clips?.forEach { c ->
      if (!c.audioUrl.isNullOrBlank()) {
        result.add(c)
      }
    }
    // Backwards-compatible fallback: if clips list is absent or empty, check single clip
    if (result.isEmpty() && clip != null && !clip.audioUrl.isNullOrBlank()) {
      result.add(clip)
    }
    return result
  }
}

@JsonClass(generateAdapter = true)
data class MusicCreditsBackendResponse(
  @Json(name = "userId") val userId: String? = null,
  @Json(name = "tier") val tier: String? = null,
  @Json(name = "used") val used: Int? = null,
  @Json(name = "limit") val limit: Int? = null,
  @Json(name = "remaining") val remaining: Int? = null,
  @Json(name = "musicCredits") val musicCredits: Int? = null,
  @Json(name = "subscriptionStatus") val subscriptionStatus: String? = null,
  @Json(name = "periodStart") val periodStart: Long? = null,
  @Json(name = "periodEnd") val periodEnd: Long? = null,
  @Json(name = "isUnlimitedDev") val isUnlimitedDev: Boolean? = null,
  @Json(name = "isOwner") val isOwner: Boolean? = null,
  @Json(name = "error") val error: String? = null,
  @Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class BillingCheckoutBackendRequest(
  @Json(name = "planId") val planId: String,
)

@JsonClass(generateAdapter = true)
data class BillingCheckoutBackendResponse(
  @Json(name = "success") val success: Boolean? = null,
  @Json(name = "authorizationUrl") val authorizationUrl: String? = null,
  @Json(name = "reference") val reference: String? = null,
  @Json(name = "planId") val planId: String? = null,
  @Json(name = "error") val error: String? = null,
  @Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class BillingVerifySessionBackendRequest(
  @Json(name = "reference") val reference: String,
)

@JsonClass(generateAdapter = true)
data class BillingVerifySessionBackendResponse(
  @Json(name = "success") val success: Boolean? = null,
  @Json(name = "alreadyProcessed") val alreadyProcessed: Boolean? = null,
  @Json(name = "tier") val tier: String? = null,
  @Json(name = "subscriptionStatus") val subscriptionStatus: String? = null,
  @Json(name = "musicCredits") val musicCredits: Int? = null,
  @Json(name = "limit") val limit: Int? = null,
  @Json(name = "remaining") val remaining: Int? = null,
  @Json(name = "periodEnd") val periodEnd: Long? = null,
  @Json(name = "isOwner") val isOwner: Boolean? = null,
  @Json(name = "error") val error: String? = null,
  @Json(name = "message") val message: String? = null,
)
