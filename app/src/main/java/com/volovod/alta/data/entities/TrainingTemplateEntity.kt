package com.volovod.alta.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_templates")
data class TrainingTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val category: String, // e.g. "Силовая", "Кардио", "Йога"
    val tagsCsv: String, // tags like "руки,грудь"
    val defaultDurationMinutes: Int = 0,
    val defaultCaloriesBurned: Int = 0,
    val createdAtEpochMs: Long,
)













