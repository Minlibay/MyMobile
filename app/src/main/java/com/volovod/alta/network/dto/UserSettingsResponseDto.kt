package com.volovod.alta.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserSettingsResponseDto(
    val id: Int,
    val user_id: Int,
    val calorie_mode: String,
    val step_goal: Int,
    val calorie_goal_override: Int?,
    val target_weight_kg: Double? = null,
    val reminders_enabled: Boolean,
    val privacy_policy_accepted_at: String? = null,
    val privacy_policy_accepted_policy_updated_at: String? = null,
    val announcement_read_at: String? = null,
    val announcement_read_announcement_updated_at: String? = null,
    val updated_at: String,
)

