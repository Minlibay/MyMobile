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
import com.example.zhivoy.data.entities.ProfileEntity
import com.example.zhivoy.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Composable
fun SplashScreen(
    onGoAuth: () -> Unit,
    onGoOnboarding: () -> Unit,
    onGoMain: () -> Unit,
) {
    val context = LocalContext.current
    val db = LocalAppDatabase.current
    val sessionStore = LocalSessionStore.current
    val authRepository = remember { AuthRepository(context, sessionStore) }
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
                        // Пытаемся загрузить профиль с бекенда
                        val profileResult = authRepository.getProfile()
                        profileResult.fold(
                            onSuccess = { profileResponse ->
                                // Сохраняем профиль локально
                                withContext(Dispatchers.IO) {
                                    val now = System.currentTimeMillis()
                                    db.profileDao().upsert(
                                        ProfileEntity(
                                            userId = newSession.userId,
                                            heightCm = profileResponse.height_cm,
                                            weightKg = profileResponse.weight_kg,
                                            age = profileResponse.age,
                                            sex = profileResponse.sex,
                                            createdAtEpochMs = now,
                                            updatedAtEpochMs = now,
                                        )
                                    )
                                }
                                onGoMain()
                            },
                            onFailure = {
                                // Профиль не найден на бекенде, проверяем локально
                                val hasProfile = withContext(Dispatchers.IO) {
                                    db.profileDao().getByUserId(newSession.userId) != null
                                }
                                if (hasProfile) onGoMain() else onGoOnboarding()
                            }
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
        
        // Пытаемся загрузить профиль с бекенда
        val profileResult = authRepository.getProfile()
        profileResult.fold(
            onSuccess = { profileResponse ->
                // Сохраняем профиль локально
                withContext(Dispatchers.IO) {
                    val now = System.currentTimeMillis()
                    db.profileDao().upsert(
                        ProfileEntity(
                            userId = session!!.userId,
                            heightCm = profileResponse.height_cm,
                            weightKg = profileResponse.weight_kg,
                            age = profileResponse.age,
                            sex = profileResponse.sex,
                            createdAtEpochMs = now,
                            updatedAtEpochMs = now,
                        )
                    )
                }
                onGoMain()
            },
            onFailure = {
                // Профиль не найден на бекенде, проверяем локально
                val hasProfile = withContext(Dispatchers.IO) {
                    db.profileDao().getByUserId(session!!.userId) != null
                }
                if (hasProfile) onGoMain() else onGoOnboarding()
            }
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


