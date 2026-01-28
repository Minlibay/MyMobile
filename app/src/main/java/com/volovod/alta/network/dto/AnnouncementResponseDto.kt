package com.volovod.alta.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementResponseDto(
    val text: String,
    val updated_at: String,
    val button_enabled: Boolean = false,
    val button_text: String? = null,
    val button_url: String? = null,
)
