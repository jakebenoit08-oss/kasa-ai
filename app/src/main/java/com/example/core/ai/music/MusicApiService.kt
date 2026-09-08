package com.example.core.ai.music

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MusicApiService {

  @GET("api/music/credits")
  suspend fun getCredits(
    @Query("userId") userId: String
  ): Response<MusicCreditsBackendResponse>

  @POST("api/music/create")
  suspend fun createMusic(
    @Body request: MusicCreateBackendRequest
  ): Response<MusicCreateBackendResponse>

  @GET("api/music/task/{taskId}")
  suspend fun getTaskStatus(
    @Path("taskId") taskId: String,
    @Query("userId") userId: String
  ): Response<MusicTaskBackendResponse>
}
