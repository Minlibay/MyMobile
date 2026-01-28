package com.example.zhivoy.feature.splash

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zhivoy.LocalAppDatabase
import com.example.zhivoy.LocalSessionStore
import com.example.zhivoy.data.repository.AuthRepository
import com.example.zhivoy.data.repository.SyncRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import com.example.zhivoy.network.ApiClient

@Composable
fun SplashScreen(
    onGoAuth: () -> Unit,
    onGoOnboarding: () -> Unit,
    onGoMain: () -> Unit,
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
    val session by sessionStore.session.collectAsState(initial = null)

    LaunchedEffect(session) {
        // Даем UI чуть "подышать", чтобы переход выглядел мягко
        delay(350)
        
        // Пытаемся автоматически войти по refresh token
        val refreshToken = sessionStore.getRefreshToken()
        if (refreshToken != null && session == null) {
            val refreshResult = authRepository.refreshToken()
            refreshResult.fold(
                onSuccess = {
                    // Токен обновлен, получаем сессию снова
                    val newSession = sessionStore.session.first()
                    if (newSession != null) {
                        // Проверяем наличие профиля на бекенде
                        val profileResult = authRepository.getProfile()
                        profileResult.fold(
                            onSuccess = { _ -> onGoMain() },
                            onFailure = { onGoOnboarding() }
                        )
                    } else {
                        onGoAuth()
                    }
                },
                onFailure = {
                    // Не удалось обновить токен, идем на авторизацию
                    onGoAuth()
                }
            )
            return@LaunchedEffect
        }
        
        if (session == null) {
            onGoAuth()
            return@LaunchedEffect
        }
        
        // Проверяем наличие профиля на бекенде
        val profileResult = authRepository.getProfile()
        profileResult.fold(
            onSuccess = { _ -> onGoMain() },
            onFailure = { onGoOnboarding() }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = "Zhivoy",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Fitness • Health • Family",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


