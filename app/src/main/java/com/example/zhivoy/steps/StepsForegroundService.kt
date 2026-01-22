package com.example.zhivoy.steps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.zhivoy.MainActivity
import com.example.zhivoy.data.AppDatabase
import com.example.zhivoy.data.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StepsForegroundService : Service() {
    companion object {
        private const val CHANNEL_ID = "steps_tracking"
        private const val NOTIFICATION_ID = 101

        private const val ACTION_START = "com.example.zhivoy.steps.START"
        private const val ACTION_STOP = "com.example.zhivoy.steps.STOP"

        fun start(context: Context) {
            val intent = Intent(context, StepsForegroundService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, StepsForegroundService::class.java).setAction(ACTION_STOP))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tracker: StepsTracker? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startForegroundCompat(buildNotification(contentText = "Запуск шагомера…"))
                startTracking()
                return START_STICKY
            }
            else -> return START_STICKY
        }
    }

    override fun onDestroy() {
        stopTracking()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTracking() {
        if (tracker != null) return
        val appContext = applicationContext
        
        // Проверяем разрешения перед запуском
        val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        if (!activityGranted) {
            stopSelf()
            return
        }

        val sessionStore = SessionStore(appContext)

        scope.launch {
            val session = sessionStore.session.first()
            val userId = session?.userId
            if (userId == null) {
                // no session -> stop
                stopSelf()
                return@launch
            }

            val db = withContext(Dispatchers.IO) {
                androidx.room.Room.databaseBuilder(appContext, AppDatabase::class.java, "zhivoy.db")
                    .fallbackToDestructiveMigration()
                    .build()
            }

            val localTracker = StepsTracker(
                context = appContext,
                db = db,
                userId = userId,
                scope = scope,
            )
            tracker = localTracker
            localTracker.start()

            // Реактивно обновляем уведомление при изменении шагов в БД
            scope.launch {
                val today = com.example.zhivoy.util.DateTime.epochDayNow()
                db.stepsDao().observeTotalForDay(userId, today).collect { steps ->
                    updateNotification("Пройдено за сегодня: $steps")
                }
            }
        }
    }

    private fun stopTracking() {
        tracker?.stop()
        tracker = null
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Шагомер",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Фоновый подсчет шагов"
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0),
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, StepsForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0),
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.example.zhivoy.R.mipmap.ic_launcher)
            .setContentTitle("Zhivoy • Активность")
            .setContentText(contentText)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            // Android 14+: pass type explicitly
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(contentText: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(contentText))
    }
}


