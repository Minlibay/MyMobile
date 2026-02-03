package com.volovod.alta.network.api

import com.volovod.alta.network.dto.AdsConfigResponse
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










