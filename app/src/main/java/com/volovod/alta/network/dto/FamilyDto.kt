package com.volovod.alta.network.dto

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

@Serializable
data class FamilyInviteResponseDto(
    val id: Int,
    val family_id: Int,
    val family_name: String,
    val invited_by_user_id: Int,
    val invited_by_login: String,
    val created_at: String,
)


