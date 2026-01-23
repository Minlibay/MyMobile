package com.example.zhivoy.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zhivoy.LocalAppDatabase
import com.example.zhivoy.LocalSessionStore
import com.example.zhivoy.data.entities.ProfileEntity
import com.example.zhivoy.data.repository.AuthRepository
import com.example.zhivoy.ui.components.ModernButton
import com.example.zhivoy.ui.components.ModernTextField
import com.example.zhivoy.ui.theme.FitnessGradientEnd
import com.example.zhivoy.ui.theme.FitnessGradientStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow

@Composable
fun AvatarSetupScreen(
    onDone: () -> Unit,
) {
    val db = LocalAppDatabase.current
    val sessionStore = LocalSessionStore.current
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context, sessionStore) }
    val scope = rememberCoroutineScope()

    var heightCm by rememberSaveable { mutableStateOf("") }
    var weightKg by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var sex by rememberSaveable { mutableStateOf("male") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val bmi = runCatching {
        val h = heightCm.toDouble() / 100.0
        val w = weightKg.toDouble()
        if (h <= 0.0) null else w / h.pow(2.0)
    }.getOrNull()

    val session by sessionStore.session.collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        FitnessGradientStart.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(
                text = "Профиль",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Пара параметров — и рекомендации станут точными",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ModernTextField(
                value = heightCm,
                onValueChange = { heightCm = it.filter { ch -> ch.isDigit() } },
                label = "Рост (см)",
            )
            ModernTextField(
                value = weightKg,
                onValueChange = { weightKg = it.replace(',', '.').filter { ch -> ch.isDigit() || ch == '.' } },
                label = "Вес (кг)",
            )
            ModernTextField(
                value = age,
                onValueChange = { age = it.filter { ch -> ch.isDigit() } },
                label = "Возраст",
            )

            Text(
                text = "Ваш пол",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.FilterChip(
                    selected = sex == "male",
                    onClick = { sex = "male" },
                    label = { Text("Мужской") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                androidx.compose.material3.FilterChip(
                    selected = sex == "female",
                    onClick = { sex = "female" },
                    label = { Text("Женский") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            if (bmi != null) {
                Text(
                    text = "ИМТ: ${"%.1f".format(bmi)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (error != null) {
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            ModernButton(
                text = if (loading) "Сохраняем..." else "Продолжить",
                onClick = {
                    val userId = session?.userId
                    if (userId == null) {
                        error = "Сессия не найдена, войдите снова"
                        return@ModernButton
                    }

                    val h = heightCm.toIntOrNull()
                    val w = weightKg.toDoubleOrNull()
                    val a = age.toIntOrNull()
                    if (h == null || h !in 80..250) {
                        error = "Рост: 80–250 см"
                        return@ModernButton
                    }
                    if (w == null || w !in 20.0..400.0) {
                        error = "Вес: 20–400 кг"
                        return@ModernButton
                    }
                    if (a == null || a !in 5..120) {
                        error = "Возраст: 5–120"
                        return@ModernButton
                    }

                    loading = true
                    scope.launch {
                        try {
                            // Сохраняем профиль на бекенд
                            val profileResult = authRepository.updateProfile(h, w, a, sex)
                            profileResult.fold(
                                onSuccess = { profileResponse ->
                                    // Сохраняем профиль локально
                                    withContext(Dispatchers.IO) {
                                        val now = System.currentTimeMillis()
                                        db.profileDao().upsert(
                                            ProfileEntity(
                                                userId = userId,
                                                heightCm = profileResponse.height_cm,
                                                weightKg = profileResponse.weight_kg,
                                                age = profileResponse.age,
                                                sex = profileResponse.sex,
                                                createdAtEpochMs = now,
                                                updatedAtEpochMs = now,
                                            ),
                                        )
                                    }
                                    loading = false
                                    onDone()
                                },
                                onFailure = { e ->
                                    error = "Ошибка сохранения: ${e.message}"
                                    loading = false
                                }
                            )
                        } catch (e: Exception) {
                            error = "Ошибка: ${e.message}"
                            loading = false
                        }
                    }
                },
                enabled = !loading,
            )
        }
    }
}


