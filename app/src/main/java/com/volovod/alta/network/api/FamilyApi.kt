package com.volovod.alta.network.api

import com.volovod.alta.network.dto.FamilyInviteRequestDto
import com.volovod.alta.network.dto.FamilyJoinRequestDto
import com.volovod.alta.network.dto.FamilyResponseDto
import com.volovod.alta.network.dto.FamilyMemberResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface FamilyApi {
    @POST("families")
    suspend fun createFamily(@Body request: com.volovod.alta.network.dto.FamilyCreateRequestDto): FamilyResponseDto

    @GET("families/me")
    suspend fun getMyFamily(): FamilyResponseDto

    @GET("families/me/members")
    suspend fun getMyFamilyMembers(): List<FamilyMemberResponseDto>

    @POST("families/me/invite")
    suspend fun inviteUser(@Body request: FamilyInviteRequestDto): Map<String, String>

    @POST("families/join")
    suspend fun joinFamily(@Body request: FamilyJoinRequestDto): FamilyResponseDto

    @POST("families/leave")
    suspend fun leaveFamily(): Map<String, String>
}


