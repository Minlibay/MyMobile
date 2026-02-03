package com.volovod.alta.network.api

import com.volovod.alta.network.dto.UserSettingsRequestDto
import com.volovod.alta.network.dto.UserSettingsResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserSettingsApi {
    @GET("user_settings/me")
    suspend fun getUserSettings(): UserSettingsResponseDto

    @PUT("user_settings/me")
    suspend fun upsertUserSettings(@Body request: UserSettingsRequestDto): UserSettingsResponseDto
}

