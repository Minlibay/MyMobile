package com.example.zhivoy.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class FamilyCreateRequestDto(
    val name: String,
)

@Serializable
data class FamilyInviteRequestDto(
    val login: String,
)

@Serializable
data class FamilyJoinRequestDto(
    val family_name: String,
)

@Serializable
data class FamilyMemberResponseDto(
    val user_id: Int,
    val login: String,
    val joined_at: String,
)

@Serializable
data class FamilyResponseDto(
    val id: Int,
    val name: String,
    val admin_user_id: Int,
    val created_at: String,
    val members: List<FamilyMemberResponseDto>,
)


