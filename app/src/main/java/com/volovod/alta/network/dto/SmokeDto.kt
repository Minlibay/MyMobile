package com.volovod.alta.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SmokeStatusRequestDto(
    val started_at: String,
    val is_active: Boolean,
    val pack_price: Double,
    val packs_per_day: Double,
)

@Serializable
data class SmokeStatusResponseDto(
    val started_at: String,
    val is_active: Boolean,
    val pack_price: Double,
    val packs_per_day: Double,
    val updated_at: String,
)
