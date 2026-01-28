package com.example.zhivoy.feature.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zhivoy.LocalAppDatabase
import com.example.zhivoy.LocalSessionStore
import com.example.zhivoy.data.entities.SmokeStatusEntity
import com.example.zhivoy.data.repository.SmokeRemoteRepository
import com.example.zhivoy.data.repository.WeightRemoteRepository
import com.example.zhivoy.data.repository.XpRemoteRepository
import com.example.zhivoy.ui.components.ModernCard
import com.example.zhivoy.ui.components.ModernButton
import com.example.zhivoy.ui.components.ModernTextField
import com.example.zhivoy.util.DateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import com.example.zhivoy.network.ApiClient

@Composable
fun HealthScreen() {
    val db = LocalAppDatabase.current
    val sessionStore = LocalSessionStore.current
    val context = LocalContext.current
    val authRepository = remember { com.example.zhivoy.data.repository.AuthRepository(
        context,
        sessionStore,
        ApiClient.createProfileApi(sessionStore),
        ApiClient.createUserSettingsApi(sessionStore),
        db.profileDao(),
        db.userSettingsDao(),
    ) }
    val session by sessionStore.session.collectAsState(initial = null)
    val userId = session?.userId
    val today = DateTime.epochDayNow()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val weightRemoteRepository = remember(sessionStore) { WeightRemoteRepository(sessionStore) }
    val smokeRemoteRepository = remember(sessionStore) { SmokeRemoteRepository(sessionStore) }
    val xpRemoteRepository = remember(sessionStore) { XpRemoteRepository(sessionStore) }

    val latestWeight by (if (userId != null) db.weightDao().observeLatest(userId) else kotlinx.coroutines.flow.flowOf(null))
        .collectAsState(initial = null)

    val profileState = remember(userId) { mutableStateOf<com.example.zhivoy.data.entities.ProfileEntity?>(null) }
    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        authRepository.getProfile().fold(
            onSuccess = { profileResponse ->
                profileState.value = com.example.zhivoy.data.entities.ProfileEntity(
                    id = 0, // Not used for remote profile
                    userId = userId,
                    heightCm = profileResponse.height_cm,
                    weightKg = profileResponse.weight_kg,
                    age = profileResponse.age,
                    sex = profileResponse.sex,
                    createdAtEpochMs = Instant.parse(profileResponse.created_at).toEpochMilli(),
                    updatedAtEpochMs = Instant.parse(profileResponse.updated_at).toEpochMilli(),
                )
            },
            onFailure = { /* Handle error or show a message */ }
        )
    }

    val weight = latestWeight?.weightKg ?: profileState.value?.weightKg
    val heightCm = profileState.value?.heightCm

    val bmi = if (weight != null && heightCm != null) {
        val h = heightCm.toDouble() / 100.0
        if (h <= 0.0) null else weight / h.pow(2.0)
    } else null

    val smokeStatus by (if (userId != null) db.smokeDao().observe(userId) else kotlinx.coroutines.flow.flowOf(null))
        .collectAsState(initial = null)

    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect

        weightRemoteRepository.getWeightRange(start = today - 60, end = today).fold(
            onSuccess = { rows ->
                withContext(Dispatchers.IO) {
                    val nowMs = System.currentTimeMillis()
                    rows.forEach { r ->
                        db.weightDao().upsert(
                            com.example.zhivoy.data.entities.WeightEntryEntity(
                                userId = userId,
                                dateEpochDay = r.date_epoch_day,
                                weightKg = r.weight_kg,
                                createdAtEpochMs = nowMs,
                            )
                        )
                    }
                }
            },
            onFailure = { }
        )

        smokeRemoteRepository.getSmokeStatus().fold(
            onSuccess = { r ->
                withContext(Dispatchers.IO) {
                    val startedAtMs = Instant.parse(r.started_at).toEpochMilli()
                    val updatedAtMs = Instant.parse(r.updated_at).toEpochMilli()
                    db.smokeDao().upsert(
                        SmokeStatusEntity(
                            userId = userId,
                            startedAtEpochMs = startedAtMs,
                            isActive = r.is_active,
                            packPrice = r.pack_price,
                            packsPerDay = r.packs_per_day,
                            updatedAtEpochMs = updatedAtMs,
                        )
                    )
                }
            },
            onFailure = {
                withContext(Dispatchers.IO) {
                    val existing = db.smokeDao().get(userId)
                    if (existing == null) {
                        val now = System.currentTimeMillis()
                        db.smokeDao().upsert(
                            SmokeStatusEntity(
                                userId = userId,
                                startedAtEpochMs = now,
                                isActive = true,
                                packPrice = 0.0,
                                packsPerDay = 0.0,
                                updatedAtEpochMs = now,
                            ),
                        )
                    }
                }
            }
        )
    }

    var nowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(smokeStatus?.startedAtEpochMs) {
        while (true) {
            nowEpochMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    val startedAt = smokeStatus?.startedAtEpochMs
    val secondsNoSmoke = if (startedAt != null) ((nowEpochMs - startedAt) / 1000).coerceAtLeast(0) else 0
    val daysNoSmoke = secondsNoSmoke / 86_400.0
    val moneySaved = daysNoSmoke * (smokeStatus?.packsPerDay ?: 0.0) * (smokeStatus?.packPrice ?: 0.0)

    var packPriceText by remember(smokeStatus?.packPrice) {
        mutableStateOf((smokeStatus?.packPrice ?: 0.0).takeIf { it > 0 }?.toString() ?: "")
    }
    var packsPerDayText by remember(smokeStatus?.packsPerDay) {
        mutableStateOf((smokeStatus?.packsPerDay ?: 0.0).takeIf { it > 0 }?.toString() ?: "")
    }

    // Нативная анимация пульсации сердца
    val infiniteTransition = rememberInfiniteTransition(label = "heartBeat")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Здоровье",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFE91E63),
                modifier = Modifier
                    .size(40.dp)
                    .scale(heartScale)
            )
        }

        ModernCard {
            Text(
                text = "ИМТ и рекомендации",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when {
                    bmi == null -> "Заполните профиль (рост/вес), чтобы рассчитать ИМТ."
                    bmi < 18.5 -> "ИМТ: ${"%.1f".format(bmi)} • Недостаточный вес"
                    bmi < 25.0 -> "ИМТ: ${"%.1f".format(bmi)} • Норма"
                    bmi < 30.0 -> "ИМТ: ${"%.1f".format(bmi)} • Избыточный вес"
                    else -> "ИМТ: ${"%.1f".format(bmi)} • Ожирение"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (bmi == null) "—" else "Рекомендация: держите ИМТ в диапазоне 18.5–24.9. При необходимости — корректируйте питание и активность.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ModernCard {
            Text(
                text = "Я не курю",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Вы не курите уже", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${(secondsNoSmoke / 3600).toInt()} ч ${(secondsNoSmoke % 3600 / 60).toInt()} мин",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Сэкономлено", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${"%.0f".format(moneySaved)} ₽",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            ModernButton(
                text = "Я покурил",
                onClick = {
                    if (userId == null) return@ModernButton
                    scope.launch {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val nowMs = System.currentTimeMillis()
                        val current = withContext(Dispatchers.IO) { db.smokeDao().get(userId) }
                        val packPrice = current?.packPrice ?: 0.0
                        val packsPerDay = current?.packsPerDay ?: 0.0

                        smokeRemoteRepository.upsertSmokeStatus(
                            startedAtIso = Instant.ofEpochMilli(nowMs).toString(),
                            isActive = true,
                            packPrice = packPrice,
                            packsPerDay = packsPerDay,
                        )

                        withContext(Dispatchers.IO) {
                            val local = (current ?: SmokeStatusEntity(
                                userId = userId,
                                startedAtEpochMs = nowMs,
                                isActive = true,
                                packPrice = packPrice,
                                packsPerDay = packsPerDay,
                                updatedAtEpochMs = nowMs,
                            ))
                            db.smokeDao().upsert(
                                local.copy(
                                    startedAtEpochMs = nowMs,
                                    updatedAtEpochMs = nowMs,
                                ),
                            )
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Настройки",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ModernTextField(
                    value = packPriceText,
                    onValueChange = { newValue ->
                        // Разрешаем только цифры и одну точку/запятую для десятичных дробей
                        val filtered = newValue.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                        // Заменяем запятую на точку для корректного парсинга
                        val formatted = filtered.replace(',', '.')
                        // Разрешаем только одну точку
                        if (formatted.count { it == '.' } <= 1) {
                            packPriceText = formatted
                        }
                    },
                    label = "Цена пачки",
                    modifier = Modifier.weight(1f)
                )
                ModernTextField(
                    value = packsPerDayText,
                    onValueChange = { newValue ->
                        // Разрешаем только цифры и одну точку/запятую для десятичных дробей
                        val filtered = newValue.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                        // Заменяем запятую на точку для корректного парсинга
                        val formatted = filtered.replace(',', '.')
                        // Разрешаем только одну точку
                        if (formatted.count { it == '.' } <= 1) {
                            packsPerDayText = formatted
                        }
                    },
                    label = "Пачек/день",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ModernButton(
                text = "Сохранить",
                onClick = {
                    if (userId == null) return@ModernButton
                    val price = packPriceText.toDoubleOrNull() ?: 0.0
                    val ppd = packsPerDayText.toDoubleOrNull() ?: 0.0
                    scope.launch {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val current = withContext(Dispatchers.IO) { db.smokeDao().get(userId) }
                        val startedAtMs = current?.startedAtEpochMs ?: System.currentTimeMillis()
                        smokeRemoteRepository.upsertSmokeStatus(
                            startedAtIso = Instant.ofEpochMilli(startedAtMs).toString(),
                            isActive = current?.isActive ?: true,
                            packPrice = price,
                            packsPerDay = ppd,
                        )
                        withContext(Dispatchers.IO) {
                            val localCurrent = db.smokeDao().get(userId) ?: return@withContext
                            // Проверяем, изменились ли значения перед обновлением
                            val isChanged = localCurrent.packPrice != price || localCurrent.packsPerDay != ppd
                            
                            db.smokeDao().upsert(
                                localCurrent.copy(
                                    packPrice = price,
                                    packsPerDay = ppd,
                                    updatedAtEpochMs = System.currentTimeMillis(),
                                ),
                            )
                            
                            // XP за день без курения (простая логика: 1 событие в день)
                            // Начисляем XP только если значения действительно изменились
                            if (isChanged) {
                                // Server-first XP event
                                xpRemoteRepository.createXpEvent(
                                    dateEpochDay = today,
                                    type = "nosmoke",
                                    points = 10,
                                    note = "No smoke settings updated",
                                )
                                // Cache locally (always) so UI updates immediately
                                db.xpDao().insert(
                                    com.example.zhivoy.data.entities.XpEventEntity(
                                        userId = userId,
                                        dateEpochDay = today,
                                        type = "nosmoke",
                                        points = 10,
                                        note = "No smoke settings updated",
                                        createdAtEpochMs = System.currentTimeMillis(),
                                    ),
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}


