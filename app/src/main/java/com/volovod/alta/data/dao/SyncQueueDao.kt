package com.volovod.alta.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.volovod.alta.data.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE userId = :userId ORDER BY createdAtEpochMs ASC")
    fun observeForUser(userId: Long): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE userId = :userId AND nextAttemptAtEpochMs <= :nowEpochMs ORDER BY nextAttemptAtEpochMs ASC")
    suspend fun getPendingForUser(userId: Long, nowEpochMs: Long): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: SyncQueueEntity): Long

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE sync_queue SET attempts = :attempts, nextAttemptAtEpochMs = :nextAttemptAtEpochMs WHERE id = :id")
    suspend fun updateAttempt(id: Long, attempts: Int, nextAttemptAtEpochMs: Long?)

    @Query("DELETE FROM sync_queue WHERE userId = :userId")
    suspend fun clearForUser(userId: Long)
}
