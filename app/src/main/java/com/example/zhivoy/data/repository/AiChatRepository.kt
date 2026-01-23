package com.example.zhivoy.data.repository

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
    private val apiKey: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun processFoodInput(userId: Long, text: String?, imageBase64: String?): Result<FoodAiResponse> {
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
            model = AiConfig.MODEL,
            messages = messages,
            response_format = ResponseFormat(type = "json_object")
        )

        return try {
            val response = openRouterApi.getCompletion("Bearer $apiKey", request)
            val content = response.choices.firstOrNull()?.message?.content ?: return Result.failure(Exception("Empty response"))
            val foodData = json.decodeFromString<FoodAiResponse>(content)
            
            // Сохраняем в БД
            val entry = FoodEntryEntity(
                userId = userId,
                dateEpochDay = DateTime.epochDayNow(),
                title = foodData.title,
                calories = foodData.calories,
                createdAtEpochMs = System.currentTimeMillis()
            )
            foodDao.insert(entry)
            
            Result.success(foodData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
