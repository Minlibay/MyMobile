package com.example.zhivoy.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserSettingsResponseDto(
    val id: Int,
    val user_id: Int,
    val calorie_mode: String,
    val step_goal: Int,
    val calorie_goal_override: Int?,
    val reminders_enabled: Boolean,
    val updated_at: String,
)

