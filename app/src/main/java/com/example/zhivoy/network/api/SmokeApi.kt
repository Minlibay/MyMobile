package com.example.zhivoy.network.api

import com.example.zhivoy.network.dto.SmokeStatusRequestDto
import com.example.zhivoy.network.dto.SmokeStatusResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface SmokeApi {
    @GET("smoke/me")
    suspend fun getSmokeStatus(): SmokeStatusResponseDto

    @PUT("smoke/me")
    suspend fun upsertSmokeStatus(@Body request: SmokeStatusRequestDto): SmokeStatusResponseDto
}
