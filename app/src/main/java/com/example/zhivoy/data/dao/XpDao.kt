package com.example.zhivoy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zhivoy.data.entities.XpEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface XpDao {
    @Query("SELECT COALESCE(SUM(points), 0) FROM xp_events WHERE userId = :userId")
    fun observeTotal(userId: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(points), 0) FROM xp_events WHERE userId = :userId AND dateEpochDay = :epochDay")
    fun observeTotalForDay(userId: Long, epochDay: Int): Flow<Int>

    @Query(
        """
        SELECT * FROM xp_events
        WHERE userId = :userId
        ORDER BY createdAtEpochMs DESC
        LIMIT :limit
        """,
    )
    fun observeLatest(userId: Long, limit: Int): Flow<List<XpEventEntity>>

    @Query("SELECT COUNT(*) FROM xp_events WHERE userId = :userId AND dateEpochDay = :epochDay AND type = :type")
    suspend fun countForDayAndType(userId: Long, epochDay: Int, type: String): Int

    @Query(
        """
        SELECT userId, COALESCE(SUM(points), 0) AS total
        FROM xp_events
        WHERE dateEpochDay >= :fromEpochDay
        GROUP BY userId
        ORDER BY total DESC
        LIMIT :limit
        """,
    )
    suspend fun topUsersForRange(fromEpochDay: Int, limit: Int): List<com.example.zhivoy.data.model.UserXpTotal>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: XpEventEntity): Long

    @Query(
        """
        SELECT dateEpochDay, SUM(points) as value FROM xp_events
        WHERE userId = :userId AND dateEpochDay >= :start AND dateEpochDay <= :end
        GROUP BY dateEpochDay
        """,
    )
    suspend fun getDailyXpInRange(userId: Long, start: Int, end: Int): List<com.example.zhivoy.data.model.DayValue>

    @Query(
        """
        SELECT dateEpochDay, SUM(points) as value FROM xp_events
        WHERE userId = :userId AND dateEpochDay >= :start AND dateEpochDay <= :end
        GROUP BY dateEpochDay
        """,
    )
    fun observeDailyXpInRange(userId: Long, start: Int, end: Int): Flow<List<com.example.zhivoy.data.model.DayValue>>
}


