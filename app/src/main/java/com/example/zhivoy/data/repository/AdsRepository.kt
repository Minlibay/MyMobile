package com.example.zhivoy.data.repository

import android.content.Context
import com.example.zhivoy.data.session.SessionStore
import com.example.zhivoy.network.ApiClient
import com.example.zhivoy.network.api.AdsApi
import com.example.zhivoy.network.dto.AdsConfigResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdsRepository(
    private val context: Context,
    private val sessionStore: SessionStore
) {
    private val adsApi: AdsApi = ApiClient.createAdsApi(sessionStore)

    suspend fun getConfig(
        platform: String = "android",
        appVersion: Int = 1,
        network: String = "yandex"
    ): Result<AdsConfigResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = adsApi.getConfig(platform, appVersion, network)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}









