package com.example.zhivoy.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminSettingsRequestDto(
    val openrouter_api_key: String? = null,
    val openrouter_model: String? = null,
)

@Serializable
data class AdminSettingsResponseDto(
    val openrouter_api_key: String? = null,
    val openrouter_model: String? = null,
    val updated_at: String,
)
