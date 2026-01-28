package com.example.zhivoy.network.api

import com.example.zhivoy.network.dto.WeightEntryResponseDto
import com.example.zhivoy.network.dto.WeightUpsertRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface WeightApi {
    @GET("weight/me")
    suspend fun getWeightRange(
        @Query("start") start: Int,
        @Query("end") end: Int,
    ): List<WeightEntryResponseDto>

    @PUT("weight/me")
    suspend fun upsertWeight(@Body request: WeightUpsertRequestDto): WeightEntryResponseDto
}
