package com.volovod.alta.data.repository

import com.volovod.alta.network.ApiClient
import com.volovod.alta.network.dto.AdminSettingsRequestDto
import com.volovod.alta.network.dto.AdminSettingsResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminSettingsRepository {
    private val api = ApiClient.createAdminSettingsApi()

    suspend fun getSettings(): Result<AdminSettingsResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getAdminSettings())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updateSettings(
        gigachatClientId: String? = null,
        gigachatAuthKey: String? = null,
        gigachatScope: String? = null,
        apiKey: String? = null,
        model: String? = null,
    ): Result<AdminSettingsResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(
                    api.updateAdminSettings(
                        AdminSettingsRequestDto(
                            gigachat_client_id = gigachatClientId,
                            gigachat_auth_key = gigachatAuthKey,
                            gigachat_scope = gigachatScope,
                            openrouter_api_key = apiKey,
                            openrouter_model = model,
                        )
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
