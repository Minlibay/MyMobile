package com.example.zhivoy.data.dao

import androidx.room.*
import com.example.zhivoy.data.entities.WaterEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Insert
    suspend fun insert(entry: WaterEntryEntity)

    @Query("SELECT SUM(amountMl) FROM water_entries WHERE userId = :userId AND dateEpochDay = :date")
    fun observeTotalForDay(userId: Long, date: Int): Flow<Int?>

    @Query("SELECT * FROM water_entries WHERE userId = :userId AND dateEpochDay = :date")
    suspend fun getEntriesForDay(userId: Long, date: Int): List<WaterEntryEntity>
    
    @Query("SELECT * FROM water_entries WHERE userId = :userId ORDER BY dateEpochDay DESC")
    fun observeAll(userId: Long): Flow<List<WaterEntryEntity>>
    
    @Query("DELETE FROM water_entries WHERE userId = :userId AND dateEpochDay = :date")
    suspend fun clearDay(userId: Long, date: Int)
}
