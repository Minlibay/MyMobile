package com.volovod.alta.network.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class AiChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class AiChatRequest(
    val messages: List<AiChatMessage>,
    val image_base64: String? = null,
    val max_tokens: Int? = null,
    val temperature: Float? = null,
    val model: String? = null,
)

@Serializable
data class AiChatResponse(
    val content: String,
)

interface AiChatApi {
    @POST("ai/chat")
    suspend fun chat(@Body request: AiChatRequest): AiChatResponse
}
