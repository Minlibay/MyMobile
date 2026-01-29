package com.volovod.alta.data.repository

import com.volovod.alta.data.session.SessionStore
import com.volovod.alta.network.ApiClient
import com.volovod.alta.network.dto.WaterCreateRequestDto
import com.volovod.alta.network.dto.WaterEntryResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WaterRepository(
    sessionStore: SessionStore,
) {
    private val api = ApiClient.createWaterApi(sessionStore)

    suspend fun getWaterRange(start: Int, end: Int): Result<List<WaterEntryResponseDto>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getWaterRange(start = start, end = end))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createWater(dateEpochDay: Int, amountMl: Int): Result<WaterEntryResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.createWater(WaterCreateRequestDto(date_epoch_day = dateEpochDay, amount_ml = amountMl)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteWater(entryId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                api.deleteWater(entryId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun clearWaterDay(dateEpochDay: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                api.clearWaterDay(dateEpochDay)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
