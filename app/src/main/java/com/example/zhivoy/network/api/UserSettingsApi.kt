package com.example.zhivoy.network.api

import com.example.zhivoy.network.dto.UserSettingsRequestDto
import com.example.zhivoy.network.dto.UserSettingsResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserSettingsApi {
    @GET("user_settings/me")
    suspend fun getUserSettings(): UserSettingsResponseDto

    @PUT("user_settings/me")
    suspend fun upsertUserSettings(@Body request: UserSettingsRequestDto): UserSettingsResponseDto
}

