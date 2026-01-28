package com.example.zhivoy.network.api

import com.example.zhivoy.network.dto.ProfileRequestDto
import com.example.zhivoy.network.dto.ProfileResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface ProfileApi {
    @GET("profile/me")
    suspend fun getProfile(): ProfileResponseDto

    @PUT("profile/me")
    suspend fun upsertProfile(@Body request: ProfileRequestDto): ProfileResponseDto
}

