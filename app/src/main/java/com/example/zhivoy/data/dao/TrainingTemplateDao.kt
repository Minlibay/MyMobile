package com.example.zhivoy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zhivoy.data.entities.TrainingTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingTemplateDao {
    @Query("SELECT * FROM training_templates WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
    fun observeAll(userId: Long): Flow<List<TrainingTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(template: TrainingTemplateEntity): Long

    @Query("DELETE FROM training_templates WHERE id = :id")
    suspend fun deleteById(id: Long)
}












