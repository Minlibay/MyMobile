package com.example.zhivoy.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "profiles",
    indices = [Index(value = ["userId"], unique = true)],
)
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val heightCm: Int,
    val weightKg: Double,
    val age: Int,
    val sex: String, // "male" | "female"
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)














