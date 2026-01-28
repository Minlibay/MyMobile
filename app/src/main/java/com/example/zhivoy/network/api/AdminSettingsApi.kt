package com.example.zhivoy.network.api

import com.example.zhivoy.network.dto.AdminSettingsResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface AdminSettingsApi {
    @GET("admin/api/settings")
    suspend fun getAdminSettings(): AdminSettingsResponseDto

    @PUT("admin/api/settings")
    suspend fun updateAdminSettings(@Body request: com.example.zhivoy.network.dto.AdminSettingsRequestDto): AdminSettingsResponseDto
}
