package com.volovod.alta.feature.main

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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.volovod.alta.LocalAppDatabase
import com.volovod.alta.LocalSessionStore
import com.volovod.alta.data.repository.AuthRepository
import com.volovod.alta.data.repository.SyncRepository
import com.volovod.alta.network.ApiClient
import com.volovod.alta.feature.main.profile.AchievementsScreen
import com.volovod.alta.feature.main.privacy.PrivacyPolicyDialog
import com.volovod.alta.feature.main.announcement.AnnouncementDialog
import com.volovod.alta.steps.StepsPermissionAndTracking
import androidx.compose.material3.SnackbarHostState
import com.volovod.alta.ui.components.ModernSnackbarHost
import com.volovod.alta.ui.components.showSuccess
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import com.volovod.alta.network.dto.PrivacyPolicyResponseDto
import com.volovod.alta.network.dto.AnnouncementResponseDto
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
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var requirePrivacyAccept by remember { mutableStateOf(false) }
    val privacyPolicyState = remember { mutableStateOf<PrivacyPolicyResponseDto?>(null) }
    var showAnnouncement by remember { mutableStateOf(false) }
    var requireAnnouncementRead by remember { mutableStateOf(false) }
    val announcementState = remember { mutableStateOf<AnnouncementResponseDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val privacyApi = remember(sessionStore) { ApiClient.createPrivacyPolicyApi(sessionStore) }
    val announcementApi = remember(sessionStore) { ApiClient.createAnnouncementApi(sessionStore) }

    LaunchedEffect(Unit) {
        runCatching { privacyApi.getPrivacyPolicy() }
            .onSuccess { policy -> privacyPolicyState.value = policy }

        runCatching { announcementApi.getAnnouncement() }
            .onSuccess { announcement -> announcementState.value = announcement }

        val userSettings = authRepository.getUserSettings().getOrNull()
        val policyUpdatedAt = privacyPolicyState.value?.updated_at
        val acceptedPolicyUpdatedAt = userSettings?.privacy_policy_accepted_policy_updated_at
        val acceptedAt = userSettings?.privacy_policy_accepted_at

        val needsAccept = policyUpdatedAt != null && (acceptedAt == null || acceptedPolicyUpdatedAt != policyUpdatedAt)
        if (needsAccept) {
            requirePrivacyAccept = true
            showPrivacyPolicy = true
        }

        val announcementUpdatedAt = announcementState.value?.updated_at
        val announcementReadAt = userSettings?.announcement_read_at
        val announcementReadUpdatedAt = userSettings?.announcement_read_announcement_updated_at

        val needsAnnouncementRead = announcementUpdatedAt != null && (announcementReadAt == null || announcementReadUpdatedAt != announcementUpdatedAt)
        if (needsAnnouncementRead) {
            requireAnnouncementRead = true
            showAnnouncement = true
        }
    }

    if (showPrivacyPolicy && privacyPolicyState.value != null) {
        PrivacyPolicyDialog(
            text = privacyPolicyState.value!!.text,
            requireAccept = requirePrivacyAccept,
            onDismiss = {
                if (!requirePrivacyAccept) {
                    showPrivacyPolicy = false
                }
            },
            onAccept = {
                scope.launch {
                    kotlin.runCatching { privacyApi.acceptPrivacyPolicy() }
                    requirePrivacyAccept = false
                    showPrivacyPolicy = false
                }
            },
        )
    }

    if (showAnnouncement && announcementState.value != null) {
        AnnouncementDialog(
            announcement = announcementState.value!!,
            onRead = {
                scope.launch {
                    kotlin.runCatching { announcementApi.readAnnouncement() }
                    requireAnnouncementRead = false
                    showAnnouncement = false
                }
            },
            onDismiss = {
                if (!requireAnnouncementRead) {
                    showAnnouncement = false
                }
            },
        )
    }

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
                        text = "Alta",
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
                                text = { Text("Политика конфиденциальности") },
                                onClick = {
                                    requirePrivacyAccept = false
                                    showPrivacyPolicy = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.PrivacyTip, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Новости") },
                                onClick = {
                                    requireAnnouncementRead = false
                                    showAnnouncement = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.PrivacyTip, contentDescription = null) }
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


