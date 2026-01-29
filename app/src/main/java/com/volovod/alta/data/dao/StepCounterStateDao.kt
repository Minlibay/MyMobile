package com.volovod.alta.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.volovod.alta.data.entities.StepCounterStateEntity

@Dao
interface StepCounterStateDao {
    @Query("SELECT * FROM step_counter_state WHERE userId = :userId LIMIT 1")
    suspend fun get(userId: Long): StepCounterStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StepCounterStateEntity): Long
}














