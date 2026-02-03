package com.volovod.alta.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_plans")
data class TrainingPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val dateEpochDay: Int,
    val templateId: Long,
    val isDone: Boolean = false,
    val createdAtEpochMs: Long,
)













