package com.example.zhivoy.data.repository

import android.content.Context
import android.provider.Settings
import com.example.zhivoy.data.session.SessionStore
import com.example.zhivoy.network.ApiClient
import com.example.zhivoy.network.api.AuthApi
import com.example.zhivoy.network.dto.LoginRequest
import com.example.zhivoy.network.dto.ProfileRequest
import com.example.zhivoy.network.dto.ProfileResponse
import com.example.zhivoy.network.dto.RegisterRequest
import com.example.zhivoy.network.dto.TokenPair
import com.example.zhivoy.network.dto.UserMeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val context: Context,
    private val sessionStore: SessionStore
) {
    private val authApi: AuthApi = ApiClient.createAuthApi(sessionStore)

    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    suspend fun register(login: String, password: String): Result<UserMeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApi.register(RegisterRequest(login = login, password = password))
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

    suspend fun getProfile(): Result<ProfileResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApi.getProfile()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateProfile(heightCm: Int, weightKg: Double, age: Int, sex: String): Result<ProfileResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApi.updateProfile(
                    ProfileRequest(
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
}

