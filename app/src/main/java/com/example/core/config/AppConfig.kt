package com.example.core.config

import com.example.BuildConfig

/**
 * Centralized Application Configuration.
 * 
 * Safely inspects runtime configuration without leaking secrets or hardcoding credentials.
 * Gemini API keys are provided via the AI Studio Secrets panel / BuildConfig.
 */
object AppConfig {
  const val APP_NAME = "KASA AI"
  const val APP_TAGLINE = "AI that speaks your world."
  const val APP_VERSION = "1.8.0-phase8"
  const val PHASE_IDENTIFIER = "PHASE 8: AUTHENTICATION, ONBOARDING, SPLASH & BRAND IDENTITY"

  /**
   * SECURITY AUDIT & CREDENTIAL ARCHITECTURE:
   * 
   * Current Architecture:
   * - The Gemini API key is injected at build time from the AI Studio Secrets panel / .env into BuildConfig.GEMINI_API_KEY.
   * - Key is retrieved safely via reflection without hardcoding in repository sources.
   * - Key is NEVER logged, displayed, or exposed in UI states.
   * 
   * Security Disclosure & Honest Tradeoff:
   * - Because KASA operates as a client-side Android application with GH₵0 budget (no intermediate paid reverse-proxy server),
   *   embedding API keys into client binaries poses a risk of extraction via reverse-engineering (e.g. APK decompilation).
   * - For enterprise/production releases, key delegation should route through an authenticated backend proxy or Firebase AI.
   * - The client-side approach is appropriate and verified for local development, rapid prototyping, and non-commercial evaluation.
   */

  // Centralized Gemini Models
  const val GEMINI_DEFAULT_MODEL = "gemini-3.5-flash"
  const val GEMINI_IMAGE_DEFAULT_MODEL = "gemini-2.5-flash-image"
  const val IMAGEN_DEFAULT_MODEL = "imagen-3.0-generate-002"
  const val GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/"

  // KASA Secure Music Backend (Render Web Service)
  // =========================================================================
  // PRODUCTION RENDER BACKEND URL:
  // After deploying the /server directory to Render, replace the placeholder
  // below with your live Render HTTPS URL (e.g. "https://kasa-music-service.onrender.com/").
  // Can also be overridden dynamically at runtime in the app under Settings -> Music Studio Backend.
  const val MUSIC_BACKEND_PRODUCTION_URL = "https://YOUR-RENDER-SERVICE.onrender.com/"
  // =========================================================================

  // Local development presets for offline emulator and ADB reverse testing
  const val MUSIC_BACKEND_PORT = 8765
  const val MUSIC_BACKEND_EMULATOR_URL = "http://10.0.2.2:8765/"
  const val MUSIC_BACKEND_LOCALHOST_URL = "http://127.0.0.1:8765/"
  const val SONIC_MUSIC_DEFAULT_MODEL = "sonic-v5-5"
  const val PREF_MUSIC_BACKEND_URL_KEY = "kasa_music_backend_url"

  /**
   * Returns the active Music Backend URL.
   * Priority:
   * 1. User-customized URL in SharedPreferences (via SettingsScreen)
   * 2. MUSIC_BACKEND_PRODUCTION_URL (if configured with your real Render hostname)
   * 3. MUSIC_BACKEND_EMULATOR_URL (fallback for local emulator testing)
   */
  fun getMusicBackendUrl(context: android.content.Context? = null): String {
    if (context != null) {
      try {
        val prefs = context.getSharedPreferences("kasa_settings", android.content.Context.MODE_PRIVATE)
        val customUrl = prefs.getString(PREF_MUSIC_BACKEND_URL_KEY, null)?.trim()
        if (!customUrl.isNullOrEmpty()) {
          return if (customUrl.endsWith("/")) customUrl else "$customUrl/"
        }
      } catch (e: Exception) {
        // Fallback to default
      }
    }
    // If the production Render URL has been configured with a real hostname, use it
    if (!MUSIC_BACKEND_PRODUCTION_URL.contains("YOUR-RENDER-SERVICE") && MUSIC_BACKEND_PRODUCTION_URL.isNotBlank()) {
      return if (MUSIC_BACKEND_PRODUCTION_URL.endsWith("/")) MUSIC_BACKEND_PRODUCTION_URL else "$MUSIC_BACKEND_PRODUCTION_URL/"
    }
    return MUSIC_BACKEND_EMULATOR_URL
  }

  fun setMusicBackendUrl(context: android.content.Context, url: String) {
    try {
      val prefs = context.getSharedPreferences("kasa_settings", android.content.Context.MODE_PRIVATE)
      prefs.edit().putString(PREF_MUSIC_BACKEND_URL_KEY, url.trim()).apply()
    } catch (e: Exception) {
      // Ignore
    }
  }
  
  // Gemini Multimodal Live Bidirectional WebSocket API
  // Supported model for real-time audio conversation tasks
  const val GEMINI_LIVE_DEFAULT_MODEL = "models/gemini-2.5-flash-native-audio-preview-12-2025"
  const val GEMINI_LIVE_WS_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"

  // Firebase Google Sign-In Web Client ID (from google-services.json client_type 3)
  const val GOOGLE_WEB_CLIENT_ID = "897697409329-s389v85jtp4arvo35tst24vr4geo5he4.apps.googleusercontent.com"

  // System instructions are now centralized in com.example.core.ai.context.GhanaianContextConfig
  val KASA_SYSTEM_INSTRUCTION: String
    get() = com.example.core.ai.context.GhanaianContextConfig.buildSystemInstruction()

  val KASA_LIVE_SYSTEM_INSTRUCTION: String
    get() = com.example.core.ai.context.GhanaianContextConfig.buildLiveSystemInstruction()

  /**
   * Returns the injected Gemini API key from BuildConfig if valid, or empty string.
   */
  fun getGeminiApiKey(): String {
    return try {
      val field = BuildConfig::class.java.getDeclaredField("GEMINI_API_KEY")
      field.isAccessible = true
      val key = field.get(null) as? String
      if (key != null && key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
    } catch (e: Throwable) {
      ""
    }
  }

  /**
   * Safe status inspection for Gemini API key configuration.
   */
  fun isGeminiConfigured(): Boolean {
    val key = getGeminiApiKey()
    return key.isNotBlank()
  }

  fun isDebug(): Boolean = BuildConfig.DEBUG

  /**
   * Masked key info for settings/diagnostic display without exposing the secret.
   */
  fun getApiKeyStatus(): String {
    return if (isGeminiConfigured()) {
      "Active (${GEMINI_DEFAULT_MODEL})"
    } else {
      "No API key detected in Secrets"
    }
  }
}
