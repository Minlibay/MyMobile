package com.example.zhivoy.data.repository

import com.example.zhivoy.data.session.SessionStore
import com.example.zhivoy.network.ApiClient
import com.example.zhivoy.network.dto.BookCreateRequestDto
import com.example.zhivoy.network.dto.BookEntryResponseDto
import com.example.zhivoy.network.dto.BookProgressRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookRemoteRepository(
    sessionStore: SessionStore,
) {
    private val api = ApiClient.createBookApi(sessionStore)

    suspend fun getBooks(): Result<List<BookEntryResponseDto>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getBooks())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createBook(
        title: String,
        author: String? = null,
        totalPages: Int,
    ): Result<BookEntryResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(
                    api.createBook(
                        BookCreateRequestDto(
                            title = title,
                            author = author,
                            total_pages = totalPages,
                        )
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updateBookProgress(
        entryId: Long,
        pagesRead: Int,
    ): Result<BookEntryResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(
                    api.updateBookProgress(
                        entryId = entryId,
                        BookProgressRequestDto(pages_read = pagesRead),
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteBook(entryId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                api.deleteBook(entryId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
