package com.example.zhivoy.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponseDto(
    val id: Int,
    val user_id: Int,
    val height_cm: Int,
    val weight_kg: Double,
    val age: Int,
    val sex: String,
    val created_at: String,
    val updated_at: String,
)

