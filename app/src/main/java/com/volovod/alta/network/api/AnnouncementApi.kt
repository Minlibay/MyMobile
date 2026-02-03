package com.volovod.alta.network.api

import com.volovod.alta.network.dto.AnnouncementReadResponseDto
import com.volovod.alta.network.dto.AnnouncementResponseDto
import retrofit2.http.GET
import retrofit2.http.POST

interface AnnouncementApi {
    @GET("announcement")
    suspend fun getAnnouncement(): AnnouncementResponseDto

    @POST("announcement/read")
    suspend fun readAnnouncement(): AnnouncementReadResponseDto
}
