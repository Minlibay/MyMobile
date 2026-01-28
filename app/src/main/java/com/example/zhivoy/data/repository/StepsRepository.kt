package com.example.zhivoy.data.repository

import com.example.zhivoy.data.session.SessionStore
import com.example.zhivoy.network.ApiClient
import com.example.zhivoy.network.dto.StepEntryResponseDto
import com.example.zhivoy.network.dto.StepUpsertRequestDto
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


