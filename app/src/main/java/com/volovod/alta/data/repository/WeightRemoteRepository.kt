package com.volovod.alta.data.repository

import com.volovod.alta.data.session.SessionStore
import com.volovod.alta.network.ApiClient
import com.volovod.alta.network.dto.WeightEntryResponseDto
import com.volovod.alta.network.dto.WeightUpsertRequestDto
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
