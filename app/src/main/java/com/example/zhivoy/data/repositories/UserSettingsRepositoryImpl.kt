package com.example.zhivoy.data.repositories

import com.example.zhivoy.data.dao.UserSettingsDao
import com.example.zhivoy.data.entities.UserSettingsEntity
import com.example.zhivoy.network.api.UserSettingsApi
import com.example.zhivoy.network.dto.UserSettingsRequestDto
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class UserSettingsRepositoryImpl(
    private val userSettingsDao: UserSettingsDao,
    private val userSettingsApi: UserSettingsApi,
    private val userId: Long,
) : UserSettingsRepository {

    override fun observeUserSettings(userId: Long): Flow<UserSettingsEntity?> {
        return userSettingsDao.observe(userId)
    }

    override suspend fun getUserSettings(userId: Long): UserSettingsEntity? {
        return userSettingsDao.get(userId)
    }

    override suspend fun upsertUserSettings(entity: UserSettingsEntity): Long {
        return userSettingsDao.upsert(entity)
    }

    override suspend fun getRemoteUserSettings(): UserSettingsEntity? {
        return try {
            val response = userSettingsApi.getUserSettings()
            UserSettingsEntity(
                id = 0, // Not used for remote, Room will auto-generate
                userId = userId,
                calorieMode = response.calorie_mode,
                stepGoal = response.step_goal,
                calorieGoalOverride = response.calorie_goal_override,
                remindersEnabled = response.reminders_enabled,
                updatedAtEpochMs = Instant.parse(response.updated_at).toEpochMilli(),
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun upsertRemoteUserSettings(entity: UserSettingsEntity): UserSettingsEntity {
        val requestDto = UserSettingsRequestDto(
            calorie_mode = entity.calorieMode,
            step_goal = entity.stepGoal,
            calorie_goal_override = entity.calorieGoalOverride,
            reminders_enabled = entity.remindersEnabled,
        )
        val response = userSettingsApi.upsertUserSettings(requestDto)
        return UserSettingsEntity(
            id = 0, // Not used for remote, Room will auto-generate
            userId = userId,
            calorieMode = response.calorie_mode,
            stepGoal = response.step_goal,
            calorieGoalOverride = response.calorie_goal_override,
            remindersEnabled = response.reminders_enabled,
            updatedAtEpochMs = Instant.parse(response.updated_at).toEpochMilli(),
        )
    }
}

