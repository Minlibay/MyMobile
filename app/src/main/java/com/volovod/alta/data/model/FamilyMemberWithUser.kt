package com.volovod.alta.data.model

data class FamilyMemberWithUser(
    val userId: Long,
    val login: String,
    val joinedAtEpochMs: Long,
    val totalXp: Int = 0
)













