package com.example.zhivoy.data.repository

import com.example.zhivoy.data.session.SessionStore
import com.example.zhivoy.network.ApiClient
import com.example.zhivoy.network.dto.SmokeStatusRequestDto
import com.example.zhivoy.network.dto.SmokeStatusResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmokeRemoteRepository(
    sessionStore: SessionStore,
) {
    private val api = ApiClient.createSmokeApi(sessionStore)

    suspend fun getSmokeStatus(): Result<SmokeStatusResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getSmokeStatus())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun upsertSmokeStatus(
        startedAtIso: String,
        isActive: Boolean,
        packPrice: Double,
        packsPerDay: Double,
    ): Result<SmokeStatusResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(
                    api.upsertSmokeStatus(
                        SmokeStatusRequestDto(
                            started_at = startedAtIso,
                            is_active = isActive,
                            pack_price = packPrice,
                            packs_per_day = packsPerDay,
                        )
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
