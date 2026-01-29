package com.volovod.alta.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import com.volovod.alta.ui.components.ModernSnackbarHost
import com.volovod.alta.ui.components.showSuccess
import com.volovod.alta.ui.components.SkeletonStatCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import com.volovod.alta.LocalAppDatabase
import com.volovod.alta.LocalSessionStore
import com.volovod.alta.data.entities.AchievementEntity
import com.volovod.alta.data.repository.AiChatRepository
import com.volovod.alta.data.repository.AdminSettingsRepository
import com.volovod.alta.data.repository.AdsRepository
import com.volovod.alta.data.repository.FoodRemoteRepository
import com.volovod.alta.data.repository.BookRemoteRepository
import com.volovod.alta.data.repository.XpRemoteRepository
import com.volovod.alta.data.repository.SyncRepository
import com.volovod.alta.data.repository.WaterRepository
import com.volovod.alta.data.repository.WeightRemoteRepository
import com.volovod.alta.network.api.OpenRouterApi
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.serialization.json.Json
import com.volovod.alta.data.entities.FoodEntryEntity
import com.volovod.alta.data.entities.WeightEntryEntity
import com.volovod.alta.feature.main.brain.AddBookDialog
import com.volovod.alta.feature.main.home.AddFoodDialog
import com.volovod.alta.feature.main.home.AddWeightDialog
import com.volovod.alta.feature.main.home.AiChatDialog
import com.volovod.alta.feature.main.home.GoalsDialog
import com.volovod.alta.ui.components.GradientCard
import com.volovod.alta.ui.components.ModernCard
import com.volovod.alta.ui.components.ModernOutlinedButton
import com.volovod.alta.ui.components.StatCard
import com.volovod.alta.ui.components.WeeklyLineChart
import com.volovod.alta.util.Calories
import com.volovod.alta.util.DateTime
import com.volovod.alta.util.Leveling
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen() {
    val db = LocalAppDatabase.current
    val sessionStore = LocalSessionStore.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val authRepository = remember(context, sessionStore, db) { 
        com.volovod.alta.data.repository.AuthRepository(
            context,
            sessionStore,
            com.volovod.alta.network.ApiClient.createProfileApi(sessionStore),
            com.volovod.alta.network.ApiClient.createUserSettingsApi(sessionStore),
            db.profileDao(),
            db.userSettingsDao(),
            SyncRepository(sessionStore, db),
        )
    }
    val adminSettingsRepository = remember { AdminSettingsRepository() }
    val waterRepository = remember(sessionStore) {
        WaterRepository(sessionStore)
    }
    val foodRemoteRepository = remember(sessionStore) {
        FoodRemoteRepository(sessionStore)
    }
    val adsRepository = remember(context, sessionStore) {
        AdsRepository(context, sessionStore)
    }
    val weightRemoteRepository = remember(sessionStore) {
        WeightRemoteRepository(sessionStore)
    }
    val bookRemoteRepository = remember(sessionStore) {
        BookRemoteRepository(sessionStore)
    }
    val xpRemoteRepository = remember(sessionStore) {
        XpRemoteRepository(sessionStore)
    }
    val syncRepository = remember(sessionStore, db) {
        SyncRepository(sessionStore, db)
    }
    val session by sessionStore.session.collectAsState(initial = null)
    val userId = session?.userId
    val today = DateTime.epochDayNow()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Stats for charts (observe changes in real-time)
    val stepsWeekEntries by (if (userId != null) db.stepsDao().observeInRange(userId, today - 6, today) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val caloriesWeekEntries by (if (userId != null) db.foodDao().observeDailyCaloriesInRange(userId, today - 6, today) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val xpWeekEntries by (if (userId != null) db.xpDao().observeDailyXpInRange(userId, today - 6, today) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val stepsWeek = remember(stepsWeekEntries, today) {
        (today - 6..today).map { day -> stepsWeekEntries.find { it.dateEpochDay == day }?.value ?: 0 }
    }
    val caloriesWeek = remember(caloriesWeekEntries, today) {
        (today - 6..today).map { day -> caloriesWeekEntries.find { it.dateEpochDay == day }?.value ?: 0 }
    }
    val xpWeek = remember(xpWeekEntries, today) {
        (today - 6..today).map { day -> xpWeekEntries.find { it.dateEpochDay == day }?.value ?: 0 }
    }
    
    val dayLabels = remember {
        (0..6).reversed().map {
            java.time.LocalDate.now().minusDays(it.toLong()).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("ru"))
        }
    }

    val stepsToday by (if (userId != null) db.stepsDao().observeTotalForDay(userId, today) else kotlinx.coroutines.flow.flowOf(0))
        .collectAsState(initial = 0)
    val caloriesToday by (if (userId != null) db.foodDao().observeTotalCaloriesForDay(userId, today) else kotlinx.coroutines.flow.flowOf(0))
        .collectAsState(initial = 0)
    val xpTotal by (if (userId != null) db.xpDao().observeTotal(userId) else kotlinx.coroutines.flow.flowOf(0))
        .collectAsState(initial = 0)
    val localWaterToday by (if (userId != null) db.waterDao().observeTotalForDay(userId, today) else kotlinx.coroutines.flow.flowOf(0))
        .collectAsState(initial = 0)
    var remoteWaterToday by remember(userId) { mutableStateOf<Int?>(null) }
    var isSavingWater by remember { mutableStateOf(false) }
    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        waterRepository.getWaterRange(start = today, end = today).fold(
            onSuccess = { entries ->
                remoteWaterToday = entries.sumOf { it.amount_ml }
            },
            onFailure = {
                // Fallback to local DB if offline / server error
            },
        )
    }
    val waterToday: Int = remoteWaterToday ?: (localWaterToday ?: 0)
    val levelInfo = remember(xpTotal) { Leveling.levelInfo(xpTotal) }

    // Settings + profile -> goals
    val settings = remember(userId) { mutableStateOf<com.volovod.alta.data.entities.UserSettingsEntity?>(null) }
    val profileState = remember(userId) { mutableStateOf<com.volovod.alta.data.entities.ProfileEntity?>(null) }
    val isLoadingProfile = remember(userId) { mutableStateOf(true) }
    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        isLoadingProfile.value = true
        authRepository.getProfile().fold(
            onSuccess = { profileResponse ->
                profileState.value = com.volovod.alta.data.entities.ProfileEntity(
                    id = 0, // Not used for remote profile
                    userId = userId,
                    heightCm = profileResponse.height_cm,
                    weightKg = profileResponse.weight_kg,
                    age = profileResponse.age,
                    sex = profileResponse.sex,
                    createdAtEpochMs = java.time.Instant.parse(profileResponse.created_at).toEpochMilli(),
                    updatedAtEpochMs = java.time.Instant.parse(profileResponse.updated_at).toEpochMilli(),
                )
            },
            onFailure = { /* Handle error or show a message */ }
        )
        authRepository.getUserSettings().fold(
            onSuccess = { userSettingsResponse ->
                settings.value = com.volovod.alta.data.entities.UserSettingsEntity(
                    id = 0, // Not used for remote
                    userId = userId,
                    calorieMode = userSettingsResponse.calorie_mode,
                    stepGoal = userSettingsResponse.step_goal,
                    calorieGoalOverride = userSettingsResponse.calorie_goal_override,
                    targetWeightKg = userSettingsResponse.target_weight_kg,
                    remindersEnabled = userSettingsResponse.reminders_enabled,
                    updatedAtEpochMs = java.time.Instant.parse(userSettingsResponse.updated_at).toEpochMilli(),
                )
            },
            onFailure = { /* Handle error or show a message */ }
        )
        isLoadingProfile.value = false
    }

    val remindersEnabled = settings.value?.remindersEnabled ?: true
    LaunchedEffect(remindersEnabled) {
        if (remindersEnabled) {
            com.volovod.alta.notifications.ReminderManager.schedulePeriodicReminders(context)
        } else {
            com.volovod.alta.notifications.ReminderManager.cancelReminder(context, "stretch")
            com.volovod.alta.notifications.ReminderManager.cancelReminder(context, "book")
        }
    }

    val stepGoal = settings.value?.stepGoal ?: 8000
    val calorieGoal = run {
        val override = settings.value?.calorieGoalOverride
        if (override != null) override
        else {
            val profile = profileState.value
            if (profile != null) Calories.calorieTarget(Calories.tdee(profile), settings.value?.calorieMode ?: "maintain") else 0
        }
    }

    var showGoals by remember { mutableStateOf(false) }

    // Квесты (состояние)
    val trainingsWeek by (if (userId != null) db.trainingDao().observeFrom(userId, today - 6) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val books by (if (userId != null) db.bookDao().observeAll(userId) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val smokeStatus by (if (userId != null) db.smokeDao().observe(userId) else kotlinx.coroutines.flow.flowOf(null))
        .collectAsState(initial = null)

    val hasTrainingToday = trainingsWeek.any { it.dateEpochDay == today }
    val hasBookToday = books.any { DateTime.epochDayFromEpochMs(it.createdAtEpochMs) == today }
    val noSmokeToday = smokeStatus?.isActive == true // упрощенно: активен — значит "не курю"

    // Вычисляем сожженные калории за сегодня
    val caloriesBurnedFromTrainings = remember(trainingsWeek, today) {
        trainingsWeek
            .filter { it.dateEpochDay == today }
            .sumOf { it.caloriesBurned.toLong() }
            .toInt()
    }
    // Калории за ходьбу: примерно 0.04 ккал на шаг (для среднего человека)
    val caloriesBurnedFromWalking = remember(stepsToday) {
        (stepsToday * 0.04).toInt()
    }
    val totalCaloriesBurned = caloriesBurnedFromTrainings + caloriesBurnedFromWalking

    val latestWeightEntry by (if (userId != null) db.weightDao().observeLatest(userId) else kotlinx.coroutines.flow.flowOf(null))
        .collectAsState(initial = null)
    val latestWeightEpochDay = latestWeightEntry?.dateEpochDay
    val shouldPromptWeight = userId != null && (latestWeightEpochDay == null || (today - latestWeightEpochDay) >= 7)

    val tdee = remember(profileState.value) {
        val p = profileState.value
        if (p != null) Calories.tdee(p) else null
    }
    val netCaloriesToday = caloriesToday - totalCaloriesBurned
    val deficitToday = (tdee?.let { it - netCaloriesToday })
    val plannedDeficit = (tdee?.let { it - calorieGoal })
    val plannedLossKgPerWeek = plannedDeficit?.takeIf { it > 0 }?.let { (it * 7.0) / 7700.0 }

    val waterGoal = 2000 // мл
    val waterDone = waterToday >= waterGoal
    
    var showConfetti by remember { mutableStateOf(false) }

    val stepsDone = stepGoal > 0 && stepsToday >= stepGoal
    val caloriesDone = calorieGoal > 0 && caloriesToday >= calorieGoal
    // waterDone is already defined above

    suspend fun awardOnce(type: String, points: Int, note: String) {
        if (userId == null) return
        val count = db.xpDao().countForDayAndType(userId, today, type)
        if (count > 0) return
        
        // Вибрация при выполнении квеста
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        showConfetti = true
        
        // Server-first XP event
        xpRemoteRepository.createXpEvent(
            dateEpochDay = today,
            type = type,
            points = points,
            note = note,
        )
        
        // Cache locally (always) so UI updates immediately
        db.xpDao().insert(
            com.volovod.alta.data.entities.XpEventEntity(
                userId = userId,
                dateEpochDay = today,
                type = type,
                points = points,
                note = note,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    LaunchedEffect(userId, stepsDone, caloriesDone, hasTrainingToday, hasBookToday, noSmokeToday, waterDone) {
        if (userId == null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            if (stepsDone) {
                awardOnce("quest_steps", 20, "Quest: steps")
                if (stepsToday >= 10000) {
                    db.achievementDao().insert(AchievementEntity(userId = userId, code = "steps_10k", createdAtEpochMs = System.currentTimeMillis()))
                }
            }
            if (caloriesDone) awardOnce("quest_calories", 20, "Quest: calories")
            if (hasTrainingToday) awardOnce("quest_training", 30, "Quest: training")
            if (hasBookToday) {
                awardOnce("quest_book", 20, "Quest: book")
                db.achievementDao().insert(AchievementEntity(userId = userId, code = "book_worm", createdAtEpochMs = System.currentTimeMillis()))
            }
            if (noSmokeToday) awardOnce("quest_nosmoke", 10, "Quest: no smoke")
            if (waterDone) {
                awardOnce("quest_water", 15, "Quest: water")
                db.achievementDao().insert(AchievementEntity(userId = userId, code = "water_hero", createdAtEpochMs = System.currentTimeMillis()))
            }
        }
    }

    var showAddFood by remember { mutableStateOf(false) }
    var showAddWeight by remember { mutableStateOf(false) }
    var showAddBook by remember { mutableStateOf(false) }
    var showAiChat by remember { mutableStateOf(false) }

    val openRouterApi = remember {
        // ВАЖНО: используем Json с ignoreUnknownKeys, иначе ответ OpenRouter с лишними полями
        // (id, created, model, usage и т.п.) будет падать при десериализации.
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenRouterApi::class.java)
    }

    val aiChatViewModel = remember(userId) {
        if (userId != null) {
            AiChatViewModel(
                AiChatRepository(openRouterApi, db.foodDao(), foodRemoteRepository, adminSettingsRepository),
                userId,
                adsRepository,
            )
        } else null
    }
    
    // Нативное конфетти
    val confettiAnim = remember { Animatable(0f) }

    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            confettiAnim.snapTo(0f)
            confettiAnim.animateTo(1f, animationSpec = tween(1500, easing = LinearOutSlowInEasing))
            showConfetti = false
        }
    }

    if (showAddFood) {
        AddFoodDialog(
            onDismiss = { showAddFood = false },
            onAdd = { title, calories ->
                if (userId == null) return@AddFoodDialog
                scope.launch {
                    val remote = foodRemoteRepository.createFood(today, title, calories)
                    if (remote.isFailure) {
                        withContext(Dispatchers.IO) {
                            db.foodDao().insert(
                                FoodEntryEntity(
                                    userId = userId,
                                    dateEpochDay = today,
                                    title = title,
                                    calories = calories,
                                    createdAtEpochMs = System.currentTimeMillis(),
                                ),
                            )
                        }
                    }

                    // Server-first XP event
                    xpRemoteRepository.createXpEvent(
                        dateEpochDay = today,
                        type = "calories",
                        points = 5,
                        note = "Food: $title",
                    )

                    // Cache locally (always) so UI updates immediately
                    withContext(Dispatchers.IO) {
                        db.xpDao().insert(
                            com.volovod.alta.data.entities.XpEventEntity(
                                userId = userId,
                                dateEpochDay = today,
                                type = "calories",
                                points = 5,
                                note = "Food: $title",
                                createdAtEpochMs = System.currentTimeMillis(),
                            ),
                        )
                    }
                    snackbarHostState.showSuccess("Еда добавлена! +5 XP")
                }
                showAddFood = false
            },
        )
    }

    if (showAddWeight) {
        AddWeightDialog(
            onDismiss = { showAddWeight = false },
            onAdd = { weightKg ->
                if (userId == null) return@AddWeightDialog
                scope.launch {
                    // Server-first
                    weightRemoteRepository.upsertWeight(today, weightKg)
                    withContext(Dispatchers.IO) {
                        db.weightDao().upsert(
                            WeightEntryEntity(
                                userId = userId,
                                dateEpochDay = today,
                                weightKg = weightKg,
                                createdAtEpochMs = System.currentTimeMillis(),
                            ),
                        )
                        // Server-first XP event
                        xpRemoteRepository.createXpEvent(
                            dateEpochDay = today,
                            type = "weight",
                            points = 10,
                            note = "Weight check-in",
                        )
                        // Cache locally (always) so UI updates immediately
                        db.xpDao().insert(
                            com.volovod.alta.data.entities.XpEventEntity(
                                userId = userId,
                                dateEpochDay = today,
                                type = "weight",
                                points = 10,
                                note = "Weight check-in",
                                createdAtEpochMs = System.currentTimeMillis(),
                            ),
                        )
                    }
                    snackbarHostState.showSuccess("Вес сохранен! +10 XP")
                }
                showAddWeight = false
            },
        )
    }

    if (showAddBook) {
        AddBookDialog(
            onDismiss = { showAddBook = false },
            onAdd = { title, author, pages ->
                if (userId == null) return@AddBookDialog
                scope.launch {
                    val remote = bookRemoteRepository.createBook(title, author, pages)

                    withContext(Dispatchers.IO) {
                        // Cache locally (always) so UI updates immediately
                        db.bookDao().insert(
                            com.volovod.alta.data.entities.BookEntryEntity(
                                userId = userId,
                                title = title,
                                author = author,
                                totalPages = pages,
                                pagesRead = 0,
                                createdAtEpochMs = System.currentTimeMillis(),
                            ),
                        )
                        // Server-first XP event
                        xpRemoteRepository.createXpEvent(
                            dateEpochDay = today,
                            type = "book",
                            points = 25,
                            note = "Book: $title",
                        )
                        // Cache locally (always) so UI updates immediately
                        db.xpDao().insert(
                            com.volovod.alta.data.entities.XpEventEntity(
                                userId = userId,
                                dateEpochDay = today,
                                type = "book",
                                points = 25,
                                note = "Book: $title",
                                createdAtEpochMs = System.currentTimeMillis(),
                            ),
                        )
                    }
                    snackbarHostState.showSuccess("Книга добавлена! +25 XP")
                }
                showAddBook = false
            },
        )
    }

    if (showGoals) {
        GoalsDialog(
            initialStepGoal = stepGoal,
            initialMode = settings.value?.calorieMode ?: "maintain",
            initialCalorieOverride = settings.value?.calorieGoalOverride,
            initialTargetWeightKg = settings.value?.targetWeightKg,
            initialRemindersEnabled = remindersEnabled,
            onDismiss = { showGoals = false },
            onSave = { newStepGoal, newMode, override, targetWeightKg, newReminders ->
                if (userId == null) return@GoalsDialog
                scope.launch {
                    authRepository.updateUserSettings(
                        calorieMode = newMode,
                        stepGoal = newStepGoal,
                        calorieGoalOverride = override,
                        targetWeightKg = targetWeightKg,
                        remindersEnabled = newReminders,
                    ).fold(
                        onSuccess = { updatedSettings ->
                            settings.value = com.volovod.alta.data.entities.UserSettingsEntity(
                                id = updatedSettings.id.toLong(),
                                userId = userId,
                                calorieMode = updatedSettings.calorie_mode,
                                stepGoal = updatedSettings.step_goal,
                                calorieGoalOverride = updatedSettings.calorie_goal_override,
                                targetWeightKg = updatedSettings.target_weight_kg,
                                remindersEnabled = updatedSettings.reminders_enabled,
                                updatedAtEpochMs = java.time.Instant.parse(updatedSettings.updated_at).toEpochMilli(),
                            )
                        },
                        onFailure = { /* Handle error */ }
                    )
                }
                showGoals = false
            },
        )
    }

    if (showAiChat) {
        if (aiChatViewModel != null) {
            // Refresh settings when opening AI chat
            LaunchedEffect(showAiChat) {
                aiChatViewModel.refreshSettings()
            }
            if (aiChatViewModel.isConfigured.collectAsState(initial = false).value) {
                AiChatDialog(
                    viewModel = aiChatViewModel,
                    onDismiss = { showAiChat = false }
                )
            } else {
                // Показать сообщение, что нужно настроить API ключ
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showAiChat = false },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { showAiChat = false }) {
                            androidx.compose.material3.Text("Ок")
                        }
                    },
                    title = { androidx.compose.material3.Text("API ключ не настроен") },
                    text = {
                        androidx.compose.material3.Text(
                            "Для использования ИИ помощника нужно настроить OpenRouter API ключ в админке: " +
                                "http://45.134.12.54/admin/settings"
                        )
                    }
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Главная",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            GradientCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Твой уровень: ${levelInfo.level}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "До следующего: ${levelInfo.xpForNextLevel - levelInfo.xpIntoLevel} XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = levelInfo.level.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { (levelInfo.xpIntoLevel.toFloat() / levelInfo.xpForNextLevel).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = androidx.compose.ui.graphics.Color.White,
                    trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isLoadingProfile.value) {
                    SkeletonStatCard(modifier = Modifier.weight(1f))
                    SkeletonStatCard(modifier = Modifier.weight(1f))
                } else {
                    StatCard(
                        title = "Шаги",
                        value = stepsToday.toString(),
                        subtitle = "Сегодня",
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    )
                    StatCard(
                        title = "Ккал",
                        value = caloriesToday.toString(),
                        subtitle = "Еда",
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                    )
                }
            }

            ModernCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Сожжено килокалорий",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalCaloriesBurned ккал",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ходьба: $caloriesBurnedFromWalking ккал • Тренировки: $caloriesBurnedFromTrainings ккал",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            if (shouldPromptWeight) {
                ModernCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Обновите вес",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Рекомендуем отмечать вес раз в неделю — так прогресс будет точнее.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            ModernOutlinedButton(
                                text = "Ввести вес",
                                onClick = { showAddWeight = true },
                            )
                        }
                    }
                }
            }

            ModernCard {
                Text(
                    text = "Прогресс к цели",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                val mode = settings.value?.calorieMode ?: "maintain"
                val planLine = when {
                    mode == "lose" && plannedDeficit != null && plannedDeficit > 0 -> {
                        val kgWeek = plannedLossKgPerWeek
                        if (kgWeek != null) {
                            "План: дефицит ${plannedDeficit.toInt()} ккал/день (≈ ${String.format(Locale.US, "%.2f", kgWeek)} кг/нед)"
                        } else {
                            "План: дефицит ${plannedDeficit.toInt()} ккал/день"
                        }
                    }
                    mode == "gain" && plannedDeficit != null && plannedDeficit < 0 -> "План: профицит ${-plannedDeficit.toInt()} ккал/день"
                    mode == "maintain" -> "План: поддержание"
                    else -> "План: заполните профиль для расчёта"
                }

                Text(
                    text = planLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val targetWeight = settings.value?.targetWeightKg
                val currentWeight = latestWeightEntry?.weightKg ?: profileState.value?.weightKg
                if (targetWeight != null && currentWeight != null && currentWeight > targetWeight) {
                    val leftKg = currentWeight - targetWeight
                    val leftKcal = leftKg * 7700.0
                    val weeks = plannedLossKgPerWeek?.takeIf { it > 0 }?.let { leftKg / it }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "До цели: ${String.format(Locale.US, "%.1f", leftKg)} кг (≈ ${leftKcal.toInt()} ккал)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (weeks != null && weeks.isFinite() && weeks > 0) {
                        Text(
                            text = "Оценка: ≈ ${String.format(Locale.US, "%.1f", weeks)} нед",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val dayLine = when {
                    deficitToday == null -> "Сегодня: добавьте рост/вес/возраст в профиль для расчёта"
                    deficitToday > 0 -> "Сегодня: дефицит ${deficitToday.toInt()} ккал"
                    deficitToday < 0 -> "Сегодня: профицит ${(-deficitToday).toInt()} ккал"
                    else -> "Сегодня: баланс"
                }

                Text(
                    text = dayLine,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Еда: $caloriesToday ккал • Активность: $totalCaloriesBurned ккал • Итог: $netCaloriesToday ккал",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ModernCard {
                Text(
                    text = "Цели сегодня",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Шаги: $stepsToday / $stepGoal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { if (stepGoal > 0) (stepsToday.toFloat() / stepGoal).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (calorieGoal > 0) "Калории: $caloriesToday / $calorieGoal" else "Калории: заполните профиль, чтобы рассчитать норму",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { if (calorieGoal > 0) (caloriesToday.toFloat() / calorieGoal).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondary,
                )

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showGoals = true }) { Text("Настроить цели") }
            }

            ModernCard {
                Text(
                    text = "Статистика недели",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(16.dp))

                WeeklyLineChart(
                    values1 = stepsWeek,
                    values2 = caloriesWeek,
                    labels = dayLabels,
                    color1 = MaterialTheme.colorScheme.primary,
                    color2 = MaterialTheme.colorScheme.secondary
                )
            }

            ModernCard {
                Box {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Вода",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Выпито: $waterToday / $waterGoal мл",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.LocalDrink,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { (waterToday.toFloat() / waterGoal).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(250, 500).forEach { ml ->
                                ModernOutlinedButton(
                                    text = "+$ml мл",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (userId == null || isSavingWater) return@ModernOutlinedButton
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            isSavingWater = true
                                            // Мгновенно обновляем UI
                                            remoteWaterToday = (remoteWaterToday ?: 0) + ml
                                            // Server-first: send to backend
                                            val result = waterRepository.createWater(today, ml)
                                            if (result.isSuccess) {
                                                // Process any pending sync items
                                                syncRepository.processPending(userId)
                                            } else {
                                                // Enqueue for retry
                                                syncRepository.enqueue(
                                                    userId = userId,
                                                    entityType = "water",
                                                    action = "create",
                                                    payload = mapOf(
                                                        "date_epoch_day" to today,
                                                        "amount_ml" to ml,
                                                    ),
                                                )
                                                // Offline fallback: keep local insert so user progress is not lost
                                                withContext(Dispatchers.IO) {
                                                    db.waterDao().insert(
                                                        com.volovod.alta.data.entities.WaterEntryEntity(
                                                            userId = userId,
                                                            dateEpochDay = today,
                                                            amountMl = ml,
                                                            createdAtEpochMs = System.currentTimeMillis()
                                                        )
                                                    )
                                                }
                                            }

                                            // Server-first XP event
                                            xpRemoteRepository.createXpEvent(
                                                dateEpochDay = today,
                                                type = "water",
                                                points = 2,
                                                note = "Выпил $ml мл воды",
                                            )
                                            // Cache locally (always) so UI updates immediately
                                            withContext(Dispatchers.IO) {
                                                db.xpDao().insert(
                                                    com.volovod.alta.data.entities.XpEventEntity(
                                                        userId = userId,
                                                        dateEpochDay = today,
                                                        type = "water",
                                                        points = 2,
                                                        note = "Выпил $ml мл воды",
                                                        createdAtEpochMs = System.currentTimeMillis(),
                                                    ),
                                                )
                                            }
                                            isSavingWater = false
                                        }
                                    },
                                    enabled = !isSavingWater
                                )
                            }
                        }
                    }
                    if (isSavingWater) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                        }
                    }
                }
            }

            ModernCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Квесты сегодня",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                val quests: List<Triple<String, Boolean, androidx.compose.ui.graphics.vector.ImageVector>> = listOf(
                    Triple("Шаги", stepsDone, Icons.AutoMirrored.Filled.DirectionsWalk),
                    Triple("Калории", caloriesDone, Icons.Default.LocalFireDepartment),
                    Triple("Вода", waterDone, Icons.Default.LocalDrink),
                    Triple("Тренировка", hasTrainingToday, Icons.Default.FitnessCenter),
                    Triple("Книга", hasBookToday, Icons.Default.AutoStories),
                    Triple("Я не курю", noSmokeToday, Icons.Default.SmokingRooms),
                )
                
                quests.forEach { (title, done, icon) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (done) MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            ModernCard {
                Text(
                    text = "Быстрые действия",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { showAddFood = true }) { Text("Еда") }
                    TextButton(onClick = { showAddWeight = true }) { Text("Вес") }
                    TextButton(onClick = { showAddBook = true }) { Text("Книга") }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAiChat = true },
            modifier = Modifier
                .padding(20.dp)
                .align(Alignment.BottomEnd),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = androidx.compose.ui.graphics.Color.White,
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }

        if (confettiAnim.value > 0f && confettiAnim.value < 1f) {
            val color1 = MaterialTheme.colorScheme.primary
            val color2 = MaterialTheme.colorScheme.secondary
            val color3 = MaterialTheme.colorScheme.tertiary
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val progress = confettiAnim.value
                val center = Offset(size.width / 2, size.height / 2)
                
                // Простая реализация разлетающихся частиц
                repeat(30) { i ->
                    rotate(i * 12f + progress * 100f, pivot = center) {
                        drawRect(
                            color = when(i % 3) {
                                0 -> color1
                                1 -> color2
                                else -> color3
                            },
                            topLeft = Offset(center.x, center.y - (progress * 500f) - (i * 5)),
                            size = androidx.compose.ui.geometry.Size(15f, 15f),
                            alpha = 1f - progress
                        )
                    }
                }
            }
        }
        
        // Snackbar для уведомлений
        ModernSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
