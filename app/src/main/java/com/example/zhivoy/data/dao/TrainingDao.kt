package com.example.zhivoy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zhivoy.data.entities.TrainingEntity
import kotlinx.coroutines.flow.Flow

@Suppress("unused")
@Dao
interface TrainingDao {
    @Query(
        """
        SELECT * FROM trainings
        WHERE userId = :userId AND dateEpochDay >= :fromEpochDay
        ORDER BY dateEpochDay ASC, createdAtEpochMs DESC
        """,
    )
    fun observeFrom(userId: Long, fromEpochDay: Int): Flow<List<TrainingEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(training: TrainingEntity): Long

    @Query("DELETE FROM trainings WHERE id = :id")
    suspend fun deleteById(id: Long)
}


