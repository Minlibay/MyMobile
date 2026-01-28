package com.volovod.alta.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserSettingsRequestDto(
    val calorie_mode: String,
    val step_goal: Int,
    val calorie_goal_override: Int?,
    val target_weight_kg: Double? = null,
    val reminders_enabled: Boolean,
)

