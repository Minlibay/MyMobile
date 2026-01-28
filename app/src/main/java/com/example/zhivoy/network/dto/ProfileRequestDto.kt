package com.example.zhivoy.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileRequestDto(
    val height_cm: Int,
    val weight_kg: Double,
    val age: Int,
    val sex: String,
)

