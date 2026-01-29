package com.volovod.alta.network.api

import com.volovod.alta.network.dto.SmokeStatusRequestDto
import com.volovod.alta.network.dto.SmokeStatusResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface SmokeApi {
    @GET("smoke/me")
    suspend fun getSmokeStatus(): SmokeStatusResponseDto

    @PUT("smoke/me")
    suspend fun upsertSmokeStatus(@Body request: SmokeStatusRequestDto): SmokeStatusResponseDto
}
