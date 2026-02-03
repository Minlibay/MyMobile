package com.volovod.alta.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminSettingsRequestDto(
    val gigachat_client_id: String? = null,
    val gigachat_auth_key: String? = null,
    val gigachat_scope: String? = null,
    val openrouter_api_key: String? = null,
    val openrouter_model: String? = null,
)

@Serializable
data class AdminSettingsResponseDto(
    val gigachat_client_id: String? = null,
    val gigachat_auth_key: String? = null,
    val gigachat_scope: String? = null,
    val openrouter_api_key: String? = null,
    val openrouter_model: String? = null,
    val updated_at: String,
)
