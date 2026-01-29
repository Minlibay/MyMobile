package com.volovod.alta.network.api

import com.volovod.alta.network.dto.XpEventCreateRequestDto
import com.volovod.alta.network.dto.XpEventResponseDto
import com.volovod.alta.network.dto.XpDailyAggregateResponseDto
import com.volovod.alta.network.dto.XpTotalResponseDto
import com.volovod.alta.network.dto.UserAchievementResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface XpApi {
    @POST("xp/me")
    suspend fun createXpEvent(@Body request: XpEventCreateRequestDto): XpEventResponseDto

    @GET("xp/me/daily")
    suspend fun getXpDaily(
        @Query("start") start: Int,
        @Query("end") end: Int,
    ): List<XpDailyAggregateResponseDto>

    @GET("xp/me/total")
    suspend fun getXpTotal(): XpTotalResponseDto

    @GET("achievements/me")
    suspend fun getAchievements(): List<UserAchievementResponseDto>
}
