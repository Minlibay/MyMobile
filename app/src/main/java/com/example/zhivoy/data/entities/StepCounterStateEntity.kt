package com.example.zhivoy.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "step_counter_state",
    indices = [Index(value = ["userId"], unique = true)],
)
data class StepCounterStateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val epochDay: Int,
    val baselineSensorTotal: Long,
    val lastSensorTotal: Long,
    val updatedAtEpochMs: Long,
)














