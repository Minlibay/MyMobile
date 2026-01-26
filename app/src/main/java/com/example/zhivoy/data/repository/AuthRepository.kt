package com.example.zhivoy.data.repository

import android.content.Context
import android.provider.Settings
import com.example.zhivoy.data.dao.ProfileDao
import com.example.zhivoy.data.dao.UserSettingsDao
import com.example.zhivoy.data.session.SessionStore
import com.example.zhivoy.network.ApiClient
import com.example.zhivoy.network.api.AuthApi
import com.example.zhivoy.network.dto.LoginRequest
import com.example.zhivoy.network.dto.ProfileRequestDto
import com.example.zhivoy.network.dto.ProfileResponseDto
import com.example.zhivoy.network.dto.UserSettingsRequestDto
import com.example.zhivoy.network.dto.UserSettingsResponseDto
import com.example.zhivoy.network.api.AuthApi
import com.example.zhivoy.network.api.ProfileApi
import com.example.zhivoy.network.api.UserSettingsApi
import com.example.zhivoy.network.dto.LoginRequest
import com.example.zhivoy.network.dto.ProfileRequestDto
import com.example.zhivoy.network.dto.ProfileResponseDto
import com.example.zhivoy.network.dto.UserSettingsRequestDto
import com.example.zhivoy.network.dto.UserSettingsResponseDto
import com.example.zhivoy.network.dto.RegisterRequest
import com.example.zhivoy.network.dto.TokenPair
import com.example.zhivoy.network.dto.UserMeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val context: Context,
    private val sessionStore: SessionStore,
    private val profileApi: ProfileApi,
    private val userSettingsApi: UserSettingsApi,
    private val profileDao: ProfileDao,
    private val userSettingsDao: UserSettingsDao,
) {
    private val authApi: AuthApi = ApiClient.createAuthApi(sessionStore)

    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    suspend fun register(login: String, password: String): Result<UserMeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApi.register(RegisterRequest(login = login, password = password))
                val loginResponse = authApi.login(LoginRequest(login = login, password = password, device_id = getDeviceId()))

                sessionStore.setTokens(loginResponse.access_token, loginResponse.refresh_token)
                val userMe = authApi.me()
                sessionStore.setUser(userMe.id.toLong())

                // Migrate local data to backend
                val userId = userMe.id.toLong()

                val localProfile = profileDao.getByUserId(userId)
                if (localProfile != null) {
                    profileApi.upsertProfile(ProfileRequestDto(
                        height_cm = localProfile.heightCm,
                        weight_kg = localProfile.weightKg,
                        age = localProfile.age,
                        sex = localProfile.sex,
                    ))
                    profileDao.delete(userId)
                }

                val localUserSettings = userSettingsDao.get(userId)
                if (localUserSettings != null) {
                    userSettingsApi.upsertUserSettings(UserSettingsRequestDto(
                        calorie_mode = localUserSettings.calorieMode,
                        step_goal = localUserSettings.stepGoal,
                        calorie_goal_override = localUserSettings.calorieGoalOverride,
                        reminders_enabled = localUserSettings.remindersEnabled,
                    ))
                    userSettingsDao.delete(userId)
                }
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun login(login: String, password: String): Result<TokenPair> {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = getDeviceId()
                val response = authApi.login(LoginRequest(login = login, password = password, device_id = deviceId))
                
                // Сохраняем токены и userId
                sessionStore.setTokens(response.access_token, response.refresh_token)
                val userMe = authApi.me()
                sessionStore.setUser(userMe.id.toLong())

                // Migrate local data to backend
                val userId = userMe.id.toLong()

                val localProfile = profileDao.getByUserId(userId)
                if (localProfile != null) {
                    profileApi.upsertProfile(ProfileRequestDto(
                        height_cm = localProfile.heightCm,
                        weight_kg = localProfile.weightKg,
                        age = localProfile.age,
                        sex = localProfile.sex,
                    ))
                    profileDao.delete(userId) // Assuming you'll add a delete method to ProfileDao
                }

                val localUserSettings = userSettingsDao.get(userId)
                if (localUserSettings != null) {
                    userSettingsApi.upsertUserSettings(UserSettingsRequestDto(
                        calorie_mode = localUserSettings.calorieMode,
                        step_goal = localUserSettings.stepGoal,
                        calorie_goal_override = localUserSettings.calorieGoalOverride,
                        reminders_enabled = localUserSettings.remindersEnabled,
                    ))
                    userSettingsDao.delete(userId) // Assuming you'll add a delete method to UserSettingsDao
                }

                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun refreshToken(): Result<TokenPair> {
        return withContext(Dispatchers.IO) {
            try {
                val response = sessionStore.refreshToken()
                if (response != null) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Failed to refresh token"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun logout(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = sessionStore.getRefreshToken()
                if (refreshToken != null) {
                    authApi.logout(com.example.zhivoy.network.dto.LogoutRequest(refresh_token = refreshToken))
                }
                sessionStore.clear()
                Result.success(Unit)
            } catch (e: Exception) {
                // Даже если запрос не удался, очищаем локальные данные
                sessionStore.clear()
                Result.success(Unit)
            }
        }
    }

    suspend fun getCurrentUser(): Result<UserMeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApi.me()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getProfile(): Result<ProfileResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = profileApi.getProfile()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateProfile(heightCm: Int, weightKg: Double, age: Int, sex: String): Result<ProfileResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = profileApi.upsertProfile(
                    ProfileRequestDto(
                        height_cm = heightCm,
                        weight_kg = weightKg,
                        age = age,
                        sex = sex
                    )
                )
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getUserSettings(): Result<UserSettingsResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userSettingsApi.getUserSettings()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateUserSettings(
        calorieMode: String,
        stepGoal: Int,
        calorieGoalOverride: Int?,
        remindersEnabled: Boolean
    ): Result<UserSettingsResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userSettingsApi.upsertUserSettings(
                    UserSettingsRequestDto(
                        calorie_mode = calorieMode,
                        step_goal = stepGoal,
                        calorie_goal_override = calorieGoalOverride,
                        reminders_enabled = remindersEnabled
                    )
                )
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

