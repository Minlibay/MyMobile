package com.example.zhivoy.data.repository

import com.example.zhivoy.data.dao.TrainingDao
import com.example.zhivoy.data.entities.TrainingEntity
import kotlinx.coroutines.flow.Flow

class TrainingRepository(
    private val trainingDao: TrainingDao,
) {
    fun observeFrom(userId: Long, fromEpochDay: Int): Flow<List<TrainingEntity>> {
        return trainingDao.observeFrom(userId, fromEpochDay)
    }

    suspend fun insert(training: TrainingEntity): Long {
        return trainingDao.insert(training)
    }

    suspend fun deleteById(id: Long) {
        trainingDao.deleteById(id)
    }
}





