package com.example.zhivoy.network.api

import com.example.zhivoy.network.dto.LoginRequest
import com.example.zhivoy.network.dto.LogoutRequest
import com.example.zhivoy.network.dto.ProfileRequest
import com.example.zhivoy.network.dto.ProfileResponse
import com.example.zhivoy.network.dto.RefreshRequest
import com.example.zhivoy.network.dto.RegisterRequest
import com.example.zhivoy.network.dto.TokenPair
import com.example.zhivoy.network.dto.UserMeResponse
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

