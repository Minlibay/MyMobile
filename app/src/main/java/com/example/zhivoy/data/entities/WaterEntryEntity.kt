package com.example.zhivoy.data.entities

import androidx.room.*

@Entity(tableName = "water_entries")
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val dateEpochDay: Int,
    val amountMl: Int,
    val createdAtEpochMs: Long
)
