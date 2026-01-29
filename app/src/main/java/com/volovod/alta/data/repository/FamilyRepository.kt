package com.volovod.alta.data.repository

import com.volovod.alta.data.session.SessionStore
import com.volovod.alta.network.ApiClient
import com.volovod.alta.network.dto.FamilyCreateRequestDto
import com.volovod.alta.network.dto.FamilyInviteRequestDto
import com.volovod.alta.network.dto.FamilyInviteResponseDto
import com.volovod.alta.network.dto.FamilyJoinRequestDto
import com.volovod.alta.network.dto.FamilyMemberResponseDto
import com.volovod.alta.network.dto.FamilyResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FamilyRepository(
    private val sessionStore: SessionStore,
) {

    private val api = ApiClient.createFamilyApi(sessionStore)

    suspend fun createFamily(name: String): Result<FamilyResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.createFamily(FamilyCreateRequestDto(name = name))
                Result.success(res)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getMyFamily(): Result<FamilyResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.getMyFamily()
                Result.success(res)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getMyFamilyMembers(): Result<List<FamilyMemberResponseDto>> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.getMyFamilyMembers()
                Result.success(res)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun inviteUser(login: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                api.inviteUser(FamilyInviteRequestDto(login = login))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getMyInvites(): Result<List<FamilyInviteResponseDto>> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.getMyInvites()
                Result.success(res)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun acceptInvite(inviteId: Int): Result<FamilyResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.acceptInvite(inviteId)
                Result.success(res)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun declineInvite(inviteId: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                api.declineInvite(inviteId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun joinFamily(name: String): Result<FamilyResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.joinFamily(FamilyJoinRequestDto(family_name = name))
                Result.success(res)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun leaveFamily(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                api.leaveFamily()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}


