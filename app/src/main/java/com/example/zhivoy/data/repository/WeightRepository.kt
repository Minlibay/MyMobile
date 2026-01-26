package com.example.zhivoy.data.repository

import com.example.zhivoy.data.dao.WeightDao
import com.example.zhivoy.data.entities.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

class WeightRepository(
    private val weightDao: WeightDao,
) {
    fun observeLatest(userId: Long): Flow<WeightEntryEntity?> {
        return weightDao.observeLatest(userId)
    }

    fun observeLast(userId: Long, limit: Int): Flow<List<WeightEntryEntity>> {
        return weightDao.observeLast(userId, limit)
    }

    suspend fun upsert(entry: WeightEntryEntity) {
        weightDao.upsert(entry)
    }
}














