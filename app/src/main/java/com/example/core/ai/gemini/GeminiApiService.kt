package com.example.core.ai.gemini

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface GeminiApiService {

  @POST("v1beta/models/{model}:generateContent")
  suspend fun generateContent(
    @Path("model") model: String,
    @Query("key") apiKey: String,
    @Body request: GeminiGenerateContentRequest,
  ): GeminiGenerateContentResponse

  @POST("v1beta/models/{model}:streamGenerateContent")
  @Streaming
  suspend fun generateContentStream(
    @Path("model") model: String,
    @Query("key") apiKey: String,
    @Query("alt") alt: String = "sse",
    @Body request: GeminiGenerateContentRequest,
  ): ResponseBody

  @POST("v1beta/models/{model}:predict")
  suspend fun predictImagen(
    @Path("model") model: String,
    @Query("key") apiKey: String,
    @Body request: ImagenPredictRequest,
  ): ImagenPredictResponse
}
