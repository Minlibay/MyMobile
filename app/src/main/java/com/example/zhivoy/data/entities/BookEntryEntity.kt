package com.example.zhivoy.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_entries",
    indices = [Index(value = ["userId", "createdAtEpochMs"])],
)
data class BookEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val author: String? = null,
    val totalPages: Int = 0,
    val pagesRead: Int = 0,
    val createdAtEpochMs: Long,
)
