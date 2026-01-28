package com.volovod.alta.data.repository

import com.volovod.alta.data.session.SessionStore
import com.volovod.alta.network.ApiClient
import com.volovod.alta.network.dto.StepEntryResponseDto
import com.volovod.alta.network.dto.StepUpsertRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StepsRepository(
    sessionStore: SessionStore,
) {
    private val api = ApiClient.createStepsApi(sessionStore)

    suspend fun upsertSteps(dateEpochDay: Int, steps: Int): Result<StepEntryResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.upsertSteps(StepUpsertRequestDto(date_epoch_day = dateEpochDay, steps = steps)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getStepsRange(start: Int, end: Int): Result<List<StepEntryResponseDto>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getStepsRange(start = start, end = end))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}


