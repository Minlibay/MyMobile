package com.example.zhivoy.data.dao

import androidx.room.*
import com.example.zhivoy.data.entities.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun observeAll(userId: Long): Flow<List<AchievementEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM achievements WHERE userId = :userId AND code = :code)")
    suspend fun hasAchievement(userId: Long, code: String): Boolean
}












