package com.volovod.alta.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementReadResponseDto(
    val read_at: String,
    val announcement_updated_at: String,
)
