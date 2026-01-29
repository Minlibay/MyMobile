package com.volovod.alta.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TrainingCreateRequestDto(
    val date_epoch_day: Int,
    val title: String,
    val description: String? = null,
    val calories_burned: Int,
    val duration_minutes: Int,
)

@Serializable
data class TrainingEntryResponseDto(
    val id: Long,
    val date_epoch_day: Int,
    val title: String,
    val description: String? = null,
    val calories_burned: Int,
    val duration_minutes: Int,
    val created_at: String,
)
