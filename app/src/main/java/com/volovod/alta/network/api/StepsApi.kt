package com.volovod.alta.network.api

import com.volovod.alta.network.dto.StepEntryResponseDto
import com.volovod.alta.network.dto.StepUpsertRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface StepsApi {
    @GET("steps/me")
    suspend fun getStepsRange(
        @Query("start") start: Int,
        @Query("end") end: Int,
    ): List<StepEntryResponseDto>

    @PUT("steps/me")
    suspend fun upsertSteps(@Body request: StepUpsertRequestDto): StepEntryResponseDto
}


