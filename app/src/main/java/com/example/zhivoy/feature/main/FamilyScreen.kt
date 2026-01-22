package com.example.zhivoy.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhivoy.LocalAppDatabase
import com.example.zhivoy.LocalSessionStore
import com.example.zhivoy.data.entities.FamilyEntity
import com.example.zhivoy.data.entities.FamilyMemberEntity
import com.example.zhivoy.ui.components.ModernButton
import com.example.zhivoy.ui.components.ModernCard
import com.example.zhivoy.ui.components.ModernTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FamilyScreen() {
    val db = LocalAppDatabase.current
    val sessionStore = LocalSessionStore.current
    val session by sessionStore.session.collectAsState(initial = null)
    val userId = session?.userId
    val scope = rememberCoroutineScope()

    val family by (if (userId != null) db.familyDao().observeUserFamily(userId) else kotlinx.coroutines.flow.flowOf(null))
        .collectAsState(initial = null)
    
    val members by (if (family != null) db.familyDao().observeFamilyMembers(family!!.id) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

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

        if (family == null) {
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
                            text = "Администратор: ${if (family!!.adminUserId == userId) "Вы" else "Другой пользователь"}",
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
                if (family!!.adminUserId == userId) {
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
                items(members.sortedByDescending { it.totalXp }) { member ->
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
                                    text = "${member.totalXp} XP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (family!!.adminUserId == userId && member.userId != userId) {
                                IconButton(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            db.familyDao().removeMember(family!!.id, member.userId)
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
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
                    if (userId != null && familyName.isNotBlank()) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val fid = db.familyDao().insertFamily(
                                    FamilyEntity(
                                        name = familyName,
                                        adminUserId = userId,
                                        createdAtEpochMs = System.currentTimeMillis()
                                    )
                                )
                                db.familyDao().insertMember(
                                    FamilyMemberEntity(
                                        familyId = fid,
                                        userId = userId,
                                        joinedAtEpochMs = System.currentTimeMillis()
                                    )
                                )
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
                            val targetId = withContext(Dispatchers.IO) {
                                db.familyDao().getUserIdByLogin(memberLogin)
                            }
                            if (targetId == null) {
                                error = "Пользователь не найден"
                            } else {
                                withContext(Dispatchers.IO) {
                                    db.familyDao().insertMember(
                                        FamilyMemberEntity(
                                            familyId = family!!.id,
                                            userId = targetId,
                                            joinedAtEpochMs = System.currentTimeMillis()
                                        )
                                    )
                                }
                                showAddMemberDialog = false
                            }
                        }
                    }
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddMemberDialog = false }) { Text("Отмена") } }
        )
    }
}
