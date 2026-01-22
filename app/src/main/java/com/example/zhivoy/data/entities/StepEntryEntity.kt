package com.example.zhivoy.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "step_entries",
    indices = [Index(value = ["userId", "dateEpochDay"], unique = true)],
)
data class StepEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val dateEpochDay: Int,
    val steps: Int,
    val updatedAtEpochMs: Long,
)





