package com.example.zhivoy.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class FoodCreateRequestDto(
    val date_epoch_day: Int,
    val title: String,
    val calories: Int,
)

@Serializable
data class FoodEntryResponseDto(
    val id: Long,
    val date_epoch_day: Int,
    val title: String,
    val calories: Int,
    val created_at: String,
)
