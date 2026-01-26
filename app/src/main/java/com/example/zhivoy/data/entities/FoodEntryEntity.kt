package com.example.zhivoy.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_entries",
    indices = [
        Index(value = ["userId", "dateEpochDay"]),
    ],
)
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val dateEpochDay: Int,
    val title: String,
    val calories: Int,
    val createdAtEpochMs: Long,
)














