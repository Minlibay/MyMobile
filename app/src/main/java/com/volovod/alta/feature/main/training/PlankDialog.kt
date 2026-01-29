package com.volovod.alta.feature.main.training

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.volovod.alta.ui.components.ModernButton
import com.volovod.alta.ui.components.ModernOutlinedButton
import kotlinx.coroutines.delay

@Composable
fun PlankDialog(
    onDismiss: () -> Unit,
    onSuccess: (durationSeconds: Int) -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Setup, 2: Timer, 3: Confirmation
    var targetSeconds by remember { mutableIntStateOf(60) }
    var secondsRemaining by remember { mutableIntStateOf(60) }
    var isRunning by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isRunning, secondsRemaining) {
        if (isRunning && secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
            if (secondsRemaining == 0) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isRunning = false
                step = 3
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = when(step) {
                    1 -> "Режим Планки"
                    2 -> "Стоим!"
                    else -> "Результат"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    1 -> {
                        Text("Выберите время тренировки", style = MaterialTheme.typography.bodyMedium)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            TextButton(onClick = { if (targetSeconds > 10) targetSeconds -= 10 }) {
                                Text("-10с", fontSize = 18.sp)
                            }
                            Text(
                                text = formatTime(targetSeconds),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = { targetSeconds += 10 }) {
                                Text("+10с", fontSize = 18.sp)
                            }
                        }
                        ModernButton(text = "Начать", onClick = {
                            secondsRemaining = targetSeconds
                            isRunning = true
                            step = 2
                        })
                    }
                    2 -> {
                        Text(
                            text = formatTime(secondsRemaining),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LinearProgressIndicator(
                            progress = (secondsRemaining.toFloat() / targetSeconds),
                            modifier = Modifier.fillMaxWidth().height(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        ModernOutlinedButton(text = "Закончить раньше", onClick = {
                            isRunning = false
                            step = 3
                        })
                    }
                    3 -> {
                        Text(
                            "Вы простояли ${formatTime(targetSeconds - secondsRemaining)}!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            "Вы справились с поставленной целью?",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ModernOutlinedButton(
                                text = "Нет",
                                modifier = Modifier.weight(1f),
                                onClick = onDismiss
                            )
                            ModernButton(
                                text = "Да!",
                                modifier = Modifier.weight(1f),
                                onClick = { onSuccess(targetSeconds - secondsRemaining) }
                            )
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

private fun formatTime(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return "%02d:%02d".format(mins, secs)
}

