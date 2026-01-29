package com.volovod.alta.data.repository

import com.volovod.alta.data.session.SessionStore
import com.volovod.alta.network.ApiClient
import com.volovod.alta.network.dto.SyncBatchItemDto
import com.volovod.alta.network.dto.SyncBatchRequestDto
import com.volovod.alta.network.dto.SyncBatchResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

class SyncRepository(
    private val sessionStore: SessionStore,
    private val db: com.volovod.alta.data.AppDatabase,
) {
    private val api = ApiClient.createSyncApi(sessionStore)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun enqueue(userId: Long, entityType: String, action: String, payload: Map<String, Any>) {
        withContext(Dispatchers.IO) {
            db.syncQueueDao().insert(
                com.volovod.alta.data.entities.SyncQueueEntity(
                    userId = userId,
                    entityType = entityType,
                    action = action,
                    payload = json.encodeToString(payload),
                    createdAtEpochMs = System.currentTimeMillis(),
                )
            )
        }
    }

    suspend fun migrateLocalToServer(userId: Long): Result<SyncBatchResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                // Collect all local data
                val items = mutableListOf<SyncBatchItemDto>()

                // Water entries
                db.waterDao().observeAll(userId).first().forEach { water ->
                    items.add(
                        SyncBatchItemDto(
                            entity_type = "water",
                            action = "create",
                            payload = json.encodeToJsonElement(mapOf(
                                "date_epoch_day" to water.dateEpochDay,
                                "amount_ml" to water.amountMl,
                            )),
                        )
                    )
                }

                // Food entries
                db.foodDao().observeAll(userId).first().forEach { food ->
                    items.add(
                        SyncBatchItemDto(
                            entity_type = "food",
                            action = "create",
                            payload = json.encodeToJsonElement(mapOf(
                                "date_epoch_day" to food.dateEpochDay,
                                "title" to food.title,
                                "calories" to food.calories,
                            )),
                        )
                    )
                }

                // Training entries
                db.trainingDao().observeFrom(userId, 0).first().forEach { training ->
                    items.add(
                        SyncBatchItemDto(
                            entity_type = "training",
                            action = "create",
                            payload = json.encodeToJsonElement(mapOf(
                                "date_epoch_day" to training.dateEpochDay,
                                "title" to training.title,
                                "description" to training.description,
                                "calories_burned" to training.caloriesBurned,
                                "duration_minutes" to training.durationMinutes,
                            )),
                        )
                    )
                }

                // Book entries
                db.bookDao().observeAll(userId).first().forEach { book ->
                    items.add(
                        SyncBatchItemDto(
                            entity_type = "book",
                            action = "create",
                            payload = json.encodeToJsonElement(mapOf(
                                "title" to book.title,
                                "author" to book.author,
                                "total_pages" to book.totalPages,
                            )),
                        )
                    )
                }

                // XP events
                db.xpDao().observeLatest(userId, Int.MAX_VALUE).first().forEach { xp ->
                    items.add(
                        SyncBatchItemDto(
                            entity_type = "xp_event",
                            action = "create",
                            payload = json.encodeToJsonElement(mapOf(
                                "date_epoch_day" to xp.dateEpochDay,
                                "type" to xp.type,
                                "points" to xp.points,
                                "note" to xp.note,
                            )),
                        )
                    )
                }

                // Weight entries
                db.weightDao().observeInRange(userId, 0, Int.MAX_VALUE).first().forEach { weight ->
                    items.add(
                        SyncBatchItemDto(
                            entity_type = "weight",
                            action = "upsert",
                            payload = json.encodeToJsonElement(mapOf(
                                "date_epoch_day" to weight.dateEpochDay,
                                "weight_kg" to weight.weightKg,
                            )),
                        )
                    )
                }

                // Smoke status
                db.smokeDao().observe(userId).first()?.let { smoke ->
                    items.add(
                        SyncBatchItemDto(
                            entity_type = "smoke_status",
                            action = "upsert",
                            payload = json.encodeToJsonElement(mapOf(
                                "started_at" to smoke.startedAtEpochMs,
                                "is_active" to smoke.isActive,
                                "pack_price" to smoke.packPrice,
                                "packs_per_day" to smoke.packsPerDay,
                            )),
                        )
                    )
                }

                // Steps
                db.stepsDao().observeInRange(userId, 0, Int.MAX_VALUE).first().forEach { step ->
                    items.add(
                        SyncBatchItemDto(
                            entity_type = "steps",
                            action = "upsert",
                            payload = json.encodeToJsonElement(mapOf(
                                "date_epoch_day" to step.dateEpochDay,
                                "steps" to step.value,
                            )),
                        )
                    )
                }

                if (items.isEmpty()) {
                    return@withContext Result.success(SyncBatchResponseDto(processed = 0, failed = 0))
                }

                // Send in batches of 50
                val response = api.syncBatch(SyncBatchRequestDto(items.take(50)))
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun processPending(userId: Long): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val pending = db.syncQueueDao().getPendingForUser(userId, now)
                if (pending.isEmpty()) return@withContext Result.success(0)

                val items = pending.map { queue ->
                    val payload = json.decodeFromString<JsonElement>(queue.payload)
                    SyncBatchItemDto(
                        entity_type = queue.entityType,
                        action = queue.action,
                        payload = payload,
                    )
                }

                val response = api.syncBatch(SyncBatchRequestDto(items))
                
                // Delete successfully processed items
                if (response.processed > 0) {
                    pending.take(response.processed).forEach { queue ->
                        db.syncQueueDao().deleteById(queue.id)
                    }
                }

                // Update failed items with exponential backoff
                if (response.failed > 0) {
                    pending.drop(response.processed).forEachIndexed { index, queue ->
                        val newAttempts = queue.attempts + 1
                        val backoffMs = (1000L * (1L shl newAttempts.coerceAtMost(10))).coerceAtMost(24 * 60 * 60 * 1000L) // Max 24 hours
                        val nextAttempt = now + backoffMs
                        db.syncQueueDao().updateAttempt(queue.id, newAttempts, nextAttempt)
                    }
                }

                Result.success(response.processed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun clearQueue(userId: Long) {
        withContext(Dispatchers.IO) {
            db.syncQueueDao().clearForUser(userId)
        }
    }
}
