package com.volovod.alta.network

import com.volovod.alta.data.session.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class TokenInterceptor(
    private val sessionStore: SessionStore
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Получаем access token синхронно
        val accessToken = runBlocking {
            sessionStore.getAccessToken()
        }

        val requestBuilder = originalRequest.newBuilder()
        if (accessToken != null) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        val response = chain.proceed(requestBuilder.build())

        // Если получили 401, пытаемся обновить токен
        // Используем отдельный API клиент без интерцептора, чтобы избежать циклической зависимости
        if (response.code == 401 && !originalRequest.url.encodedPath.contains("/auth/refresh")) {
            response.close()
            val newTokenPair = runBlocking {
                sessionStore.refreshToken()
            }

            if (newTokenPair != null) {
                // Повторяем запрос с новым токеном
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer ${newTokenPair.access_token}")
                    .build()
                return chain.proceed(newRequest)
            }
        }

        return response
    }
}

