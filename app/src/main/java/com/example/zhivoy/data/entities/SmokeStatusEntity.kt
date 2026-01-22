package com.example.zhivoy.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "smoke_status",
    indices = [Index(value = ["userId"], unique = true)],
)
data class SmokeStatusEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val startedAtEpochMs: Long,
    val isActive: Boolean,
    val packPrice: Double,
    val packsPerDay: Double,
    val updatedAtEpochMs: Long,
)





