package com.example.zhivoy.feature.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zhivoy.data.repository.AiChatRepository
import com.example.zhivoy.data.repository.FoodAiResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val imageUri: Uri? = null
)

class AiChatViewModel(
    private val repository: AiChatRepository,
    private val userId: Long
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Привет! Что вы сегодня ели? Можете прислать фото или описать текстом.", false)
    ))
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun sendMessage(text: String?, imageUri: Uri?, context: Context) {
        if (text.isNullOrBlank() && imageUri == null) return

        val userMessage = ChatMessage(text ?: "Фото еды", true, imageUri)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            val base64Image = imageUri?.let { uriToBase64(it, context) }
            val result = repository.processFoodInput(userId, text, base64Image)
            
            result.onSuccess { food ->
                _messages.value = _messages.value + ChatMessage(
                    "Понял! Добавил: ${food.title} (${food.calories} ккал).",
                    false
                )
            }.onFailure {
                _messages.value = _messages.value + ChatMessage(
                    "Извините, не удалось распознать еду. Попробуйте еще раз или опишите подробнее.",
                    false
                )
            }
            _isLoading.value = false
        }
    }

    private fun uriToBase64(uri: Uri, context: Context): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
