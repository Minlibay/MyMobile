package com.volovod.alta.feature.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.volovod.alta.ui.components.ModernButton
import com.volovod.alta.ui.components.ModernOutlinedButton

@Composable
fun GoalsDialog(
    initialStepGoal: Int,
    initialMode: String,
    initialCalorieOverride: Int?,
    initialTargetWeightKg: Double?,
    initialRemindersEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (stepGoal: Int, mode: String, calorieOverride: Int?, targetWeightKg: Double?, remindersEnabled: Boolean) -> Unit,
) {
    var stepGoalText by rememberSaveable { mutableStateOf(initialStepGoal.toString()) }
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    var calorieOverrideText by rememberSaveable { mutableStateOf(initialCalorieOverride?.toString() ?: "") }
    var targetWeightText by rememberSaveable { mutableStateOf(initialTargetWeightKg?.toString() ?: "") }
    var remindersEnabled by rememberSaveable { mutableStateOf(initialRemindersEnabled) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { 
            Text(
                "Настройка целей", 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = stepGoalText,
                    onValueChange = { stepGoalText = it.filter(Char::isDigit) },
                    label = { Text("Цель шагов в день") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Режим калорий", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            "lose" to "Похудение", 
                            "maintain" to "Баланс", 
                            "gain" to "Набор"
                        )
                        modes.forEach { (m, label) ->
                            FilterChip(
                                selected = mode == m,
                                onClick = { mode = m },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = calorieOverrideText,
                    onValueChange = { calorieOverrideText = it.filter(Char::isDigit) },
                    label = { Text("Своя цель калорий (опция)") },
                    placeholder = { Text("По умолчанию — по формуле") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                OutlinedTextField(
                    value = targetWeightText,
                    onValueChange = { targetWeightText = it.replace(',', '.').filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Целевой вес (кг)") },
                    placeholder = { Text("Например: 72.5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Напоминания",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Разминка и чтение",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { remindersEnabled = it }
                    )
                }

                if (error != null) {
                    Text(
                        error!!, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                ModernButton(
                    text = "Сохранить изменения",
                    onClick = {
                        val sg = stepGoalText.toIntOrNull()
                        if (sg == null || sg !in 1000..50000) {
                            error = "Шаги должны быть от 1000 до 50000"
                            return@ModernButton
                        }
                        val override = calorieOverrideText.toIntOrNull()
                        if (override != null && override !in 800..6000) {
                            error = "Калории должны быть от 800 до 6000"
                            return@ModernButton
                        }
                        val tw = targetWeightText.toDoubleOrNull()
                        if (tw != null && tw !in 20.0..400.0) {
                            error = "Целевой вес должен быть 20–400 кг"
                            return@ModernButton
                        }
                        onSave(sg, mode, override, tw, remindersEnabled)
                    },
                )
                ModernOutlinedButton(
                    text = "Отмена", 
                    onClick = onDismiss
                )
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}


