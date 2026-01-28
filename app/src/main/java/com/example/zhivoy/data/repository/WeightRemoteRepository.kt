package com.example.zhivoy.data.repository

import com.example.zhivoy.data.session.SessionStore
import com.example.zhivoy.network.ApiClient
import com.example.zhivoy.network.dto.WeightEntryResponseDto
import com.example.zhivoy.network.dto.WeightUpsertRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeightRemoteRepository(
    sessionStore: SessionStore,
) {
    private val api = ApiClient.createWeightApi(sessionStore)

    suspend fun getWeightRange(start: Int, end: Int): Result<List<WeightEntryResponseDto>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getWeightRange(start = start, end = end))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun upsertWeight(dateEpochDay: Int, weightKg: Double): Result<WeightEntryResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.upsertWeight(WeightUpsertRequestDto(date_epoch_day = dateEpochDay, weight_kg = weightKg)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
