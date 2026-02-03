package com.volovod.alta.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class WaterCreateRequestDto(
    val date_epoch_day: Int,
    val amount_ml: Int,
)

@Serializable
data class WaterEntryResponseDto(
    val id: Long,
    val date_epoch_day: Int,
    val amount_ml: Int,
    val created_at: String,
)
