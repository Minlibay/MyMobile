package com.volovod.alta.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val dateEpochDay: Int,
    val weightKg: Double,
    val createdAtEpochMs: Long,
)















