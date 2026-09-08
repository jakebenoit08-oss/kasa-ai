package com.example.core.ai.image

import android.content.Context
import android.util.Base64
import com.example.core.ai.gemini.GeminiApiService
import com.example.core.ai.gemini.GeminiContent
import com.example.core.ai.gemini.GeminiGenerateContentRequest
import com.example.core.ai.gemini.GeminiGenerationConfig
import com.example.core.ai.gemini.GeminiImageConfig
import com.example.core.ai.gemini.GeminiPart
import com.example.core.config.AppConfig
import com.example.core.error.AppError
import com.example.core.result.AppResult
import com.example.data.model.GeneratedImage
import com.example.data.repository.GeneratedImageRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

interface ImageGenerationService {
  suspend fun generateImage(
    userId: String,
    prompt: String,
    aspectRatio: String = "1:1",
  ): AppResult<GeneratedImage>
}

class GeminiImageGenerationService(
  private val context: Context,
  private val imageRepository: GeneratedImageRepository,
) : ImageGenerationService {

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

  private val apiService: GeminiApiService by lazy {
    Retrofit.Builder()
      .baseUrl(AppConfig.GEMINI_API_BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(GeminiApiService::class.java)
  }

  private val imagesDirectory: File by lazy {
    val dir = File(context.filesDir, "kasa_images")
    if (!dir.exists()) {
      dir.mkdirs()
    }
    dir
  }

  override suspend fun generateImage(
    userId: String,
    prompt: String,
    aspectRatio: String,
  ): AppResult<GeneratedImage> = withContext(Dispatchers.IO) {
    val cleanPrompt = prompt.trim()
    if (cleanPrompt.isBlank()) {
      return@withContext AppResult.Error(
        AppError.ValidationError("Please enter a description for the image you want to create.")
      )
    }

    val apiKey = AppConfig.getGeminiApiKey().trim()
    if (apiKey.isBlank()) {
      return@withContext AppResult.Error(
        AppError.ConfigurationError("Gemini API key is not configured. Please add your API key in Google AI Studio Secrets.")
      )
    }

    val imageId = UUID.randomUUID().toString()
    val imageFile = File(imagesDirectory, "img_$imageId.jpg")

    var primaryHttpException: HttpException? = null

    // Primary Generation Attempt: gemini-2.5-flash-image (Nano Banana)
    try {
      val geminiRequest = GeminiGenerateContentRequest(
        contents = listOf(
          GeminiContent(
            role = "user",
            parts = listOf(GeminiPart(text = cleanPrompt))
          )
        ),
        generationConfig = GeminiGenerationConfig(
          responseModalities = listOf("TEXT", "IMAGE"),
          imageConfig = GeminiImageConfig(
            aspectRatio = aspectRatio,
            imageSize = "1K",
          ),
          temperature = 0.7f,
        )
      )

      val response = apiService.generateContent(
        model = AppConfig.GEMINI_IMAGE_DEFAULT_MODEL,
        apiKey = apiKey,
        request = geminiRequest,
      )

      var base64Data: String? = null
      var mimeType = "image/jpeg"
      var refinedPrompt: String? = null

      val candidate = response.candidates?.firstOrNull()
      if (candidate != null) {
        for (part in candidate.content?.parts.orEmpty()) {
          if (!part.text.isNullOrBlank()) {
            refinedPrompt = part.text
          }
          val inline = part.inlineData
          if (inline != null && !inline.data.isNullOrBlank()) {
            base64Data = inline.data
            mimeType = inline.mimeType ?: "image/jpeg"
            break
          }
        }
      }

      if (!base64Data.isNullOrBlank()) {
        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
        FileOutputStream(imageFile).use { fos ->
          fos.write(imageBytes)
          fos.flush()
        }

        val generatedImage = GeneratedImage(
          id = imageId,
          userId = userId,
          prompt = cleanPrompt,
          refinedPrompt = refinedPrompt,
          imagePath = imageFile.absolutePath,
          mimeType = mimeType,
          aspectRatio = aspectRatio,
          createdAt = System.currentTimeMillis(),
          width = 1024,
          height = 1024,
        )

        imageRepository.saveImage(generatedImage)
        return@withContext AppResult.Success(generatedImage)
      } else {
        return@withContext AppResult.Error(
          AppError.AiEngineError("The image generation service did not return an image for this prompt. Please try a different description.")
        )
      }
    } catch (e: HttpException) {
      val statusCode = e.code()
      val parsedMsg = parseApiErrorMessage(e)
      // COST PROTECTION: Quota (429), validation (400), authentication (401/403), or not found (404)
      // MUST NOT trigger paid fallback or repeated billing requests.
      if (statusCode in 400..499) {
        val errorMsg = parsedMsg ?: when (statusCode) {
          429 -> "Image generation reached its quota or rate limit. Google AI Studio requires an active billing plan or image quota to generate images."
          403, 401 -> "Image generation is unavailable for this API key. Please check your AI Studio Secrets key."
          400 -> "The image description could not be processed. If your prompt includes sensitive terms, please rephrase your request."
          else -> "Image generation request error (HTTP $statusCode). Please try again shortly."
        }
        return@withContext AppResult.Error(AppError.AiEngineError(errorMsg, e))
      }
      primaryHttpException = e
    } catch (e: java.io.IOException) {
      return@withContext AppResult.Error(
        AppError.NetworkError("Couldn't connect to the image service. Please check your internet connection.", e)
      )
    } catch (e: Exception) {
      return@withContext AppResult.Error(
        AppError.AiEngineError(e.localizedMessage ?: "Image generation encountered an unexpected error. Please try again.", e)
      )
    }

    // Recoverable Server Error Fallback: ONLY triggered for 5xx transient outages on Google's backend
    if (primaryHttpException != null && primaryHttpException.code() in 500..599) {
      try {
        val fallbackRequest = GeminiGenerateContentRequest(
          contents = listOf(
            GeminiContent(
              role = "user",
              parts = listOf(GeminiPart(text = cleanPrompt))
            )
          ),
          generationConfig = GeminiGenerationConfig(
            responseModalities = listOf("TEXT", "IMAGE"),
            imageConfig = GeminiImageConfig(
              aspectRatio = aspectRatio,
              imageSize = "1K",
            ),
            temperature = 0.7f,
          )
        )

        val response = apiService.generateContent(
          model = "gemini-3.1-flash-image-preview",
          apiKey = apiKey,
          request = fallbackRequest,
        )

        var base64Data: String? = null
        var mimeType = "image/jpeg"
        var refinedPrompt: String? = null

        val candidate = response.candidates?.firstOrNull()
        if (candidate != null) {
          for (part in candidate.content?.parts.orEmpty()) {
            if (!part.text.isNullOrBlank()) {
              refinedPrompt = part.text
            }
            val inline = part.inlineData
            if (inline != null && !inline.data.isNullOrBlank()) {
              base64Data = inline.data
              mimeType = inline.mimeType ?: "image/jpeg"
              break
            }
          }
        }

        if (!base64Data.isNullOrBlank()) {
          val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
          FileOutputStream(imageFile).use { fos ->
            fos.write(imageBytes)
            fos.flush()
          }

          val generatedImage = GeneratedImage(
            id = imageId,
            userId = userId,
            prompt = cleanPrompt,
            refinedPrompt = refinedPrompt,
            imagePath = imageFile.absolutePath,
            mimeType = mimeType,
            aspectRatio = aspectRatio,
            createdAt = System.currentTimeMillis(),
            width = 1024,
            height = 1024,
          )

          imageRepository.saveImage(generatedImage)
          return@withContext AppResult.Success(generatedImage)
        }
      } catch (e: Exception) {
        // Fallback also failed; report primary server error
      }
    }

    val serverCode = primaryHttpException?.code() ?: 500
    return@withContext AppResult.Error(
      AppError.AiEngineError("Image generation service is temporarily unavailable (HTTP $serverCode). Please try again shortly.", primaryHttpException)
    )
  }

  private fun parseApiErrorMessage(e: HttpException): String? {
    return try {
      val raw = e.response()?.errorBody()?.string() ?: return null
      val jsonObj = org.json.JSONObject(raw)
      val errorObj = jsonObj.optJSONObject("error") ?: return null
      val msg = errorObj.optString("message")

      val details = errorObj.optJSONArray("details")
      var isQuotaZero = false
      if (details != null) {
        for (i in 0 until details.length()) {
          val detail = details.optJSONObject(i)
          val metadata = detail?.optJSONObject("metadata")
          if (metadata?.optString("quota_limit") == "0") {
            isQuotaZero = true
            break
          }
        }
      }

      if (isQuotaZero || msg.contains("limit: 0", ignoreCase = true) || (e.code() == 429 && msg.contains("quota", ignoreCase = true))) {
        "Gemini image generation quota exceeded (limit: 0 on free tier). Google AI Studio requires an API key with active billing or assigned image quota to generate images."
      } else if (msg.isNotBlank()) {
        msg
      } else null
    } catch (_: Throwable) {
      null
    }
  }
}
