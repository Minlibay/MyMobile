package com.volovod.alta.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookCreateRequestDto(
    val title: String,
    val author: String? = null,
    val total_pages: Int,
)

@Serializable
data class BookProgressRequestDto(
    val pages_read: Int,
)

@Serializable
data class BookEntryResponseDto(
    val id: Long,
    val title: String,
    val author: String? = null,
    val total_pages: Int,
    val pages_read: Int,
    val created_at: String,
)
