package com.example.zhivoy.network.api

import com.example.zhivoy.network.dto.SyncBatchRequestDto
import com.example.zhivoy.network.dto.SyncBatchResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface SyncApi {
    @POST("sync/batch")
    suspend fun syncBatch(@Body request: SyncBatchRequestDto): SyncBatchResponseDto
}
