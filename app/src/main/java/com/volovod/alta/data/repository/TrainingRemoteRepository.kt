package com.volovod.alta.data.repository

import com.volovod.alta.data.session.SessionStore
import com.volovod.alta.network.ApiClient
import com.volovod.alta.network.dto.TrainingCreateRequestDto
import com.volovod.alta.network.dto.TrainingEntryResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrainingRemoteRepository(
    sessionStore: SessionStore,
) {
    private val api = ApiClient.createTrainingApi(sessionStore)

    suspend fun getTrainingRange(start: Int, end: Int): Result<List<TrainingEntryResponseDto>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getTrainingRange(start = start, end = end))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createTraining(
        dateEpochDay: Int,
        title: String,
        description: String? = null,
        caloriesBurned: Int,
        durationMinutes: Int,
    ): Result<TrainingEntryResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(
                    api.createTraining(
                        TrainingCreateRequestDto(
                            date_epoch_day = dateEpochDay,
                            title = title,
                            description = description,
                            calories_burned = caloriesBurned,
                            duration_minutes = durationMinutes,
                        )
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteTraining(entryId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                api.deleteTraining(entryId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
