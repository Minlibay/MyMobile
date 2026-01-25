package com.example.zhivoy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zhivoy.data.entities.ProfileEntity

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity): Long
}













