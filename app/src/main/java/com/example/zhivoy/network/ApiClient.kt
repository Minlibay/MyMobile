package com.example.zhivoy.network

import com.example.zhivoy.data.session.SessionStore
import com.example.zhivoy.network.api.AdsApi
import com.example.zhivoy.network.api.AuthApi
import com.example.zhivoy.network.api.OpenRouterApi
import com.example.zhivoy.network.api.AdminSettingsApi
import com.example.zhivoy.network.api.FamilyApi
import com.example.zhivoy.network.api.FoodApi
import com.example.zhivoy.network.api.BookApi
import com.example.zhivoy.network.api.TrainingApi
import com.example.zhivoy.network.api.XpApi
import com.example.zhivoy.network.api.SyncApi
import com.example.zhivoy.network.api.SmokeApi
import com.example.zhivoy.network.api.StepsApi
import com.example.zhivoy.network.api.WeightApi
import com.example.zhivoy.network.api.WaterApi
import com.example.zhivoy.network.api.ProfileApi
import com.example.zhivoy.network.api.UserSettingsApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://45.134.12.54/"
    private const val OPEN_ROUTER_BASE_URL = "https://openrouter.ai/api/v1/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    fun createAuthApi(sessionStore: SessionStore): AuthApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
    }

    fun createAuthApiWithoutInterceptor(): AuthApi {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
    }

    fun createAdsApi(sessionStore: SessionStore): AdsApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AdsApi::class.java)
    }

    fun createProfileApi(sessionStore: SessionStore): ProfileApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ProfileApi::class.java)
    }

    fun createUserSettingsApi(sessionStore: SessionStore): UserSettingsApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(UserSettingsApi::class.java)
    }

    fun createFamilyApi(sessionStore: SessionStore): FamilyApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FamilyApi::class.java)
    }

    fun createStepsApi(sessionStore: SessionStore): StepsApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(StepsApi::class.java)
    }

    fun createWaterApi(sessionStore: SessionStore): WaterApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WaterApi::class.java)
    }

    fun createWeightApi(sessionStore: SessionStore): WeightApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WeightApi::class.java)
    }

    fun createSmokeApi(sessionStore: SessionStore): SmokeApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SmokeApi::class.java)
    }

    fun createFoodApi(sessionStore: SessionStore): FoodApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FoodApi::class.java)
    }

    fun createTrainingApi(sessionStore: SessionStore): TrainingApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TrainingApi::class.java)
    }

    fun createBookApi(sessionStore: SessionStore): BookApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BookApi::class.java)
    }

    fun createXpApi(sessionStore: SessionStore): XpApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(XpApi::class.java)
    }

    fun createSyncApi(sessionStore: SessionStore): SyncApi {
        val okHttpClient = createOkHttpClient(sessionStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SyncApi::class.java)
    }

    fun createAdminSettingsApi(): AdminSettingsApi {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
            
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AdminSettingsApi::class.java)
    }

    fun createOpenRouterApi(): OpenRouterApi {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
            
        return Retrofit.Builder()
            .baseUrl(OPEN_ROUTER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenRouterApi::class.java)
    }

    private fun createOkHttpClient(sessionStore: SessionStore): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor(sessionStore))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
