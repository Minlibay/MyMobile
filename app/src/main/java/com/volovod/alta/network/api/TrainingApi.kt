package com.volovod.alta.network.api

import com.volovod.alta.network.dto.TrainingCreateRequestDto
import com.volovod.alta.network.dto.TrainingEntryResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TrainingApi {
    @GET("training/me")
    suspend fun getTrainingRange(
        @Query("start") start: Int,
        @Query("end") end: Int,
    ): List<TrainingEntryResponseDto>

    @POST("training/me")
    suspend fun createTraining(@Body request: TrainingCreateRequestDto): TrainingEntryResponseDto

    @DELETE("training/me/{entry_id}")
    suspend fun deleteTraining(@Path("entry_id") entryId: Long)
}
