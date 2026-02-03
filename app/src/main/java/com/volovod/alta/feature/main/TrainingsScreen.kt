package com.volovod.alta.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.volovod.alta.LocalAppDatabase
import com.volovod.alta.LocalSessionStore
import com.volovod.alta.data.repository.AdminSettingsRepository
import com.volovod.alta.data.entities.TrainingEntity
import com.volovod.alta.data.entities.TrainingPlanEntity
import com.volovod.alta.data.entities.TrainingTemplateEntity
import com.volovod.alta.data.entities.TrainingWeekGoalEntity
import com.volovod.alta.data.entities.XpEventEntity
import com.volovod.alta.data.repository.TrainingRemoteRepository
import com.volovod.alta.data.repository.XpRemoteRepository
import com.volovod.alta.feature.main.training.AddTemplateDialog
import com.volovod.alta.feature.main.training.AddTrainingDialog
import com.volovod.alta.feature.main.training.PlankDialog
import com.volovod.alta.ui.components.ModernButton
import com.volovod.alta.ui.components.ModernOutlinedButton
import com.volovod.alta.ui.components.ModernCard
import com.volovod.alta.ui.components.SkeletonCard
import com.volovod.alta.ui.components.SkeletonStatCard
import com.volovod.alta.network.ApiClient
import com.volovod.alta.network.api.AiChatMessage
import com.volovod.alta.network.api.AiChatRequest
import com.volovod.alta.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TrainingsScreen() {
    val db = LocalAppDatabase.current
    val sessionStore = LocalSessionStore.current
    val session by sessionStore.session.collectAsState(initial = null)
    val userId = session?.userId
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val trainingRemoteRepository = remember(sessionStore) { TrainingRemoteRepository(sessionStore) }
    val adminSettingsRepository = remember { AdminSettingsRepository() }

    val xpRemoteRepository = remember(sessionStore) { XpRemoteRepository(sessionStore) }

    val today = DateTime.epochDayNow()
    var selectedDay by remember { mutableIntStateOf(today) }

    // Week Calendar
    val weekDays = remember {
        val startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY)
        (0..6).map { startOfWeek.plusDays(it.toLong()).toEpochDay().toInt() }
    }

    // Dialogs
    var showAddTemplate by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }
    var showPlank by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var aiResult by remember { mutableStateOf<String?>(null) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var aiMode by remember { mutableStateOf("today") } // today | week
    var showSaveAiTemplateDialog by remember { mutableStateOf(false) }

    if (showSaveAiTemplateDialog && aiResult != null) {
        SaveAiTemplateDialog(
            result = aiResult ?: "",
            mode = aiMode,
            onDismiss = { showSaveAiTemplateDialog = false },
            onSave = { title, duration, calories, tags ->
                if (userId == null) return@SaveAiTemplateDialog
                scope.launch {
                    withContext(Dispatchers.IO) {
                        db.trainingTemplateDao().insert(
                            TrainingTemplateEntity(
                                userId = userId,
                                title = title,
                                category = "AI тренер",
                                tagsCsv = tags,
                                defaultDurationMinutes = duration,
                                defaultCaloriesBurned = calories,
                                createdAtEpochMs = System.currentTimeMillis()
                            )
                        )
                    }
                }
                showSaveAiTemplateDialog = false
            }
        )
    }

    if (showAiDialog) {
        AiTrainingDialog(
            mode = aiMode,
            onModeChange = { aiMode = it },
            loading = aiLoading,
            error = aiError,
            result = aiResult,
            onDismiss = { showAiDialog = false },
            onGenerate = {
                if (userId == null) return@AiTrainingDialog
                scope.launch {
                    aiLoading = true
                    aiError = null
                    try {
                        val profile = withContext(Dispatchers.IO) { db.profileDao().getByUserId(userId) }
                        val weight = profile?.weightKg ?: 70.0
                        val height = profile?.heightCm ?: 170
                        val sex = profile?.sex ?: "unknown"
                        val age = profile?.age ?: 30
                        val fitnessGoal = "поддерживать форму" // нет явного поля цели, фиксируем формулировку

                        val settingsRes = adminSettingsRepository.getSettings()
                        val settings = settingsRes.getOrNull()
                        val authKey = settings?.gigachat_auth_key
                        if (authKey.isNullOrBlank()) {
                            aiError = "GigaChat не настроен"
                            aiLoading = false
                            return@launch
                        }

                        val promptMode = if (aiMode == "week") "Составь план тренировок на неделю" else "Составь тренировку на сегодня"
                        val system = "Ты — персональный тренер. Давай конкретный список упражнений с повторениями/временем. Формат: буллеты без лишнего текста. Учитывай опыт новичка и безопасность."
                        val userPrompt = "${promptMode}. Параметры: вес ${weight} кг, рост ${height} см, возраст ${age}, пол ${sex}, цель: ${fitnessGoal}. Верни 5-7 упражнений, укажи время или повторы, короткие подсказки по технике безопасности."

                        val aiChatApi = ApiClient.createAiChatApi(sessionStore)
                        val request = AiChatRequest(
                            messages = listOf(
                                AiChatMessage(role = "system", content = system),
                                AiChatMessage(role = "user", content = userPrompt)
                            ),
                            max_tokens = 800,
                            temperature = 0.5f
                        )
                        val response = withContext(Dispatchers.IO) {
                            aiChatApi.chat(request)
                        }
                        val content = response.content
                        if (content.isNullOrBlank()) {
                            aiError = "Пустой ответ ИИ"
                        } else {
                            aiResult = content
                            showSaveAiTemplateDialog = true
                        }
                    } catch (e: Exception) {
                        aiError = e.message ?: "Ошибка ИИ"
                    } finally {
                        aiLoading = false
                    }
                }
            }
        )
    }

    // Data for selected day
    val completedTrainings by (if (userId != null) db.trainingDao().observeFrom(userId, selectedDay) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val completedForSelected = completedTrainings.filter { it.dateEpochDay == selectedDay }

    val plansForDay by (if (userId != null) db.trainingPlanDao().observeForDay(userId, selectedDay) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    // Templates
    val templates by (if (userId != null) db.trainingTemplateDao().observeAll(userId) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    // Week Goal
    val monday = weekDays.first()
    val weekGoal by (if (userId != null) db.trainingWeekGoalDao().observeForWeek(userId, monday) else kotlinx.coroutines.flow.flowOf(null))
        .collectAsState(initial = null)

    // Stats for week
    val weekTrainings by (if (userId != null) db.trainingDao().observeFrom(userId, monday) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val completedThisWeek = weekTrainings.filter { it.dateEpochDay in weekDays }.size

    var isLoading by remember(userId) { mutableStateOf(true) }
    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        isLoading = true
        // Здесь можно добавить загрузку данных с бэкенда, если нужно
        isLoading = false
    }

    if (showPlank) {
        PlankDialog(
            onDismiss = { showPlank = false },
            onSuccess = { seconds ->
                if (userId == null) return@PlankDialog
                scope.launch {
                    val title = "Планка: ${seconds}с"
                    val caloriesBurned = (seconds * 0.1).toInt()
                    val durationMinutes = (seconds / 60).coerceAtLeast(1)

                    val remote = trainingRemoteRepository.createTraining(
                        dateEpochDay = selectedDay,
                        title = title,
                        caloriesBurned = caloriesBurned,
                        durationMinutes = durationMinutes,
                    )

                    withContext(Dispatchers.IO) {
                        // Cache locally (always) so UI updates immediately
                        db.trainingDao().insert(
                            TrainingEntity(
                                userId = userId,
                                dateEpochDay = selectedDay,
                                title = title,
                                caloriesBurned = caloriesBurned,
                                durationMinutes = durationMinutes,
                                createdAtEpochMs = System.currentTimeMillis()
                            )
                        )

                        val xp = (seconds / 10).coerceAtLeast(5)
                        // Server-first XP event
                        xpRemoteRepository.createXpEvent(
                            dateEpochDay = selectedDay,
                            type = "plank",
                            points = xp,
                            note = title,
                        )
                        // Cache locally (always) so UI updates immediately
                        db.xpDao().insert(
                            XpEventEntity(
                                userId = userId,
                                dateEpochDay = selectedDay,
                                type = "plank",
                                points = xp,
                                note = title,
                                createdAtEpochMs = System.currentTimeMillis()
                            )
                        )
                    }
                }
                showPlank = false
            }
        )
    }

    if (showAddTemplate) {
        AddTemplateDialog(
            onDismiss = { showAddTemplate = false },
            onAdd = { title, category, tags, duration, calories ->
                if (userId == null) return@AddTemplateDialog
                scope.launch {
                    withContext(Dispatchers.IO) {
                        db.trainingTemplateDao().insert(
                            TrainingTemplateEntity(
                                userId = userId,
                                title = title,
                                category = category,
                                tagsCsv = tags,
                                defaultDurationMinutes = duration,
                                defaultCaloriesBurned = calories,
                                createdAtEpochMs = System.currentTimeMillis()
                            )
                        )
                    }
                }
                showAddTemplate = false
            }
        )
    }

    if (showQuickAdd) {
        AddTrainingDialog(
            onDismiss = { showQuickAdd = false },
            onAdd = { title, duration, calories ->
                if (userId == null) return@AddTrainingDialog
                scope.launch {
                    val remote = trainingRemoteRepository.createTraining(
                        dateEpochDay = selectedDay,
                        title = title,
                        caloriesBurned = calories,
                        durationMinutes = duration,
                    )

                    withContext(Dispatchers.IO) {
                        // Cache locally (always) so UI updates immediately
                        db.trainingDao().insert(
                            TrainingEntity(
                                userId = userId,
                                dateEpochDay = selectedDay,
                                title = title,
                                caloriesBurned = calories,
                                durationMinutes = duration,
                                createdAtEpochMs = System.currentTimeMillis()
                            )
                        )
                        // Server-first XP event
                        xpRemoteRepository.createXpEvent(
                            dateEpochDay = selectedDay,
                            type = "training",
                            points = 30,
                            note = "Training: $title",
                        )
                        // Cache locally (always) so UI updates immediately
                        db.xpDao().insert(
                            XpEventEntity(
                                userId = userId,
                                dateEpochDay = selectedDay,
                                type = "training",
                                points = 30,
                                note = "Training: $title",
                                createdAtEpochMs = System.currentTimeMillis()
                            )
                        )
                    }
                }
                showQuickAdd = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Тренировки",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // AI Trainer Highlight
        ModernCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI тренер", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Персональная тренировка с учетом веса, возраста и цели. Выберите режим и получите список упражнений.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernButton(
                        text = "Сегодня",
                        onClick = {
                            aiMode = "today"
                            showAiDialog = true
                        }
                    )
                    ModernOutlinedButton(
                        text = "Неделя",
                        onClick = {
                            aiMode = "week"
                            showAiDialog = true
                        }
                    )
                }
            }
        }

        // Weekly Goal Progress
        if (isLoading) {
            SkeletonCard(modifier = Modifier.fillMaxWidth())
        } else {
            ModernCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Цель на неделю",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val target = weekGoal?.targetTrainingsCount ?: 3
                        Text(
                            text = "Выполнено: $completedThisWeek из $target",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = (completedThisWeek.toFloat() / target).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        if (userId == null) return@IconButton
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val target = weekGoal?.targetTrainingsCount ?: 3
                                db.trainingWeekGoalDao().upsert(
                                    TrainingWeekGoalEntity(
                                        id = weekGoal?.id ?: 0,
                                        userId = userId,
                                        weekEpochDay = monday,
                                        targetTrainingsCount = if (target >= 7) 1 else target + 1,
                                        updatedAtEpochMs = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Edit Goal")
                    }
                }
            }
            IconButton(onClick = { showAiDialog = true }) {
                Icon(Icons.Default.Chat, contentDescription = "AI тренер")
            }
        }

        // Calendar Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(weekDays) { day ->
                val date = LocalDate.ofEpochDay(day.toLong())
                val isSelected = day == selectedDay
                val isToday = day == today

                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else if (isToday) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { selectedDay = day }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru")).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Selected Day Activity
        Text(
            text = if (selectedDay == today) "Сегодня" else LocalDate.ofEpochDay(selectedDay.toLong()).format(java.time.format.DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        // Planned Trainings
        if (isLoading) {
            SkeletonCard(modifier = Modifier.fillMaxWidth())
        } else if (plansForDay.isNotEmpty()) {
            plansForDay.forEach { plan ->
                val template = templates.find { it.id == plan.templateId }
                ModernCard(
                    onClick = {
                        if (userId == null) return@ModernCard
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                if (!plan.isDone) {
                                    // Mark as done and add to actual trainings
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    db.trainingPlanDao().setDone(plan.id, true)
                                    db.trainingDao().insert(
                                        TrainingEntity(
                                            userId = userId,
                                            dateEpochDay = selectedDay,
                                            title = template?.title ?: "Плановая тренировка",
                                            caloriesBurned = template?.defaultCaloriesBurned ?: 0,
                                            durationMinutes = template?.defaultDurationMinutes ?: 0,
                                            templateId = template?.id,
                                            createdAtEpochMs = System.currentTimeMillis()
                                        )
                                    )
                                    // Server-first XP event
                                    xpRemoteRepository.createXpEvent(
                                        dateEpochDay = selectedDay,
                                        type = "training",
                                        points = 40, // Bonus for planned training
                                        note = "Planned training done",
                                    )
                                    // Cache locally (always) so UI updates immediately
                                    db.xpDao().insert(
                                        XpEventEntity(
                                            userId = userId,
                                            dateEpochDay = selectedDay,
                                            type = "training",
                                            points = 40, // Bonus for planned training
                                            note = "Planned training done",
                                            createdAtEpochMs = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (plan.isDone) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (plan.isDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = template?.title ?: "Плановая тренировка",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (plan.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            if (template != null) {
                                Text(
                                    text = "${template.category} • ${template.defaultDurationMinutes} мин",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Completed Trainings
        if (isLoading) {
            SkeletonCard(modifier = Modifier.fillMaxWidth())
        } else if (completedForSelected.isNotEmpty()) {
            completedForSelected.forEach { training ->
                ModernCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = training.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${training.durationMinutes} мин • ${training.caloriesBurned} ккал",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (!isLoading && plansForDay.isEmpty() && completedForSelected.isEmpty()) {
            Text(
                text = "На этот день ничего не запланировано",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        ModernButton(
            text = "Добавить тренировку",
            onClick = { showQuickAdd = true }
        )

        ModernOutlinedButton(
            text = "Режим Планки 🔥",
            onClick = { showPlank = true }
        )

        ModernOutlinedButton(
            text = "Тренировка с AI",
            onClick = { showAiDialog = true }
        )

        // Templates Section
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Мои шаблоны",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = { showAddTemplate = true }) {
                Text("Создать шаблон")
            }
        }

        if (templates.isEmpty()) {
            Text(
                text = "Создайте шаблоны для быстрого планирования",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            templates.forEach { template ->
                ModernCard(
                    onClick = {
                        if (userId == null) return@ModernCard
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.trainingPlanDao().insert(
                                    TrainingPlanEntity(
                                        userId = userId,
                                        dateEpochDay = selectedDay,
                                        templateId = template.id,
                                        createdAtEpochMs = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                ) {
                    Column {
                        Text(
                            text = template.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${template.category} • ${template.defaultDurationMinutes} мин",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (template.tagsCsv.isNotBlank()) {
                            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                template.tagsCsv.split(",").forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = tag.trim(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun AiTrainingDialog(
    mode: String,
    onModeChange: (String) -> Unit,
    loading: Boolean,
    error: String?,
    result: String?,
    onDismiss: () -> Unit,
    onGenerate: () -> Unit,
) {
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI тренер") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Что нужно?", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == "today",
                        onClick = { onModeChange("today") },
                        label = { Text("Сегодня") }
                    )
                    FilterChip(
                        selected = mode == "week",
                        onClick = { onModeChange("week") },
                        label = { Text("Неделя") }
                    )
                }

                if (loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (!error.isNullOrBlank()) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (!result.isNullOrBlank()) {
                    Text("Рекомендация:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 240.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .verticalScroll(scrollState)
                            .padding(12.dp)
                    ) {
                        Text(result, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onGenerate, enabled = !loading) {
                Text(if (loading) "Генерируем..." else "Сгенерировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("Закрыть") }
        }
    )
}

@Composable
private fun SaveAiTemplateDialog(
    result: String,
    mode: String,
    onDismiss: () -> Unit,
    onSave: (title: String, duration: Int, calories: Int, tags: String) -> Unit,
) {
    val defaultTitle = if (mode == "week") "AI план на неделю" else "AI тренировка"
    var title by remember { mutableStateOf(defaultTitle) }
    var durationText by remember { mutableStateOf("45") }
    var caloriesText by remember { mutableStateOf("300") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сохранить в шаблоны?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "AI предложил тренировку. Сохранить как шаблон?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter(Char::isDigit) },
                        label = { Text("Мин") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = caloriesText,
                        onValueChange = { caloriesText = it.filter(Char::isDigit) },
                        label = { Text("Ккал") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = result.take(300),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val duration = durationText.toIntOrNull() ?: 0
                val calories = caloriesText.toIntOrNull() ?: 0
                val tags = "ai,training"
                onSave(title.ifBlank { defaultTitle }, duration, calories, tags)
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Не нужно") }
        }
    )
}
