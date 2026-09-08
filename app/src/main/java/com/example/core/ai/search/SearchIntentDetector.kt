package com.example.core.ai.search

/**
 * Intelligent detector for identifying queries that require real-time web search
 * or explicit search grounding.
 */
object SearchIntentDetector {

  private val EXPLICIT_SEARCH_PREFIXES = listOf(
    "search the web for",
    "search the web",
    "search web for",
    "search web",
    "search for",
    "search",
    "google",
    "look up",
    "find the latest",
    "find current",
    "find news about",
    "find news on",
    "browse the web for",
    "check online for",
    "check the web for",
  )

  private val REAL_TIME_INDICATORS = listOf(
    "today",
    "tonight",
    "tomorrow",
    "yesterday",
    "right now",
    "latest news",
    "recent updates",
    "breaking news",
    "current price",
    "current rate",
    "exchange rate",
    "dollar to cedi",
    "cedi to dollar",
    "usd to ghs",
    "ghs to usd",
    "fuel price",
    "petrol price",
    "diesel price",
    "wassce results",
    "bece results",
    "cssps placement",
    "weather",
    "forecast",
    "weather today",
    "weather tomorrow",
    "election results",
    "sports score",
    "match yesterday",
    "black stars match",
    "premier league",
    "inflation rate in ghana",
    "bank of ghana policy rate",
    "bank of ghana",
    "happening now in ghana",
  )

  /**
   * Determines whether the user's message should trigger real-time web search grounding.
   */
  fun shouldTriggerSearch(query: String, searchModeEnabled: Boolean = false): Boolean {
    if (searchModeEnabled) return true
    val lower = query.trim().lowercase()
    if (lower.isBlank()) return false

    // Check if query starts with or explicitly contains direct search commands
    if (EXPLICIT_SEARCH_PREFIXES.any { prefix ->
        lower.startsWith(prefix) || lower.startsWith("/search") || lower.contains("search the web")
      }
    ) {
      return true
    }

    // Check for real-time keywords
    if (REAL_TIME_INDICATORS.any { indicator -> lower.contains(indicator) }) {
      return true
    }

    return false
  }

  /**
   * Extracts clean query keywords if the user entered explicit command prefixes.
   */
  fun sanitizeSearchQuery(query: String): String {
    var clean = query.trim()
    if (clean.startsWith("/search", ignoreCase = true)) {
      clean = clean.substring(7).trim()
    }
    for (prefix in EXPLICIT_SEARCH_PREFIXES) {
      if (clean.startsWith(prefix, ignoreCase = true)) {
        clean = clean.substring(prefix.length).trim().removePrefix(":").trim()
        break
      }
    }
    return clean.ifBlank { query.trim() }
  }
}
