package com.example.zhivoy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zhivoy.data.entities.BookEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query(
        """
        SELECT * FROM book_entries
        WHERE userId = :userId
        ORDER BY createdAtEpochMs DESC
        """,
    )
    fun observeAll(userId: Long): Flow<List<BookEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: BookEntryEntity): Long

    @Query("UPDATE book_entries SET pagesRead = :pagesRead WHERE id = :id")
    suspend fun updateProgress(id: Long, pagesRead: Int)

    @Query("DELETE FROM book_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}


