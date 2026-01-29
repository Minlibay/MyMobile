package com.volovod.alta.data.repositories

import com.volovod.alta.data.dao.UserSettingsDao
import com.volovod.alta.data.entities.UserSettingsEntity
import com.volovod.alta.network.api.UserSettingsApi
import com.volovod.alta.network.dto.UserSettingsRequestDto
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
                targetWeightKg = response.target_weight_kg,
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
            target_weight_kg = entity.targetWeightKg,
            reminders_enabled = entity.remindersEnabled,
        )
        val response = userSettingsApi.upsertUserSettings(requestDto)
        return UserSettingsEntity(
            id = 0, // Not used for remote, Room will auto-generate
            userId = userId,
            calorieMode = response.calorie_mode,
            stepGoal = response.step_goal,
            calorieGoalOverride = response.calorie_goal_override,
            targetWeightKg = response.target_weight_kg,
            remindersEnabled = response.reminders_enabled,
            updatedAtEpochMs = Instant.parse(response.updated_at).toEpochMilli(),
        )
    }
}

