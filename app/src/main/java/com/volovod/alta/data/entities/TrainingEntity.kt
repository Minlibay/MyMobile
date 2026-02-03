package com.volovod.alta.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trainings")
data class TrainingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val dateEpochDay: Int,
    val title: String,
    val description: String? = null,
    val caloriesBurned: Int = 0,
    val durationMinutes: Int = 0,
    val templateId: Long? = null,
    val createdAtEpochMs: Long,
)















