package com.volovod.alta.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.volovod.alta.LocalSessionStore
import com.volovod.alta.data.repository.FamilyRepository
import com.volovod.alta.ui.components.ModernButton
import com.volovod.alta.ui.components.ModernCard
import com.volovod.alta.ui.components.ModernTextField
import com.volovod.alta.ui.components.SkeletonCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun FamilyScreen() {
    val sessionStore = LocalSessionStore.current
    val scope = rememberCoroutineScope()
    val repository = remember { FamilyRepository(sessionStore) }

    var family by remember { mutableStateOf<com.volovod.alta.network.dto.FamilyResponseDto?>(null) }
    var members by remember { mutableStateOf<List<com.volovod.alta.network.dto.FamilyMemberResponseDto>>(emptyList()) }
    var invites by remember { mutableStateOf<List<com.volovod.alta.network.dto.FamilyInviteResponseDto>>(emptyList()) }
    var invitesLoading by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var currentUserId by remember { mutableStateOf<Int?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    suspend fun loadFamilyData() {
        loading = true
        invitesLoading = true
        errorText = null

        val session = withContext(Dispatchers.IO) { sessionStore.session.firstOrNull() }
        currentUserId = session?.userId?.toInt()

        repository.getMyInvites()
            .onSuccess { invites = it }
            .onFailure { invites = emptyList() }
        invitesLoading = false

        repository.getMyFamily()
            .onSuccess { f ->
                family = f
                repository.getMyFamilyMembers()
                    .onSuccess { members = it }
                    .onFailure { errorText = it.message }
            }
            .onFailure { e ->
                if (!e.message.orEmpty().contains("404")) {
                    errorText = e.message
                } else {
                    family = null
                    members = emptyList()
                }
            }
        loading = false
    }

    LaunchedEffect(Unit) {
        loadFamilyData()
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                loadFamilyData()
                refreshing = false
            }
        }
    )


    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
        Text(
            text = "Семья",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Статистика семьи
        if (family != null) {
            FamilyStatsCard(family = family!!, members = members)
        }

        if (invitesLoading) {
            SkeletonCard(modifier = Modifier.fillMaxWidth())
        } else if (invites.isNotEmpty() && family == null) {
            Text(
                text = "Приглашения",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            invites.forEach { invite ->
                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = invite.family_name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Пригласил: ${invite.invited_by_login}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ModernButton(
                                text = "Принять",
                                onClick = {
                                    scope.launch {
                                        repository.acceptInvite(invite.id)
                                            .onSuccess { f ->
                                                family = f
                                                repository.getMyFamilyMembers()
                                                    .onSuccess { members = it }
                                                invites = emptyList()
                                            }
                                            .onFailure { errorText = it.message ?: "Ошибка принятия" }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        repository.declineInvite(invite.id)
                                            .onSuccess {
                                                invites = invites.filterNot { it.id == invite.id }
                                            }
                                            .onFailure { errorText = it.message ?: "Ошибка отклонения" }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Отклонить")
                            }
                        }
                    }
                }
            }
        }

        if (loading) {
            SkeletonCard(modifier = Modifier.fillMaxWidth())
            SkeletonCard(modifier = Modifier.fillMaxWidth())
        } else if (family == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FamilyRestroom,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "У вас пока нет семейной группы",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    ModernButton(
                        text = "Создать семью",
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.width(200.dp)
                    )
                }
            }
        } else {
            // Информационная карточка семьи
            FamilyInfoCard(family = family!!, isAdmin = family!!.admin_user_id == currentUserId, membersCount = members.size)

            // Совместные цели (мок-данные на клиенте)
            JointGoalsSection(members = members)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Участники (${members.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (family!!.admin_user_id == currentUserId) {
                    TextButton(onClick = { showAddMemberDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Добавить")
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(members) { member ->
                    FamilyMemberCard(
                        member = member,
                        isAdmin = member.user_id == family!!.admin_user_id,
                        isCurrentUser = member.user_id == currentUserId
                    )
                }
            }
        }

        // End of main content column
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (showCreateDialog) {
        var familyName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Создать семью") },
            text = {
                ModernTextField(
                    value = familyName,
                    onValueChange = { familyName = it },
                    label = "Название семьи"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (familyName.isNotBlank()) {
                        scope.launch {
                            val result = repository.createFamily(familyName)
                            result.onSuccess {
                                family = it
                                repository.getMyFamilyMembers()
                                    .onSuccess { members = it }
                            }
                        }
                        showCreateDialog = false
                    }
                }) { Text("Создать") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Отмена") } }
        )
    }

    if (showAddMemberDialog) {
        var memberLogin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Добавить участника") },
            text = {
                Column {
                    ModernTextField(
                        value = memberLogin,
                        onValueChange = { memberLogin = it },
                        label = "Логин пользователя"
                    )
                    if (error != null) {
                        Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (family != null && memberLogin.isNotBlank()) {
                        scope.launch {
                            val result = repository.inviteUser(memberLogin)
                            result.onSuccess {
                                error = null
                                showAddMemberDialog = false
                                // Обновим список участников
                                repository.getMyFamilyMembers()
                                    .onSuccess { members = it }
                                    .onFailure { error = it.message }
                            }.onFailure {
                                error = it.message ?: "Ошибка приглашения"
                            }
                        }
                    }
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddMemberDialog = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun FamilyStatsCard(
    family: com.volovod.alta.network.dto.FamilyResponseDto,
    members: List<com.volovod.alta.network.dto.FamilyMemberResponseDto>
) {
    val createdDate = try {
        LocalDate.parse(family.created_at, DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: Exception) {
        LocalDate.now()
    }
    val daysSinceCreation = ChronoUnit.DAYS.between(createdDate, LocalDate.now())
    
    ModernCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Статистика семьи",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Group,
                    label = "Участники",
                    value = "${members.size}",
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    icon = Icons.Default.CalendarToday,
                    label = "Дней вместе",
                    value = "$daysSinceCreation",
                    color = MaterialTheme.colorScheme.secondary
                )
                StatItem(
                    icon = Icons.Default.Star,
                    label = "Роль",
                    value = "Семья",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

private data class JointGoal(
    val title: String,
    val unit: String,
    val goal: Int,
    val progress: Int,
    val contributions: List<MemberContribution>
)

private data class MemberContribution(
    val name: String,
    val value: Int
)

@Composable
private fun JointGoalsSection(members: List<com.volovod.alta.network.dto.FamilyMemberResponseDto>) {
    val safeMembers = members.ifEmpty { listOf(com.volovod.alta.network.dto.FamilyMemberResponseDto(0, "Вы", "")) }
    val goals = remember(safeMembers) { buildMockGoals(safeMembers) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Совместные цели недели",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        goals.forEach { goal ->
            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${goal.progress} / ${goal.goal} ${goal.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${(goal.progress * 100 / goal.goal).coerceAtMost(100)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = (goal.progress.toFloat() / goal.goal.toFloat()).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        goal.contributions.forEach { c ->
                            val ratio = (c.value.toFloat() / goal.goal.toFloat()).coerceIn(0f, 1f)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(c.name, style = MaterialTheme.typography.bodySmall)
                                Text("${c.value} ${goal.unit}", style = MaterialTheme.typography.bodySmall)
                            }
                            LinearProgressIndicator(
                                progress = ratio,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildMockGoals(members: List<com.volovod.alta.network.dto.FamilyMemberResponseDto>): List<JointGoal> {
    val totalMembers = members.size.coerceAtLeast(1)

    fun distribute(goal: Int, baseStep: Int): List<MemberContribution> {
        val contributions = members.mapIndexed { index, member ->
            val value = min(goal, (index + 1) * baseStep)
            MemberContribution(name = member.login, value = value)
        }
        return contributions
    }

    val stepsGoal = 70000
    val trainingsGoal = 6
    val waterGoal = 21000 // мл за неделю

    val stepsContrib = distribute(stepsGoal, 6000)
    val trainingsContrib = distribute(trainingsGoal, 2)
    val waterContrib = distribute(waterGoal, 2500)

    return listOf(
        JointGoal(
            title = "Шаги",
            unit = "шагов",
            goal = stepsGoal,
            progress = stepsContrib.sumOf { it.value }.coerceAtMost(stepsGoal),
            contributions = stepsContrib
        ),
        JointGoal(
            title = "Тренировки",
            unit = "трен.",
            goal = trainingsGoal,
            progress = trainingsContrib.sumOf { it.value }.coerceAtMost(trainingsGoal),
            contributions = trainingsContrib
        ),
        JointGoal(
            title = "Вода",
            unit = "мл",
            goal = waterGoal,
            progress = waterContrib.sumOf { it.value }.coerceAtMost(waterGoal),
            contributions = waterContrib
        )
    )
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FamilyInfoCard(
    family: com.volovod.alta.network.dto.FamilyResponseDto,
    isAdmin: Boolean,
    membersCount: Int
) {
    ModernCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FamilyRestroom,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = family.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isAdmin) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Администратор",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Участник",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "Создана: ${family.created_at.substring(0, 10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FamilyMemberCard(
    member: com.volovod.alta.network.dto.FamilyMemberResponseDto,
    isAdmin: Boolean,
    isCurrentUser: Boolean
) {
    ModernCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAdmin) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isAdmin) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = member.login,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (isAdmin) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (isCurrentUser) {
                        Text(
                            text = "(Вы)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "В семье с: ${member.joined_at.substring(0, 10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
