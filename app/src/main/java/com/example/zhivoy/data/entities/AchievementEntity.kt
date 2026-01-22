package com.example.zhivoy.data.entities

import androidx.room.*

@Entity(
    tableName = "achievements",
    indices = [Index(value = ["userId", "code"], unique = true)]
)
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val code: String, // e.g. "steps_10k", "water_7d", etc.
    val createdAtEpochMs: Long
)
