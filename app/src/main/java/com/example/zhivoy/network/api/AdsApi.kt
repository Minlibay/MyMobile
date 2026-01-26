package com.example.zhivoy.network.api

import com.example.zhivoy.network.dto.AdsConfigResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AdsApi {
    @GET("ads/config")
    suspend fun getConfig(
        @Query("platform") platform: String = "android",
        @Query("appVersion") appVersion: Int = 1,
        @Query("network") network: String = "yandex"
    ): AdsConfigResponse
}










