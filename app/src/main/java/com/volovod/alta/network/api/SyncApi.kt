package com.volovod.alta.network.api

import com.volovod.alta.network.dto.SyncBatchRequestDto
import com.volovod.alta.network.dto.SyncBatchResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface SyncApi {
    @POST("sync/batch")
    suspend fun syncBatch(@Body request: SyncBatchRequestDto): SyncBatchResponseDto
}
