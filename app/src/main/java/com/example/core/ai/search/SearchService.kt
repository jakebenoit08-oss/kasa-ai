package com.example.core.ai.search

import com.example.core.ai.ChatTurn
import com.example.core.ai.SupportedLanguage
import com.example.core.result.AppResult
import com.example.data.model.search.SearchGroundedResponse

/**
 * Service contract for performing real-time web search and grounded answers.
 */
interface SearchService {

  /**
   * Executes web search grounding using Gemini and returns synthesized answers with verified source links.
   */
  suspend fun searchAndGenerate(
    userId: String,
    query: String,
    history: List<ChatTurn> = emptyList(),
    systemInstruction: String? = null,
    language: SupportedLanguage = SupportedLanguage.ENGLISH,
  ): AppResult<SearchGroundedResponse>
}
