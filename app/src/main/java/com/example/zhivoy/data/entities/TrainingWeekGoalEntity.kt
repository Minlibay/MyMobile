package com.example.zhivoy.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_week_goals")
data class TrainingWeekGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val weekEpochDay: Int, // e.g. start of week
    val targetTrainingsCount: Int,
    val updatedAtEpochMs: Long,
)




