package com.volovod.alta.network.api

import com.volovod.alta.network.dto.FoodCreateRequestDto
import com.volovod.alta.network.dto.FoodEntryResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FoodApi {
    @GET("food/me")
    suspend fun getFoodRange(
        @Query("start") start: Int,
        @Query("end") end: Int,
    ): List<FoodEntryResponseDto>

    @POST("food/me")
    suspend fun createFood(@Body request: FoodCreateRequestDto): FoodEntryResponseDto

    @DELETE("food/me/{entry_id}")
    suspend fun deleteFood(@Path("entry_id") entryId: Long)
}
