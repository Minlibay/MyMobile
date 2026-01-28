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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.volovod.alta.LocalSessionStore
import com.volovod.alta.data.repository.FamilyRepository
import com.volovod.alta.ui.components.ModernButton
import com.volovod.alta.ui.components.ModernCard
import com.volovod.alta.ui.components.ModernTextField
import kotlinx.coroutines.launch

@Composable
fun FamilyScreen() {
    val sessionStore = LocalSessionStore.current
    val scope = rememberCoroutineScope()
    val repository = remember { FamilyRepository(sessionStore) }

    var family by remember { mutableStateOf<com.volovod.alta.network.dto.FamilyResponseDto?>(null) }
    var members by remember { mutableStateOf<List<com.volovod.alta.network.dto.FamilyMemberResponseDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        errorText = null
        repository.getMyFamily()
            .onSuccess { f ->
                family = f
                repository.getMyFamilyMembers()
                    .onSuccess { members = it }
                    .onFailure { errorText = it.message }
            }
            .onFailure { e ->
                // 404 -> нет семьи, это не ошибка
                if (!e.message.orEmpty().contains("404")) {
                    errorText = e.message
                }
            }
        loading = false
    }


    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

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

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
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
            ModernCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = family!!.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Администратор: ${if (/* current user */ false) "Вы" else "Другой пользователь"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Участники",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (true) { // админский функционал пока не различаем по пользователю
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
                    ModernCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.login,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = member.joined_at,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            // пока не поддерживаем удаление участника через бэкенд
                        }
                    }
                }
            }
        }
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
