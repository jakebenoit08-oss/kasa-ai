package com.example.data.model.search

import com.squareup.moshi.JsonClass

/**
 * Representation of a verified external web source retrieved via Search Grounding.
 */
@JsonClass(generateAdapter = true)
data class SearchSource(
  val title: String,
  val url: String,
  val domain: String,
  val snippet: String? = null,
  val publicationDate: String? = null,
)
