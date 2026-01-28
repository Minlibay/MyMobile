package com.volovod.alta.data.repositories

import com.volovod.alta.data.entities.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    fun observeUserSettings(userId: Long): Flow<UserSettingsEntity?>
    suspend fun getUserSettings(userId: Long): UserSettingsEntity?
    suspend fun upsertUserSettings(entity: UserSettingsEntity): Long

    // New functions for backend interaction
    suspend fun getRemoteUserSettings(): UserSettingsEntity?
    suspend fun upsertRemoteUserSettings(entity: UserSettingsEntity): UserSettingsEntity
}

