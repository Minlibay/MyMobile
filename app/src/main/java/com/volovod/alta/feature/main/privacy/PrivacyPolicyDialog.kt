package com.volovod.alta.feature.main.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.volovod.alta.ui.components.ModernButton
import com.volovod.alta.ui.components.ModernOutlinedButton

@Composable
fun PrivacyPolicyDialog(
    text: String,
    requireAccept: Boolean,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
) {
    var accepted by remember { mutableStateOf(!requireAccept) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = "Политика конфиденциальности",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (requireAccept) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = accepted,
                            onCheckedChange = { accepted = it },
                        )
                        Text(
                            text = "Я ознакомлен(а) и принимаю условия",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (requireAccept) {
                    ModernButton(
                        text = "Принять",
                        onClick = onAccept,
                        enabled = accepted,
                    )
                    ModernOutlinedButton(
                        text = "Отмена",
                        onClick = onDismiss,
                    )
                } else {
                    ModernButton(
                        text = "Закрыть",
                        onClick = onDismiss,
                    )
                }
            }
        },
    )
}
