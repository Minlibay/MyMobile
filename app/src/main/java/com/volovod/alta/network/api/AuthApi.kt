package com.volovod.alta.network.api

import com.volovod.alta.network.dto.LoginRequest
import com.volovod.alta.network.dto.LogoutRequest
import com.volovod.alta.network.dto.ProfileRequest
import com.volovod.alta.network.dto.ProfileResponse
import com.volovod.alta.network.dto.RefreshRequest
import com.volovod.alta.network.dto.RegisterRequest
import com.volovod.alta.network.dto.TokenPair
import com.volovod.alta.network.dto.UserMeResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): UserMeResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenPair

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): TokenPair

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Map<String, String>

    @GET("users/me")
    suspend fun me(): UserMeResponse

    @GET("users/me/profile")
    suspend fun getProfile(): ProfileResponse

    @PUT("users/me/profile")
    suspend fun updateProfile(@Body request: ProfileRequest): ProfileResponse
}

