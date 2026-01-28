package com.example.zhivoy.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val entityType: String,
    val action: String,
    val payload: String, // JSON string
    val createdAtEpochMs: Long,
    var attempts: Int = 0,
    var nextAttemptAtEpochMs: Long? = null,
)
