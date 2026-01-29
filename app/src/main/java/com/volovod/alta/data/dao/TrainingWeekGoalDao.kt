package com.volovod.alta.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.volovod.alta.data.entities.TrainingWeekGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingWeekGoalDao {
    @Query("SELECT * FROM training_week_goals WHERE userId = :userId AND weekEpochDay = :weekStartEpochDay LIMIT 1")
    fun observeForWeek(userId: Long, weekStartEpochDay: Int): Flow<TrainingWeekGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: TrainingWeekGoalEntity): Long
}
