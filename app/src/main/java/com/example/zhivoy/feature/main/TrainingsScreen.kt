package com.example.zhivoy.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.zhivoy.LocalAppDatabase
import com.example.zhivoy.LocalSessionStore
import com.example.zhivoy.data.entities.TrainingEntity
import com.example.zhivoy.data.entities.TrainingPlanEntity
import com.example.zhivoy.data.entities.TrainingTemplateEntity
import com.example.zhivoy.data.entities.TrainingWeekGoalEntity
import com.example.zhivoy.data.entities.XpEventEntity
import com.example.zhivoy.data.repository.TrainingRemoteRepository
import com.example.zhivoy.data.repository.XpRemoteRepository
import com.example.zhivoy.feature.main.training.AddTemplateDialog
import com.example.zhivoy.feature.main.training.AddTrainingDialog
import com.example.zhivoy.feature.main.training.PlankDialog
import com.example.zhivoy.ui.components.ModernButton
import com.example.zhivoy.ui.components.ModernOutlinedButton
import com.example.zhivoy.ui.components.ModernCard
import com.example.zhivoy.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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

    val xpRemoteRepository = remember(sessionStore) { XpRemoteRepository(sessionStore) }

    val today = DateTime.epochDayNow()
    var selectedDay by remember { mutableIntStateOf(today) }

    // Week Calendar
    val weekDays = remember {
        val startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY)
        (0..6).map { startOfWeek.plusDays(it.toLong()).toEpochDay().toInt() }
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

    // Dialogs
    var showAddTemplate by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }
    var showPlank by remember { mutableStateOf(false) }

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

        // Weekly Goal Progress
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
        if (plansForDay.isNotEmpty()) {
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
        if (completedForSelected.isNotEmpty()) {
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

        if (plansForDay.isEmpty() && completedForSelected.isEmpty()) {
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
