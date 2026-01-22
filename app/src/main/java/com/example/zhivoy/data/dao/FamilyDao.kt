package com.example.zhivoy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zhivoy.data.entities.FamilyEntity
import com.example.zhivoy.data.entities.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {
    @Query("SELECT * FROM families WHERE adminUserId = :userId OR id IN (SELECT familyId FROM family_members WHERE userId = :userId) LIMIT 1")
    fun observeUserFamily(userId: Long): Flow<FamilyEntity?>

    @Query("SELECT * FROM families WHERE adminUserId = :userId OR id IN (SELECT familyId FROM family_members WHERE userId = :userId) LIMIT 1")
    suspend fun getUserFamily(userId: Long): FamilyEntity?

    @Query(
        """
        SELECT 
            u.id as userId, 
            u.login as login, 
            fm.joinedAtEpochMs as joinedAtEpochMs,
            (SELECT COALESCE(SUM(points), 0) FROM xp_events WHERE userId = u.id) as totalXp
        FROM family_members fm
        JOIN users u ON fm.userId = u.id
        WHERE fm.familyId = :familyId
        """
    )
    fun observeFamilyMembers(familyId: Long): Flow<List<com.example.zhivoy.data.model.FamilyMemberWithUser>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFamily(family: FamilyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMemberEntity)

    @Query("DELETE FROM family_members WHERE familyId = :familyId AND userId = :userId")
    suspend fun removeMember(familyId: Long, userId: Long)

    @Query("SELECT id FROM users WHERE login = :login LIMIT 1")
    suspend fun getUserIdByLogin(login: String): Long?
}




