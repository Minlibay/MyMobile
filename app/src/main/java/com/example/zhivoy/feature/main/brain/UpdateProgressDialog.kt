package com.example.zhivoy.feature.main.brain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zhivoy.ui.components.ModernButton
import com.example.zhivoy.ui.components.ModernOutlinedButton

@Composable
fun UpdateProgressDialog(
    currentPages: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onUpdate: (pagesRead: Int) -> Unit,
) {
    var pagesReadText by rememberSaveable { mutableStateOf(currentPages.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(text = "Обновить прогресс", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Прочитано страниц (всего: $totalPages)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pagesReadText,
                    onValueChange = { pagesReadText = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Страниц") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                )
                if (error != null) {
                    Text(text = error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(4.dp))
                ModernButton(
                    text = "Сохранить",
                    onClick = {
                        val p = pagesReadText.toIntOrNull() ?: 0
                        if (p > totalPages) {
                            error = "Прочитано больше, чем всего страниц"
                            return@ModernButton
                        }
                        onUpdate(p)
                    },
                )
                ModernOutlinedButton(text = "Отмена", onClick = onDismiss)
            }
        },
    )
}













