package com.example.zhivoy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zhivoy.data.entities.TrainingPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingPlanDao {
    @Query("SELECT * FROM training_plans WHERE userId = :userId AND dateEpochDay = :epochDay")
    fun observeForDay(userId: Long, epochDay: Int): Flow<List<TrainingPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(plan: TrainingPlanEntity): Long

    @Query("UPDATE training_plans SET isDone = :isDone WHERE id = :id")
    suspend fun setDone(id: Long, isDone: Boolean)

    @Query("DELETE FROM training_plans WHERE id = :id")
    suspend fun deleteById(id: Long)
}




