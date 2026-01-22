package com.example.zhivoy.steps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.zhivoy.LocalSessionStore
import kotlinx.coroutines.launch

@Composable
fun StepsPermissionAndTracking() {
    val context = LocalContext.current
    val sessionStore = LocalSessionStore.current
    val session by sessionStore.session.collectAsState(initial = null)
    val userId = session?.userId

    val asked by sessionStore.activityRecognitionAsked.collectAsState(initial = false)
    val notificationsAsked by sessionStore.postNotificationsAsked.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    val needsActivityPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val activityGranted = !needsActivityPermission || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACTIVITY_RECOGNITION,
    ) == PackageManager.PERMISSION_GRANTED

    val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val notificationGranted = !needsNotificationPermission || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED

    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        scope.launch { sessionStore.setActivityRecognitionAsked(true) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        scope.launch { sessionStore.setPostNotificationsAsked(true) }
    }

    LaunchedEffect(userId, asked, notificationsAsked) {
        if (userId == null) return@LaunchedEffect
        
        // Спрашиваем уведомления (Android 13+)
        if (needsNotificationPermission && !notificationGranted && !notificationsAsked) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } 
        // Спрашиваем физическую активность (Android 10+)
        else if (needsActivityPermission && !activityGranted && !asked) {
            activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    // Запускаем сервис, если есть разрешение на активность. 
    // Уведомления (notificationGranted) желательны для прозрачности, но не критичны для работы датчика.
    LaunchedEffect(userId, activityGranted) {
        if (userId != null && activityGranted) {
            StepsForegroundService.start(context.applicationContext)
        }
    }
}


