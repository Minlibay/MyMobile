package com.example.zhivoy.data.repository

import com.example.zhivoy.data.session.SessionStore
import com.example.zhivoy.network.ApiClient
import com.example.zhivoy.network.dto.FoodCreateRequestDto
import com.example.zhivoy.network.dto.FoodEntryResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FoodRemoteRepository(
    sessionStore: SessionStore,
) {
    private val api = ApiClient.createFoodApi(sessionStore)

    suspend fun getFoodRange(start: Int, end: Int): Result<List<FoodEntryResponseDto>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getFoodRange(start = start, end = end))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createFood(dateEpochDay: Int, title: String, calories: Int): Result<FoodEntryResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(
                    api.createFood(
                        FoodCreateRequestDto(
                            date_epoch_day = dateEpochDay,
                            title = title,
                            calories = calories,
                        )
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteFood(entryId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                api.deleteFood(entryId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
