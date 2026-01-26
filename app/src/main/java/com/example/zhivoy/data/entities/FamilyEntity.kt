package com.example.zhivoy.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "families")
data class FamilyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val adminUserId: Long,
    val createdAtEpochMs: Long,
)

@Entity(
    tableName = "family_members",
    primaryKeys = ["familyId", "userId"]
)
data class FamilyMemberEntity(
    val familyId: Long,
    val userId: Long,
    val joinedAtEpochMs: Long,
)













