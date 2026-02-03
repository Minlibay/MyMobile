package com.volovod.alta.data.repository

import android.util.Log
import com.volovod.alta.data.dao.FoodDao
import com.volovod.alta.data.entities.FoodEntryEntity
import com.volovod.alta.network.api.AiChatApi
import com.volovod.alta.network.api.AiChatMessage
import com.volovod.alta.network.api.AiChatRequest
import com.volovod.alta.config.AiConfig
import com.volovod.alta.util.DateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Serializable
data class FoodAiResponse(
    val title: String,
    val calories: Int
)

class AiChatRepository(
    private val aiChatApi: AiChatApi,
    private val foodDao: FoodDao,
    private val foodRemoteRepository: FoodRemoteRepository,
    val adminSettingsRepository: AdminSettingsRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun getAiConfig(): String {
        val settings = adminSettingsRepository.getSettings()
        return if (settings.isSuccess) {
            val authKey = settings.getOrNull()?.gigachat_auth_key ?: ""
            android.util.Log.d("AiChatRepository", "API config: authKey=${authKey.take(10)}...")
            authKey
        } else {
            android.util.Log.e("AiChatRepository", "Failed to get settings: ${settings.exceptionOrNull()?.message}")
            ""
        }
    }

    suspend fun processFoodInput(userId: Long, text: String?, imageBase64: String?): Result<FoodAiResponse> {
        val authKey = getAiConfig()
        if (authKey.isEmpty()) {
            return Result.failure(Exception("GigaChat auth key not configured"))
        }

        val systemPrompt = AiConfig.SYSTEM_PROMPT

        val userContent = listOfNotNull(text?.takeIf { it.isNotBlank() }, if (imageBase64 != null) "[image]" else null)
            .joinToString(" ")
            .ifBlank { "Фото еды" }

        val request = AiChatRequest(
            messages = listOf(
                AiChatMessage(role = "system", content = systemPrompt),
                AiChatMessage(role = "user", content = userContent)
            ),
            image_base64 = imageBase64,
            max_tokens = 1000,
            temperature = 0.3f
        )

        return try {
            Log.d("AiChatRepository", "Sending AI request: hasText=${!text.isNullOrBlank()}, hasImage=${imageBase64 != null}")
            val response = try {
                aiChatApi.chat(request)
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("AiChatRepository", "GigaChat error ${e.code()}: $errorBody")
                throw e
            }

            val content = response.content

            Log.d("AiChatRepository", "AI content: $content")
            val foodData = json.decodeFromString<FoodAiResponse>(content)
            Log.d("AiChatRepository", "Parsed food: title=${foodData.title}, calories=${foodData.calories}")

            val epochDay = DateTime.epochDayNow()
            val createdAtMs = System.currentTimeMillis()

            val remoteResult = foodRemoteRepository.createFood(
                dateEpochDay = epochDay,
                title = foodData.title,
                calories = foodData.calories,
            )

            if (remoteResult.isSuccess) {
                // Optionally cache locally for immediate UI
                val entry = FoodEntryEntity(
                    userId = userId,
                    dateEpochDay = epochDay,
                    title = foodData.title,
                    calories = foodData.calories,
                    createdAtEpochMs = createdAtMs
                )
                foodDao.insert(entry)
            } else {
                // Offline fallback
                val entry = FoodEntryEntity(
                    userId = userId,
                    dateEpochDay = epochDay,
                    title = foodData.title,
                    calories = foodData.calories,
                    createdAtEpochMs = createdAtMs
                )
                foodDao.insert(entry)
            }
            
            Result.success(foodData)
        } catch (e: Exception) {
            Log.e("AiChatRepository", "AI request failed", e)
            Result.failure(e)
        }
    }
}
