package com.volovod.alta.feature.main.announcement

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.volovod.alta.network.dto.AnnouncementResponseDto
import com.volovod.alta.ui.components.ModernButton
import com.volovod.alta.ui.components.ModernOutlinedButton

@Composable
fun AnnouncementDialog(
    announcement: AnnouncementResponseDto,
    onRead: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = "Сообщение",
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
                    text = announcement.text,
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (announcement.button_enabled && !announcement.button_text.isNullOrBlank() && !announcement.button_url.isNullOrBlank()) {
                    ModernButton(
                        text = announcement.button_text!!,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(announcement.button_url))
                            context.startActivity(intent)
                        },
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                ModernButton(
                    text = "Прочитано",
                    onClick = onRead,
                )

                ModernOutlinedButton(
                    text = "Закрыть",
                    onClick = onDismiss,
                )
            }
        },
    )
}
