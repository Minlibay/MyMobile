package com.volovod.alta.network.api

import com.volovod.alta.network.dto.AdminSettingsResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface AdminSettingsApi {
    @GET("admin/api/settings")
    suspend fun getAdminSettings(): AdminSettingsResponseDto

    @PUT("admin/api/settings")
    suspend fun updateAdminSettings(@Body request: com.volovod.alta.network.dto.AdminSettingsRequestDto): AdminSettingsResponseDto
}
