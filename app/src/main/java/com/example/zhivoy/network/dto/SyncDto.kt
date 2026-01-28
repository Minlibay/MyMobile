package com.example.zhivoy.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyncBatchItemDto(
    val entity_type: String,
    val action: String,
    val payload: Map<String, Any>,
)

@Serializable
data class SyncBatchRequestDto(
    val items: List<SyncBatchItemDto>,
)

@Serializable
data class SyncBatchResponseDto(
    val processed: Int,
    val failed: Int,
    val errors: List<String> = emptyList(),
)
