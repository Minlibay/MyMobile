package com.example.zhivoy.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.zhivoy.LocalAppDatabase
import com.example.zhivoy.LocalSessionStore
import com.example.zhivoy.data.repository.AuthRepository
import com.example.zhivoy.network.ApiClient
import com.example.zhivoy.ui.components.ModernButton
import com.example.zhivoy.ui.components.ModernOutlinedButton
import com.example.zhivoy.ui.components.ModernTextField
import com.example.zhivoy.ui.theme.FitnessGradientEnd
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onGoLogin: () -> Unit,
    onRegistered: () -> Unit,
) {
    val context = LocalContext.current
    val sessionStore = LocalSessionStore.current
    val db = LocalAppDatabase.current
    val authRepository = remember { AuthRepository(
        context,
        sessionStore,
        ApiClient.createProfileApi(sessionStore),
        ApiClient.createUserSettingsApi(sessionStore),
        db.profileDao(),
        db.userSettingsDao(),
    ) }
    val scope = rememberCoroutineScope()

    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun submit() {
        if (loading) return
        error = null

        val l = login.trim()
        if (l.length < 3) {
            error = "Логин минимум 3 символа"
            return
        }
        if (password.length < 6) {
            error = "Пароль минимум 6 символов"
            return
        }

        loading = true
        scope.launch {
            val result = authRepository.register(l, password)
            loading = false
            
            result.fold(
                onSuccess = { user ->
                    // После регистрации автоматически логинимся
                    val loginResult = authRepository.login(l, password)
                    loginResult.fold(
                        onSuccess = {
                            onRegistered()
                        },
                        onFailure = { e ->
                            error = "Ошибка входа после регистрации: ${e.message ?: "Неизвестная ошибка"}"
                        }
                    )
                },
                onFailure = { e ->
                    error = when {
                        e.message?.contains("409") == true -> "Логин уже занят"
                        e.message?.contains("401") == true -> "Ошибка авторизации"
                        else -> "Ошибка регистрации: ${e.message ?: "Неизвестная ошибка"}"
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
                        FitnessGradientEnd.copy(alpha = 0.12f),
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
                text = "Регистрация",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Создайте аккаунт и начните отслеживание",
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
                value = login,
                onValueChange = { login = it },
                label = "Логин",
                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
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
                placeholder = "Минимум 6 символов"
            )

            if (error != null) {
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            ModernButton(
                text = "Создать аккаунт",
                onClick = { submit() },
                enabled = !loading,
                isLoading = loading
            )

            ModernOutlinedButton(
                text = "У меня уже есть аккаунт",
                onClick = onGoLogin,
                enabled = !loading,
            )
        }
    }
}


