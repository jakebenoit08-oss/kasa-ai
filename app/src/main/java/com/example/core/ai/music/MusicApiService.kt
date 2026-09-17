package com.example.core.ai.music

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MusicApiService {

  @GET("api/music/credits")
  suspend fun getCredits(
    @Header("Authorization") authHeader: String,
    @Query("userId") userId: String? = null,
  ): Response<MusicCreditsBackendResponse>

  @POST("api/music/create")
  suspend fun createMusic(
    @Header("Authorization") authHeader: String,
    @Body request: MusicCreateBackendRequest,
  ): Response<MusicCreateBackendResponse>

  @GET("api/music/task/{taskId}")
  suspend fun getTaskStatus(
    @Path("taskId") taskId: String,
    @Header("Authorization") authHeader: String,
    @Query("userId") userId: String? = null,
  ): Response<MusicTaskBackendResponse>

  @POST("api/billing/initialize-checkout")
  suspend fun initializeCheckout(
    @Header("Authorization") authHeader: String,
    @Body request: BillingCheckoutBackendRequest,
  ): Response<BillingCheckoutBackendResponse>

  @POST("api/billing/verify-session")
  suspend fun verifySession(
    @Header("Authorization") authHeader: String,
    @Body request: BillingVerifySessionBackendRequest,
  ): Response<BillingVerifySessionBackendResponse>
}
