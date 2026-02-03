package com.volovod.alta.network.api

import com.volovod.alta.network.dto.PrivacyPolicyAcceptResponseDto
import com.volovod.alta.network.dto.PrivacyPolicyResponseDto
import retrofit2.http.GET
import retrofit2.http.POST

interface PrivacyPolicyApi {
    @GET("privacy_policy")
    suspend fun getPrivacyPolicy(): PrivacyPolicyResponseDto

    @POST("privacy_policy/accept")
    suspend fun acceptPrivacyPolicy(): PrivacyPolicyAcceptResponseDto
}
