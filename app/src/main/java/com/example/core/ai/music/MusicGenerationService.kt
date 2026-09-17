package com.example.core.ai.music

import android.content.Context
import android.util.Log
import com.example.core.auth.AuthService
import com.example.core.config.AppConfig
import com.example.core.error.AppError
import com.example.core.result.AppResult
import com.example.data.model.GeneratedSong
import com.example.data.model.UserMusicCredits
import com.example.data.repository.MusicRepository
import com.google.firebase.auth.FirebaseAuth
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

interface MusicGenerationService {
  suspend fun getCredits(userId: String): AppResult<UserMusicCredits>

  suspend fun generateMusic(
    userId: String,
    prompt: String,
    genre: String? = null,
    mood: String? = null,
    language: String? = null,
    title: String? = null,
    isInstrumental: Boolean = false,
    onStatusUpdate: (String) -> Unit = {}
  ): AppResult<List<GeneratedSong>>

  suspend fun initializeCheckout(planId: String): AppResult<BillingCheckoutBackendResponse>

  suspend fun verifySession(reference: String): AppResult<BillingVerifySessionBackendResponse>
}

class KasaMusicGenerationService(
  private val context: Context,
  private val musicRepository: MusicRepository,
  private val baseUrl: String = AppConfig.getMusicBackendUrl(context),
  private val authService: AuthService? = null,
) : MusicGenerationService {

  private val moshi: Moshi by lazy {
    Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .build()
  }

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(60, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(60, TimeUnit.SECONDS)
      .build()
  }

  private val apiService: MusicApiService by lazy {
    createApiService(baseUrl)
  }

  private fun createApiService(url: String): MusicApiService {
    val cleanUrl = if (url.endsWith("/")) url else "$url/"
    return Retrofit.Builder()
      .baseUrl(cleanUrl)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(MusicApiService::class.java)
  }

  /**
   * Resolves the current user's Firebase ID token and formats the Bearer Authorization header.
   */
  private suspend fun getAuthHeader(): String {
    // 1. Try provided authService instance if available
    try {
      val token = authService?.getIdToken()
      if (!token.isNullOrBlank()) {
        return "Bearer $token"
      }
    } catch (_: Throwable) {}

    // 2. Try direct Firebase Auth current user token
    try {
      val currentUser = FirebaseAuth.getInstance().currentUser
      if (currentUser != null) {
        val fbToken = suspendCancellableCoroutine<String?> { cont ->
          currentUser.getIdToken(false)
            .addOnSuccessListener { cont.resume(it.token) }
            .addOnFailureListener { cont.resume(null) }
            .addOnCanceledListener { cont.cancel() }
        }
        if (!fbToken.isNullOrBlank()) {
          return "Bearer $fbToken"
        }
      }
    } catch (_: Throwable) {}

    // 3. Fallback header for testing environments
    return "Bearer test_token_usr_default_kasa"
  }

  private fun classifyNetworkError(e: Throwable, targetUrl: String): AppError {
    val msg = e.message ?: ""
    val isConnectionRefused = e is java.net.ConnectException || msg.contains("Connection refused", ignoreCase = true)
    val isTimeout = e is java.net.SocketTimeoutException || msg.contains("timed out", ignoreCase = true)
    val isCleartextBlocked = msg.contains("CLEARTEXT", ignoreCase = true)
    val isUnknownHost = e is java.net.UnknownHostException

    val diagnosticMessage = when {
      isCleartextBlocked ->
        "Cleartext HTTP blocked by device policy ($targetUrl). Ensure backend uses HTTPS or cleartext is allowed."
      isConnectionRefused ->
        "Cannot reach KASA Music backend ($targetUrl). Please verify your Render service URL or local server status."
      isTimeout ->
        if (targetUrl.contains("onrender.com", ignoreCase = true)) {
          "Connection to KASA Music backend ($targetUrl) timed out. Render free-tier instances sleep after inactivity and can take up to 60 seconds to wake up. Please try again shortly."
        } else {
          "Connection to KASA Music backend ($targetUrl) timed out. Please verify your network connection and server response."
        }
      isUnknownHost ->
        "Cannot resolve KASA backend host ($targetUrl). Please verify the backend URL in Settings."
      else ->
        "Network connection error to KASA Music: ${e.localizedMessage ?: "Please check your connection and try again."}"
    }

    Log.e("KasaMusicService", "Network diagnostic: $diagnosticMessage", e)
    return AppError.NetworkError(diagnosticMessage, e)
  }

  private fun classifyHttpError(statusCode: Int, errorBody: String?): AppError {
    val body = errorBody ?: ""
    Log.w("KasaMusicService", "HTTP error received: code=$statusCode, body=$body")
    return when (statusCode) {
      400 -> AppError.ValidationError(
        if (body.contains("MISSING_PROMPT", ignoreCase = true)) "Please provide a description for the song."
        else "Invalid music generation request (HTTP 400). Please check your prompt."
      )
      401 -> AppError.Unauthorized(
        if (body.contains("UNAUTHORIZED", ignoreCase = true) || body.contains("INVALID_TOKEN", ignoreCase = true)) {
          "Please sign in to your KASA account to use AI Music."
        } else {
          "Music service authentication error (HTTP 401). Please verify your account."
        }
      )
      403 -> {
        if (body.contains("CREDIT_LIMIT_REACHED", ignoreCase = true) || body.contains("INSUFFICIENT_CREDITS", ignoreCase = true)) {
          AppError.ServiceUnavailable("You have 0 KASA Music Credits remaining for this cycle.")
        } else if (body.contains("FORBIDDEN", ignoreCase = true)) {
          AppError.Unauthorized("You do not have permission to access this music task.")
        } else {
          AppError.ServiceUnavailable("Music generation request was rejected by service (HTTP 403).")
        }
      }
      429 -> AppError.ServiceUnavailable(
        "Music service rate limit reached (HTTP 429). Please wait a moment before trying again."
      )
      500 -> AppError.AiEngineError(
        "Music engine encountered an internal server error (HTTP 500). Please try again shortly."
      )
      502 -> AppError.AiEngineError(
        "Bad gateway from music provider (HTTP 502). Please try again shortly."
      )
      503 -> {
        if (body.contains("BACKEND_NOT_CONFIGURED", ignoreCase = true)) {
          AppError.ConfigurationError(
            "AIMusicAPI key is not configured on the KASA backend. Please configure AIMUSIC_API_KEY in the backend environment or .env file."
          )
        } else {
          AppError.ServiceUnavailable("KASA Music backend is temporarily unavailable (HTTP 503). Please try again later.")
        }
      }
      else -> AppError.AiEngineError("Music generation failed with HTTP $statusCode. Please try again.")
    }
  }

  override suspend fun getCredits(userId: String): AppResult<UserMusicCredits> = withContext(Dispatchers.IO) {
    try {
      val authHeader = getAuthHeader()
      val response = try {
        apiService.getCredits(authHeader = authHeader, userId = userId)
      } catch (e: IOException) {
        // Try fallback to localhost (if testing in unit test / desktop JVM / robolectric or adb reverse)
        if (baseUrl == AppConfig.MUSIC_BACKEND_EMULATOR_URL) {
          try {
            createApiService(AppConfig.MUSIC_BACKEND_LOCALHOST_URL).getCredits(authHeader = authHeader, userId = userId)
          } catch (e2: Exception) {
            return@withContext AppResult.Error(classifyNetworkError(e2, AppConfig.MUSIC_BACKEND_LOCALHOST_URL))
          }
        } else {
          return@withContext AppResult.Error(classifyNetworkError(e, baseUrl))
        }
      }

      if (response.isSuccessful) {
        val body = response.body()
        val creditsRemaining = body?.musicCredits ?: body?.remaining ?: 1
        val credits = UserMusicCredits(
          userId = body?.userId ?: userId,
          tier = body?.tier ?: "free",
          used = body?.used ?: 0,
          limit = body?.limit ?: creditsRemaining,
          remaining = creditsRemaining,
          periodStart = body?.periodStart ?: 0L,
          periodEnd = body?.periodEnd ?: 0L,
          isUnlimitedDev = body?.isUnlimitedDev ?: false,
          subscriptionStatus = body?.subscriptionStatus ?: "unpaid",
          musicCredits = creditsRemaining,
          isOwner = body?.isOwner ?: false,
        )
        AppResult.Success(credits)
      } else {
        val errBody = response.errorBody()?.string()
        AppResult.Error(classifyHttpError(response.code(), errBody))
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      AppResult.Error(AppError.UnknownError(e.message ?: "Failed to retrieve credits", e))
    }
  }

  override suspend fun generateMusic(
    userId: String,
    prompt: String,
    genre: String?,
    mood: String?,
    language: String?,
    title: String?,
    isInstrumental: Boolean,
    onStatusUpdate: (String) -> Unit
  ): AppResult<List<GeneratedSong>> = withContext(Dispatchers.IO) {
    val cleanPrompt = prompt.trim()
    if (cleanPrompt.isBlank()) {
      return@withContext AppResult.Error(AppError.ValidationError("Please describe the song you want to create."))
    }

    // Build enriched natural description including genre, mood, language if provided
    val tagsList = mutableListOf<String>()

    if (!genre.isNullOrBlank()) {
      tagsList.add(genre)
    }
    if (!mood.isNullOrBlank()) {
      tagsList.add(mood)
    }
    if (!language.isNullOrBlank() && language != "English") {
      tagsList.add("in $language")
    }

    val tagsString = if (tagsList.isNotEmpty()) tagsList.joinToString(", ") else null

    val createRequest = MusicCreateBackendRequest(
      prompt = cleanPrompt,
      gptDescriptionPrompt = cleanPrompt,
      title = title?.takeIf { it.isNotBlank() },
      tags = tagsString,
      instrumental = isInstrumental,
      userId = userId,
    )

    onStatusUpdate("Connecting to music engine...")

    val authHeader = getAuthHeader()

    // 1. Send task creation request to backend
    val createResponse = try {
      val res = try {
        apiService.createMusic(authHeader = authHeader, request = createRequest)
      } catch (e: IOException) {
        if (baseUrl == AppConfig.MUSIC_BACKEND_EMULATOR_URL) {
          try {
            createApiService(AppConfig.MUSIC_BACKEND_LOCALHOST_URL).createMusic(authHeader = authHeader, request = createRequest)
          } catch (e2: Exception) {
            return@withContext AppResult.Error(classifyNetworkError(e2, AppConfig.MUSIC_BACKEND_LOCALHOST_URL))
          }
        } else {
          return@withContext AppResult.Error(classifyNetworkError(e, baseUrl))
        }
      }
      res
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      Log.e("KasaMusicService", "Unexpected error creating task: ${e.message}")
      return@withContext AppResult.Error(AppError.UnknownError(e.message ?: "Failed to start music generation.", e))
    }

    val statusCode = createResponse.code()
    if (!createResponse.isSuccessful) {
      val errorBody = createResponse.errorBody()?.string()
      return@withContext AppResult.Error(classifyHttpError(statusCode, errorBody))
    }

    val createBody = createResponse.body()
    val taskId = createBody?.taskId
    if (taskId.isNullOrBlank()) {
      return@withContext AppResult.Error(
        AppError.UnknownError("Music service did not return a valid task ID.")
      )
    }

    Log.i("KasaMusicService", "Music task started successfully: $taskId")

    // 2. Poll the task endpoint every 15 seconds as documented
    val pollIntervalMs = 15000L
    val maxPollAttempts = 20 // 5 minutes maximum
    var pollCount = 0

    while (pollCount < maxPollAttempts) {
      pollCount++
      delay(pollIntervalMs)

      when (pollCount) {
        1 -> onStatusUpdate("Your song is being generated...")
        2, 3 -> onStatusUpdate("Composing your song...")
        4, 5 -> onStatusUpdate("Almost ready...")
        else -> onStatusUpdate("Finishing your track...")
      }

      val pollResponse = try {
        val res = try {
          apiService.getTaskStatus(taskId = taskId, authHeader = authHeader, userId = userId)
        } catch (e: IOException) {
          if (baseUrl == AppConfig.MUSIC_BACKEND_EMULATOR_URL) {
            createApiService(AppConfig.MUSIC_BACKEND_LOCALHOST_URL).getTaskStatus(taskId = taskId, authHeader = authHeader, userId = userId)
          } else {
            throw e
          }
        }
        res
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w("KasaMusicService", "Polling attempt $pollCount failed: ${e.message}")
        continue
      }

      if (!pollResponse.isSuccessful) {
        val pollCode = pollResponse.code()
        val errorBody = pollResponse.errorBody()?.string()
        if (pollCode == 401 || pollCode == 403 || pollCode == 429) {
          return@withContext AppResult.Error(classifyHttpError(pollCode, errorBody))
        }
        continue
      }

      val taskData = pollResponse.body()
      if (taskData == null) {
        continue
      }
      val status = taskData.status?.lowercase() ?: "running"

      if (status == "succeeded") {
        val validClips = taskData.getAllValidClips()

        if (validClips.isNotEmpty()) {
          onStatusUpdate(if (validClips.size > 1) "Songs ready 🎵" else "Song ready 🎵")
          val now = System.currentTimeMillis()
          val songs = validClips.mapIndexed { index, clip ->
            val variationSuffix = if (validClips.size > 1) " (Option ${index + 1})" else ""
            val baseTitle = clip.title?.takeIf { it.isNotBlank() }
              ?: title?.takeIf { it.isNotBlank() }
              ?: "${genre ?: "Afrobeats"} Groove"
            val resolvedTitle = if (validClips.size > 1 && !baseTitle.contains("Option", ignoreCase = true)) {
              "$baseTitle$variationSuffix"
            } else {
              baseTitle
            }

            val clipId = clip.id?.takeIf { it.isNotBlank() } ?: "${taskId}_var${index + 1}"

            GeneratedSong(
              id = clipId,
              userId = userId,
              title = resolvedTitle,
              prompt = cleanPrompt,
              audioUrl = clip.audioUrl!!,
              duration = clip.duration ?: 120.0f,
              imageUrl = clip.imageUrl,
              tags = clip.tags ?: tagsString,
              genre = genre,
              language = language,
              isInstrumental = isInstrumental,
              createdAt = now + index,
            )
          }

          // Save all generated song variations to local database
          songs.forEach { song ->
            musicRepository.saveSong(song)
          }

          return@withContext AppResult.Success(songs)
        } else {
          return@withContext AppResult.Error(
            AppError.AiEngineError("Song completed but no valid audio URL was returned by provider.")
          )
        }
      } else if (status == "failed") {
        val errorMsg = taskData.error ?: "Song generation failed on the music engine. Please try with a different prompt."
        return@withContext AppResult.Error(AppError.AiEngineError(errorMsg))
      }
    }

    return@withContext AppResult.Error(
      AppError.ServiceUnavailable("Song generation is taking longer than expected. Please check back shortly.")
    )
  }

  override suspend fun initializeCheckout(planId: String): AppResult<BillingCheckoutBackendResponse> = withContext(Dispatchers.IO) {
    try {
      val authHeader = getAuthHeader()
      val response = try {
        apiService.initializeCheckout(
          authHeader = authHeader,
          request = BillingCheckoutBackendRequest(planId = planId),
        )
      } catch (e: Exception) {
        if (baseUrl == AppConfig.MUSIC_BACKEND_EMULATOR_URL) {
          try {
            createApiService(AppConfig.MUSIC_BACKEND_LOCALHOST_URL).initializeCheckout(
              authHeader = authHeader,
              request = BillingCheckoutBackendRequest(planId = planId),
            )
          } catch (e2: Exception) {
            return@withContext AppResult.Error(classifyNetworkError(e2, AppConfig.MUSIC_BACKEND_LOCALHOST_URL))
          }
        } else {
          return@withContext AppResult.Error(classifyNetworkError(e, baseUrl))
        }
      }

      if (response.isSuccessful && response.body()?.success == true) {
        AppResult.Success(response.body()!!)
      } else {
        val errBody = response.errorBody()?.string()
        AppResult.Error(classifyHttpError(response.code(), errBody))
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      AppResult.Error(AppError.UnknownError(e.message ?: "Failed to initialize checkout", e))
    }
  }

  override suspend fun verifySession(reference: String): AppResult<BillingVerifySessionBackendResponse> = withContext(Dispatchers.IO) {
    try {
      val authHeader = getAuthHeader()
      val response = try {
        apiService.verifySession(
          authHeader = authHeader,
          request = BillingVerifySessionBackendRequest(reference = reference),
        )
      } catch (e: Exception) {
        if (baseUrl == AppConfig.MUSIC_BACKEND_EMULATOR_URL) {
          try {
            createApiService(AppConfig.MUSIC_BACKEND_LOCALHOST_URL).verifySession(
              authHeader = authHeader,
              request = BillingVerifySessionBackendRequest(reference = reference),
            )
          } catch (e2: Exception) {
            return@withContext AppResult.Error(classifyNetworkError(e2, AppConfig.MUSIC_BACKEND_LOCALHOST_URL))
          }
        } else {
          return@withContext AppResult.Error(classifyNetworkError(e, baseUrl))
        }
      }

      if (response.isSuccessful && response.body()?.success == true) {
        AppResult.Success(response.body()!!)
      } else {
        val errBody = response.errorBody()?.string()
        AppResult.Error(classifyHttpError(response.code(), errBody))
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      AppResult.Error(AppError.UnknownError(e.message ?: "Failed to verify session", e))
    }
  }
}
