package com.example.zhivoy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zhivoy.data.entities.StepEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepsDao {
    @Query("SELECT * FROM step_entries WHERE userId = :userId AND dateEpochDay = :epochDay LIMIT 1")
    fun observeForDay(userId: Long, epochDay: Int): Flow<StepEntryEntity?>

    @Query("SELECT COALESCE(SUM(steps), 0) FROM step_entries WHERE userId = :userId AND dateEpochDay = :epochDay")
    fun observeTotalForDay(userId: Long, epochDay: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: StepEntryEntity): Long

    @Query("SELECT dateEpochDay, steps as value FROM step_entries WHERE userId = :userId AND dateEpochDay >= :start AND dateEpochDay <= :end")
    suspend fun getInRange(userId: Long, start: Int, end: Int): List<com.example.zhivoy.data.model.DayValue>

    @Query("SELECT dateEpochDay, steps as value FROM step_entries WHERE userId = :userId AND dateEpochDay >= :start AND dateEpochDay <= :end")
    fun observeInRange(userId: Long, start: Int, end: Int): Flow<List<com.example.zhivoy.data.model.DayValue>>
}


