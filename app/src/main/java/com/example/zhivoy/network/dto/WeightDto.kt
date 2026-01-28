package com.example.zhivoy.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class WeightUpsertRequestDto(
    val date_epoch_day: Int,
    val weight_kg: Double,
)

@Serializable
data class WeightEntryResponseDto(
    val date_epoch_day: Int,
    val weight_kg: Double,
    val updated_at: String,
)
