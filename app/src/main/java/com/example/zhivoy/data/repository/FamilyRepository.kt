package com.example.zhivoy.data.repository

import com.example.zhivoy.data.session.SessionStore
import com.example.zhivoy.network.ApiClient
import com.example.zhivoy.network.dto.FamilyCreateRequestDto
import com.example.zhivoy.network.dto.FamilyInviteRequestDto
import com.example.zhivoy.network.dto.FamilyJoinRequestDto
import com.example.zhivoy.network.dto.FamilyMemberResponseDto
import com.example.zhivoy.network.dto.FamilyResponseDto
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


