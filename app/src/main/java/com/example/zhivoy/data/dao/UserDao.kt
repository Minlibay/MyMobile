package com.example.zhivoy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zhivoy.data.entities.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE login = :login LIMIT 1")
    suspend fun getByLogin(login: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE login = :loginOrEmail OR email = :loginOrEmail LIMIT 1")
    suspend fun getByLoginOrEmail(loginOrEmail: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT id, login FROM users WHERE id IN (:ids)")
    suspend fun getLoginsForIds(ids: List<Long>): List<UserIdLogin>
}

data class UserIdLogin(
    val id: Long,
    val login: String,
)


