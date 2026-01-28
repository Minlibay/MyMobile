package com.example.zhivoy.network.api

import com.example.zhivoy.network.dto.BookCreateRequestDto
import com.example.zhivoy.network.dto.BookEntryResponseDto
import com.example.zhivoy.network.dto.BookProgressRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface BookApi {
    @GET("books/me")
    suspend fun getBooks(): List<BookEntryResponseDto>

    @POST("books/me")
    suspend fun createBook(@Body request: BookCreateRequestDto): BookEntryResponseDto

    @PUT("books/me/{entry_id}")
    suspend fun updateBookProgress(
        @Path("entry_id") entryId: Long,
        @Body request: BookProgressRequestDto,
    ): BookEntryResponseDto

    @DELETE("books/me/{entry_id}")
    suspend fun deleteBook(@Path("entry_id") entryId: Long)
}
