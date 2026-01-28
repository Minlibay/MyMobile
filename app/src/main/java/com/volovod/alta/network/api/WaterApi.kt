package com.volovod.alta.network.api

import com.volovod.alta.network.dto.WaterCreateRequestDto
import com.volovod.alta.network.dto.WaterEntryResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WaterApi {
    @GET("water/me")
    suspend fun getWaterRange(
        @Query("start") start: Int,
        @Query("end") end: Int,
    ): List<WaterEntryResponseDto>

    @POST("water/me")
    suspend fun createWater(@Body request: WaterCreateRequestDto): WaterEntryResponseDto

    @DELETE("water/me/{entry_id}")
    suspend fun deleteWater(@Path("entry_id") entryId: Long)

    @DELETE("water/me")
    suspend fun clearWaterDay(@Query("date_epoch_day") dateEpochDay: Int)
}
