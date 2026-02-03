package com.volovod.alta.network.api

import com.volovod.alta.network.dto.WeightEntryResponseDto
import com.volovod.alta.network.dto.WeightUpsertRequestDto
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
