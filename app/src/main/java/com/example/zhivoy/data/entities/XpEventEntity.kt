package com.example.zhivoy.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "xp_events",
    indices = [Index(value = ["userId", "dateEpochDay"])],
)
data class XpEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val dateEpochDay: Int,
    val type: String, // e.g. "book", "steps", "nosmoke", "calories", "training"
    val points: Int,
    val note: String? = null,
    val createdAtEpochMs: Long,
)














