package com.example.zhivoy.data.repository

import com.example.zhivoy.data.session.SessionStore
import com.example.zhivoy.network.ApiClient
import com.example.zhivoy.network.dto.XpEventCreateRequestDto
import com.example.zhivoy.network.dto.XpDailyAggregateResponseDto
import com.example.zhivoy.network.dto.XpTotalResponseDto
import com.example.zhivoy.network.dto.UserAchievementResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class XpRemoteRepository(
    sessionStore: SessionStore,
) {
    private val api = ApiClient.createXpApi(sessionStore)

    suspend fun createXpEvent(
        dateEpochDay: Int,
        type: String,
        points: Int,
        note: String? = null,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                api.createXpEvent(
                    XpEventCreateRequestDto(
                        date_epoch_day = dateEpochDay,
                        type = type,
                        points = points,
                        note = note,
                    )
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getXpDaily(start: Int, end: Int): Result<List<XpDailyAggregateResponseDto>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getXpDaily(start = start, end = end))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getXpTotal(): Result<XpTotalResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getXpTotal())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getAchievements(): Result<List<UserAchievementResponseDto>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getAchievements())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
