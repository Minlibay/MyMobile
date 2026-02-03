package com.volovod.alta.feature.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.volovod.alta.ui.components.ModernButton
import com.volovod.alta.ui.components.ModernOutlinedButton

@Composable
fun AddFoodDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, calories: Int) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var caloriesText by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(text = "Добавить еду", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ккал") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                ModernButton(
                    text = "Добавить",
                    onClick = {
                        val c = caloriesText.toIntOrNull()
                        if (title.trim().isEmpty() || c == null || c <= 0) {
                            error = "Введите название и калории"
                            return@ModernButton
                        }
                        onAdd(title.trim(), c)
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
                ModernOutlinedButton(
                    text = "Отмена",
                    onClick = onDismiss,
                )
            }
        },
    )
}














