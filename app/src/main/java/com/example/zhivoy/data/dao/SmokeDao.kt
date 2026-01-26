package com.example.zhivoy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zhivoy.data.entities.SmokeStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmokeDao {
    @Query("SELECT * FROM smoke_status WHERE userId = :userId LIMIT 1")
    fun observe(userId: Long): Flow<SmokeStatusEntity?>

    @Query("SELECT * FROM smoke_status WHERE userId = :userId LIMIT 1")
    suspend fun get(userId: Long): SmokeStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SmokeStatusEntity): Long
}














