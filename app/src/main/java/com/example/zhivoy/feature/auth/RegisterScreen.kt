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
import androidx.compose.material.icons.filled.Email
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
import com.example.zhivoy.LocalAppDatabase
import com.example.zhivoy.LocalSessionStore
import com.example.zhivoy.data.entities.UserEntity
import com.example.zhivoy.ui.components.ModernButton
import com.example.zhivoy.ui.components.ModernOutlinedButton
import com.example.zhivoy.ui.components.ModernTextField
import com.example.zhivoy.ui.theme.FitnessGradientEnd
import com.example.zhivoy.ui.theme.FitnessGradientStart
import com.example.zhivoy.util.Crypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RegisterScreen(
    onGoLogin: () -> Unit,
    onRegistered: () -> Unit,
) {
    val db = LocalAppDatabase.current
    val sessionStore = LocalSessionStore.current
    val scope = rememberCoroutineScope()

    var login by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun submit() {
        if (loading) return
        error = null

        val l = login.trim()
        val e = email.trim()
        if (l.length < 3) {
            error = "Логин минимум 3 символа"
            return
        }
        if (!e.contains("@") || !e.contains(".")) {
            error = "Введите корректный email"
            return
        }
        if (password.length < 6) {
            error = "Пароль минимум 6 символов"
            return
        }

        loading = true
        scope.launch {
            val exists = withContext(Dispatchers.IO) {
                db.userDao().getByLogin(l) != null || db.userDao().getByEmail(e) != null
            }
            if (exists) {
                loading = false
                error = "Логин или email уже заняты"
                return@launch
            }

            val newId = withContext(Dispatchers.IO) {
                db.userDao().insert(
                    UserEntity(
                        login = l,
                        email = e,
                        passwordHash = Crypto.sha256(password),
                        createdAtEpochMs = System.currentTimeMillis(),
                    ),
                )
            }

            sessionStore.setUser(newId)
            loading = false
            onRegistered()
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
                value = email,
                onValueChange = { email = it },
                label = "Email",
                leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
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
                text = if (loading) "Создаем..." else "Создать аккаунт",
                onClick = { submit() },
                enabled = !loading,
            )

            ModernOutlinedButton(
                text = "У меня уже есть аккаунт",
                onClick = onGoLogin,
                enabled = !loading,
            )
        }
    }
}


