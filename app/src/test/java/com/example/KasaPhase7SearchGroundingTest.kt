package com.example

import com.example.core.ai.search.SearchIntentDetector
import com.example.data.model.search.SearchGroundedResponse
import com.example.data.model.search.SearchSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KasaPhase7SearchGroundingTest {

  @Test
  fun searchIntentDetector_detectsCurrentRealTimeQueries() {
    val queries = listOf(
      "What is the current dollar to cedi exchange rate today?",
      "Who won the Ghana Premier League match yesterday?",
      "Latest news headlines in Accra right now",
      "What is the weather forecast for Kumasi tomorrow?",
      "Search the web for current fuel prices in Ghana",
      "What are the recent updates on Bank of Ghana policy rate?"
    )

    for (query in queries) {
      assertTrue(
        "Query '$query' should trigger search intent",
        SearchIntentDetector.shouldTriggerSearch(query)
      )
    }
  }

  @Test
  fun searchIntentDetector_ignoresStandardConversationalAndMathQueries() {
    val queries = listOf(
      "Hello, how are you doing?",
      "Explain Newton's second law of motion.",
      "Solve the quadratic equation x^2 + 5x + 6 = 0.",
      "Teach me how to say good morning in Twi.",
      "Write a short essay on photosynthesis."
    )

    for (query in queries) {
      assertFalse(
        "Query '$query' should not automatically trigger web search",
        SearchIntentDetector.shouldTriggerSearch(query)
      )
    }
  }

  @Test
  fun searchGroundedResponse_modelStructureIntegrity() {
    val sources = listOf(
      SearchSource(
        title = "Bank of Ghana Official Rates",
        url = "https://www.bog.gov.gh/rates",
        domain = "bog.gov.gh"
      ),
      SearchSource(
        title = "JoyNews Online",
        url = "https://www.myjoyonline.com/news",
        domain = "myjoyonline.com"
      )
    )

    val response = SearchGroundedResponse(
      content = "The current interbank exchange rate is approximately 1 USD to 15.5 GHS.",
      sources = sources,
      searchQueries = listOf("current dollar to cedi exchange rate Bank of Ghana"),
      isGrounded = true
    )

    assertEquals(2, response.sources.size)
    assertTrue(response.isGrounded)
    assertEquals("bog.gov.gh", response.sources[0].domain)
    assertEquals("https://www.bog.gov.gh/rates", response.sources[0].url)
  }
}
