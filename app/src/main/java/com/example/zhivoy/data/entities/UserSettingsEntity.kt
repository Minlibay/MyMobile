package com.example.zhivoy.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_settings",
    indices = [Index(value = ["userId"], unique = true)],
)
data class UserSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val calorieMode: String, // "maintain" | "lose" | "gain"
    val stepGoal: Int,
    val calorieGoalOverride: Int?, // если null -> считаем по профилю (Mifflin)
    val remindersEnabled: Boolean = true,
    val updatedAtEpochMs: Long,
)


