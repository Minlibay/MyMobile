package com.volovod.alta.network.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int? = null,
    val temperature: Float? = null,
    val response_format: ResponseFormat? = null
)

@Serializable
data class Message(
    val role: String,
    val content: List<ContentPart>
)

@Serializable
data class ContentPart(
    val type: String,
    val text: String? = null,
    val image_url: ImageUrl? = null
)

@Serializable
data class ImageUrl(
    val url: String
)

@Serializable
data class ResponseFormat(
    val type: String
)

@Serializable
data class OpenRouterResponse(
    val choices: List<Choice>,
    val id: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val usage: Usage? = null
)

@Serializable
data class Usage(
    val prompt_tokens: Int? = null,
    val completion_tokens: Int? = null,
    val total_tokens: Int? = null
)

@Serializable
data class Choice(
    val message: ResponseMessage
)

@Serializable
data class ResponseMessage(
    val content: String
)

interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun getCompletion(
        @Header("Authorization") token: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}
