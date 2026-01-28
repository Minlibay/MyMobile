package com.example.zhivoy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zhivoy.data.entities.FoodEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query(
        """
        SELECT * FROM food_entries
        WHERE userId = :userId AND dateEpochDay = :epochDay
        ORDER BY createdAtEpochMs DESC
        """,
    )
    fun observeForDay(userId: Long, epochDay: Int): Flow<List<FoodEntryEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(calories), 0) FROM food_entries
        WHERE userId = :userId AND dateEpochDay = :epochDay
        """,
    )
    fun observeTotalCaloriesForDay(userId: Long, epochDay: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: FoodEntryEntity): Long

    @Query("SELECT * FROM food_entries WHERE userId = :userId ORDER BY dateEpochDay DESC")
    fun observeAll(userId: Long): Flow<List<FoodEntryEntity>>

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        SELECT dateEpochDay, SUM(calories) as value FROM food_entries
        WHERE userId = :userId AND dateEpochDay >= :start AND dateEpochDay <= :end
        GROUP BY dateEpochDay
        """,
    )
    suspend fun getDailyCaloriesInRange(userId: Long, start: Int, end: Int): List<com.example.zhivoy.data.model.DayValue>

    @Query(
        """
        SELECT dateEpochDay, SUM(calories) as value FROM food_entries
        WHERE userId = :userId AND dateEpochDay >= :start AND dateEpochDay <= :end
        GROUP BY dateEpochDay
        """,
    )
    fun observeDailyCaloriesInRange(userId: Long, start: Int, end: Int): Flow<List<com.example.zhivoy.data.model.DayValue>>
}


