package com.example.zhivoy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zhivoy.data.entities.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

@Suppress("unused")
@Dao
interface WeightDao {
    @Query(
        """
        SELECT * FROM weight_entries
        WHERE userId = :userId
        ORDER BY dateEpochDay DESC
        LIMIT 1
        """,
    )
    fun observeLatest(userId: Long): Flow<WeightEntryEntity?>

    @Query(
        """
        SELECT * FROM weight_entries
        WHERE userId = :userId
        ORDER BY dateEpochDay DESC
        LIMIT :limit
        """,
    )
    fun observeLast(userId: Long, limit: Int): Flow<List<WeightEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeightEntryEntity)

    @Query("SELECT * FROM weight_entries WHERE userId = :userId AND dateEpochDay >= :start AND dateEpochDay <= :end ORDER BY dateEpochDay DESC")
    fun observeInRange(userId: Long, start: Int, end: Int): Flow<List<WeightEntryEntity>>
}


