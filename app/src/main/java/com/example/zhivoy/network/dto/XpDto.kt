package com.example.zhivoy.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class XpEventCreateRequestDto(
    val date_epoch_day: Int,
    val type: String,
    val points: Int,
    val note: String? = null,
)

@Serializable
data class XpEventResponseDto(
    val id: Long,
    val date_epoch_day: Int,
    val type: String,
    val points: Int,
    val note: String? = null,
    val created_at: String,
)

@Serializable
data class XpDailyAggregateResponseDto(
    val date_epoch_day: Int,
    val total_points: Int,
)

@Serializable
data class XpTotalResponseDto(
    val total_points: Int,
    val level: Int,
)

@Serializable
data class UserAchievementResponseDto(
    val id: Long,
    val code: String,
    val created_at: String,
)
