package com.volovod.alta.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PrivacyPolicyResponseDto(
    val text: String,
    val updated_at: String,
)
