package com.example.data.model.search

/**
 * Result structure returned by SearchGroundingService.
 */
data class SearchGroundedResponse(
  val content: String,
  val sources: List<SearchSource> = emptyList(),
  val searchQueries: List<String> = emptyList(),
  val isGrounded: Boolean = false,
)
