package com.volovod.alta.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.volovod.alta.LocalAppDatabase
import com.volovod.alta.LocalSessionStore
import com.volovod.alta.data.repository.AuthRepository
import com.volovod.alta.data.repository.SyncRepository
import com.volovod.alta.network.ApiClient
import com.volovod.alta.ui.components.ModernButton
import com.volovod.alta.ui.components.ModernOutlinedButton
import com.volovod.alta.ui.components.ModernTextField
import com.volovod.alta.ui.theme.FitnessGradientEnd
import com.volovod.alta.ui.theme.FitnessGradientStart
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onGoRegister: () -> Unit,
    onNeedOnboarding: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    val context = LocalContext.current
    val db = LocalAppDatabase.current
    val sessionStore = LocalSessionStore.current
    val authRepository = remember(context, sessionStore, db) {
        AuthRepository(
            context,
            sessionStore,
            ApiClient.createProfileApi(sessionStore),
            ApiClient.createUserSettingsApi(sessionStore),
            db.profileDao(),
            db.userSettingsDao(),
            SyncRepository(sessionStore, db),
        )
    }
    val scope = rememberCoroutineScope()

    var loginOrEmail by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun submit() {
        if (loading) return
        error = null

        val login = loginOrEmail.trim()
        if (login.isEmpty() || password.isEmpty()) {
            error = "Заполните логин и пароль"
            return
        }

        loading = true
        scope.launch {
            val result = authRepository.login(login, password)
            loading = false
            
            result.fold(
                onSuccess = {
                    // Пытаемся загрузить профиль с бекенда
                    val userId = sessionStore.session.first()?.userId
                    if (userId != null) {
                        val profileResult = authRepository.getProfile()
                        profileResult.fold(
                            onSuccess = { _ -> onLoggedIn() },
                            onFailure = { onNeedOnboarding() }
                        )
                    } else {
                        onNeedOnboarding()
                    }
                },
                onFailure = { e ->
                    error = when {
                        e.message?.contains("401") == true -> "Неверный логин или пароль"
                        e.message?.contains("409") == true -> "Пользователь уже существует"
                        else -> "Ошибка подключения: ${e.message ?: "Неизвестная ошибка"}"
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        FitnessGradientStart.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(FitnessGradientStart, FitnessGradientEnd),
                            ),
                        )
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "Alta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Вход",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Продолжайте свой прогресс",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                value = loginOrEmail,
                onValueChange = { loginOrEmail = it },
                label = "Логин или Email",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                    )
                },
            )

            ModernTextField(
                value = password,
                onValueChange = { password = it },
                label = "Пароль",
                leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                placeholder = "Ваш секретный пароль"
            )

            if (error != null) {
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            ModernButton(
                text = "Войти",
                onClick = { submit() },
                enabled = !loading,
                isLoading = loading
            )

            ModernOutlinedButton(
                text = "Создать аккаунт",
                onClick = onGoRegister,
                enabled = !loading,
            )
        }
    }
}


