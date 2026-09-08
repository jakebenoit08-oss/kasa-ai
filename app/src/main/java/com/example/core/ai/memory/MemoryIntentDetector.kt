package com.example.core.ai.memory

sealed class MemoryDetectionResult {
  data object None : MemoryDetectionResult()

  data class Candidate(
    val cleanSnippet: String,
    val rawTrigger: String,
  ) : MemoryDetectionResult()

  data class RejectedSensitive(
    val reason: String,
  ) : MemoryDetectionResult()
}

object MemoryIntentDetector {

  private val SENSITIVE_KEYWORDS = listOf(
    "password", "passcode", "pin code", "secret key", "api key", "apikey",
    "private key", "access token", "auth token", "credit card", "debit card",
    "cvv", "ccv", "bank account", "momo pin", "mobile money pin", "ssn",
    "social security", "ghana card pin"
  )

  private val TRIGGER_PREFIXES = listOf(
    "please remember that",
    "please remember this",
    "please remember",
    "remember that",
    "remember this",
    "remember i",
    "remember my",
    "remember",
    "don't forget that",
    "dont forget that",
    "don't forget",
    "dont forget",
    "keep this in mind:",
    "keep this in mind",
    "keep in mind that",
    "keep in mind",
    "save this preference:",
    "save this as a preference:",
    "save this preference",
    "save this as a preference",
  )

  /**
   * Scans a user's input to determine if they explicitly asked KASA to remember something.
   */
  fun scanForMemoryIntent(text: String): MemoryDetectionResult {
    val trimmed = text.trim()
    if (trimmed.length < 5) return MemoryDetectionResult.None

    val lower = trimmed.lowercase()

    // 1. Check for sensitive keyword attempt
    for (sensitive in SENSITIVE_KEYWORDS) {
      if (lower.contains(sensitive)) {
        // Only flag if it appears in conjunction with remembering or storage
        val hasMemoryIntent = TRIGGER_PREFIXES.any { lower.contains(it) } || lower.contains("save") || lower.contains("store")
        if (hasMemoryIntent) {
          return MemoryDetectionResult.RejectedSensitive(
            "For your security and privacy, sensitive credentials (passwords, PINs, bank accounts, or API keys) cannot be saved as memories."
          )
        }
      }
    }

    // 2. Check for explicit trigger prefixes
    for (prefix in TRIGGER_PREFIXES) {
      if (lower.startsWith(prefix)) {
        val snippet = trimmed.substring(prefix.length).trim().removePrefix(":").removePrefix(",").trim()
        if (snippet.isNotBlank()) {
          val cleanSnippet = sanitizeSnippet(snippet)
          if (cleanSnippet.isNotBlank()) {
            return MemoryDetectionResult.Candidate(
              cleanSnippet = cleanSnippet,
              rawTrigger = prefix,
            )
          }
        }
      }
    }

    // 3. Check for mid-sentence trigger if structured (e.g. "Also, please remember that I am studying for WASSCE")
    for (prefix in TRIGGER_PREFIXES) {
      val idx = lower.indexOf(prefix)
      if (idx > 0) {
        val snippet = trimmed.substring(idx + prefix.length).trim().removePrefix(":").removePrefix(",").trim()
        if (snippet.length >= 4) {
          val cleanSnippet = sanitizeSnippet(snippet)
          if (cleanSnippet.isNotBlank()) {
            return MemoryDetectionResult.Candidate(
              cleanSnippet = cleanSnippet,
              rawTrigger = prefix,
            )
          }
        }
      }
    }

    return MemoryDetectionResult.None
  }

  private fun sanitizeSnippet(raw: String): String {
    var s = raw.trim()
    // Remove trailing periods or quotes if unbalanced
    s = s.removeSurrounding("\"", "\"").removeSurrounding("'", "'").trim()
    // Ensure reasonable length cap (max 280 chars)
    if (s.length > 280) {
      s = s.take(280).trim() + "..."
    }
    return s
  }
}
