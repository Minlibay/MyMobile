package com.volovod.alta.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class StepUpsertRequestDto(
    val date_epoch_day: Int,
    val steps: Int,
)

@Serializable
data class StepEntryResponseDto(
    val date_epoch_day: Int,
    val steps: Int,
    val updated_at: String,
)


