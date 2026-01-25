package com.example.zhivoy.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val login: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String,
    val device_id: String? = null
)

@Serializable
data class TokenPair(
    val access_token: String,
    val refresh_token: String
)

@Serializable
data class RefreshRequest(
    val refresh_token: String,
    val device_id: String? = null
)

@Serializable
data class LogoutRequest(
    val refresh_token: String
)

@Serializable
data class UserMeResponse(
    val id: Int,
    val login: String
)

@Serializable
data class AdsConfigResponse(
    val network: String,
    val units: Map<String, String>
)

@Serializable
data class HealthResponse(
    val status: String
)

@Serializable
data class ProfileRequest(
    val height_cm: Int,
    val weight_kg: Double,
    val age: Int,
    val sex: String
)

@Serializable
data class ProfileResponse(
    val height_cm: Int,
    val weight_kg: Double,
    val age: Int,
    val sex: String,
    val created_at: String,
    val updated_at: String
)

