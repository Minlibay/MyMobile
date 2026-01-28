package com.example.zhivoy.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.zhivoy.LocalAppDatabase
import com.example.zhivoy.LocalSessionStore
import com.example.zhivoy.data.repository.AuthRepository
import com.example.zhivoy.data.repository.SyncRepository
import com.example.zhivoy.network.ApiClient
import com.example.zhivoy.feature.main.profile.AchievementsScreen
import com.example.zhivoy.steps.StepsPermissionAndTracking
import androidx.compose.material3.SnackbarHostState
import com.example.zhivoy.ui.components.ModernSnackbarHost
import com.example.zhivoy.ui.components.showSuccess
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch

private data class MainTab(
    val title: String,
    val icon: @Composable () -> Unit,
    val content: @Composable () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLoggedOut: () -> Unit,
    onOpenAds: () -> Unit = {},
) {
    StepsPermissionAndTracking()
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
    var showMenu by remember { mutableStateOf(false) }
    var showAchievements by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    if (showAchievements) {
        AchievementsScreen(onBack = { showAchievements = false })
        return
    }

    val tabs = listOf(
        MainTab(
            title = "Главная",
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            content = { HomeScreen() },
        ),
        MainTab(
            title = "Здоровье",
            icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
            content = { HealthScreen() },
        ),
        MainTab(
            title = "Тренировки",
            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
            content = { TrainingsScreen() },
        ),
        MainTab(
            title = "Семья",
            icon = { Icon(Icons.Default.Groups, contentDescription = null) },
            content = { FamilyScreen() },
        ),
        MainTab(
            title = "Мозг",
            icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
            content = { BrainScreen() },
        ),
    )

    var selected by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Zhivoy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Профиль")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Достижения") },
                                onClick = {
                                    showAchievements = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Реклама") },
                                onClick = {
                                    onOpenAds()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Выйти") },
                                onClick = {
                                    scope.launch { 
                                        authRepository.logout()
                                        snackbarHostState.showSuccess("Вы вышли из аккаунта")
                                        onLoggedOut()
                                    }
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = index == selected,
                        onClick = { selected = index },
                        icon = tab.icon,
                        alwaysShowLabel = false
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            tabs[selected].content()
            ModernSnackbarHost(
                snackbarHostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}


