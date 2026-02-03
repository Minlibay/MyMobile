package com.volovod.alta.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SyncBatchItemDto(
    val entity_type: String,
    val action: String,
    val payload: JsonElement,
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
