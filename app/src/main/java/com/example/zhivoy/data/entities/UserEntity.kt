package com.example.zhivoy.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["login"], unique = true),
        Index(value = ["email"], unique = true),
    ],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val login: String,
    val email: String,
    val passwordHash: String,
    val createdAtEpochMs: Long,
)














