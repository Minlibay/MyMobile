package com.example.zhivoy.feature.splash

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zhivoy.LocalAppDatabase
import com.example.zhivoy.LocalSessionStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SplashScreen(
    onGoAuth: () -> Unit,
    onGoOnboarding: () -> Unit,
    onGoMain: () -> Unit,
) {
    val db = LocalAppDatabase.current
    val sessionStore = LocalSessionStore.current
    val session by sessionStore.session.collectAsState(initial = null)

    LaunchedEffect(session) {
        // Даем UI чуть “подышать”, чтобы переход выглядел мягко
        delay(350)
        if (session == null) {
            onGoAuth()
            return@LaunchedEffect
        }
        val hasProfile = withContext(Dispatchers.IO) {
            db.profileDao().getByUserId(session!!.userId) != null
        }
        if (hasProfile) onGoMain() else onGoOnboarding()
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


