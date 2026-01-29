package com.volovod.alta.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PrivacyPolicyAcceptResponseDto(
    val accepted_at: String,
    val policy_updated_at: String,
)
