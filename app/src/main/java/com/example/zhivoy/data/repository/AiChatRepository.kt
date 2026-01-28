package com.example.zhivoy.data.repository

import android.util.Log
import com.example.zhivoy.data.dao.FoodDao
import com.example.zhivoy.data.entities.FoodEntryEntity
import com.example.zhivoy.network.api.ContentPart
import com.example.zhivoy.network.api.ImageUrl
import com.example.zhivoy.network.api.Message
import com.example.zhivoy.network.api.OpenRouterApi
import com.example.zhivoy.network.api.OpenRouterRequest
import com.example.zhivoy.network.api.ResponseFormat
import com.example.zhivoy.config.AiConfig
import com.example.zhivoy.util.DateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FoodAiResponse(
    val title: String,
    val calories: Int
)

class AiChatRepository(
    private val openRouterApi: OpenRouterApi,
    private val foodDao: FoodDao,
    private val foodRemoteRepository: FoodRemoteRepository,
    private val adminSettingsRepository: AdminSettingsRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun getAiConfig(): Pair<String, String> {
        val settings = adminSettingsRepository.getSettings()
        return if (settings.isSuccess) {
            val apiKey = settings.getOrNull()?.openrouter_api_key ?: ""
            val model = settings.getOrNull()?.openrouter_model ?: "anthropic/claude-3-haiku"
            Pair(apiKey, model)
        } else {
            Pair("", "anthropic/claude-3-haiku") // Fallback
        }
    }

    suspend fun processFoodInput(userId: Long, text: String?, imageBase64: String?): Result<FoodAiResponse> {
        val (apiKey, model) = getAiConfig()
        if (apiKey.isEmpty()) {
            return Result.failure(Exception("OpenRouter API key not configured"))
        }

        val systemPrompt = AiConfig.SYSTEM_PROMPT

        val contentParts = mutableListOf<ContentPart>()
        text?.let { contentParts.add(ContentPart(type = "text", text = it)) }
        imageBase64?.let { 
            contentParts.add(ContentPart(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$it")))
        }

        val messages = listOf(
            Message(role = "system", content = listOf(ContentPart(type = "text", text = systemPrompt))),
            Message(role = "user", content = contentParts)
        )

        val request = OpenRouterRequest(
            model = model,
            messages = messages
        )

        return try {
            Log.d("AiChatRepository", "Sending AI request: model=$model, hasText=${!text.isNullOrBlank()}, hasImage=${imageBase64 != null}")
            val response = openRouterApi.getCompletion("Bearer $apiKey", request)
            Log.d("AiChatRepository", "AI response received: choices=${response.choices.size}")

            val content = response.choices.firstOrNull()?.message?.content
                ?: run {
                    Log.e("AiChatRepository", "AI response has no choices or content")
                    return Result.failure(Exception("Empty response"))
                }

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
