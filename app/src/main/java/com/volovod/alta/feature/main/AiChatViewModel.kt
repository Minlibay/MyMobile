package com.volovod.alta.feature.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.volovod.alta.ads.AppodealManager
import com.volovod.alta.data.repository.AdsRepository
import com.volovod.alta.data.repository.AiChatRepository
import kotlinx.coroutines.delay
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
    private val userId: Long,
    private val adsRepository: AdsRepository,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(com.volovod.alta.config.AiConfig.INITIAL_ASSISTANT_MESSAGE, false)
    ))
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isConfigured = MutableStateFlow(false)
    val isConfigured = _isConfigured.asStateFlow()

    private val _isConfiguring = MutableStateFlow(true)
    val isConfiguring = _isConfiguring.asStateFlow()

    init {
        viewModelScope.launch {
            _isConfiguring.value = true
            val settings = repository.adminSettingsRepository.getSettings()
            _isConfigured.value = settings.isSuccess && 
                settings.getOrNull()?.openrouter_api_key?.isNotBlank() == true
            _isConfiguring.value = false
        }
    }

    private suspend fun maybeShowAiInterstitial(context: Context) {
        val activity = context as? android.app.Activity ?: return

        val now = System.currentTimeMillis()
        if (now - lastInterstitialAtMs < 60_000) return

        val cfg = adsRepository.getConfig(network = "appodeal").getOrNull() ?: return
        if (!cfg.appodeal_enabled) return
        val appKey = cfg.appodeal_app_key ?: return

        AppodealManager.initializeIfNeeded(
            activity = activity,
            appKey = appKey,
            banner = cfg.appodeal_banner_enabled,
            interstitial = cfg.appodeal_interstitial_enabled,
            rewarded = cfg.appodeal_rewarded_enabled,
        )

        if (cfg.appodeal_interstitial_enabled) {
            AppodealManager.showInterstitial(activity)
            lastInterstitialAtMs = now
            delay(400)
        }
    }

    private companion object {
        private var lastInterstitialAtMs: Long = 0
    }

    fun refreshSettings() {
        viewModelScope.launch {
            _isConfiguring.value = true
            val settings = repository.adminSettingsRepository.getSettings()
            _isConfigured.value = settings.isSuccess && 
                settings.getOrNull()?.openrouter_api_key?.isNotBlank() == true
            _isConfiguring.value = false
        }
    }

    fun sendMessage(text: String?, imageUri: Uri?, context: Context) {
        if (text.isNullOrBlank() && imageUri == null) return

        val userMessage = ChatMessage(text ?: "Фото еды", true, imageUri)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            maybeShowAiInterstitial(context)
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
