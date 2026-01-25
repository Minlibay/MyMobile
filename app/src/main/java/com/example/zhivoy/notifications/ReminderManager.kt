package com.example.zhivoy.notifications

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object ReminderManager {
    fun scheduleReminder(context: Context, type: String, delayHours: Long) {
        val data = workDataOf("type" to type)
        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayHours, TimeUnit.HOURS)
            .setInputData(data)
            .addTag("reminder_$type")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_$type",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelReminder(context: Context, type: String) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$type")
    }

    // Периодическое напоминание (например, каждые 4 часа в дневное время)
    fun schedulePeriodicReminders(context: Context) {
        val stretchRequest = PeriodicWorkRequestBuilder<ReminderWorker>(4, TimeUnit.HOURS)
            .setInputData(workDataOf("type" to "stretch"))
            .addTag("periodic_stretch")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "periodic_stretch",
            ExistingPeriodicWorkPolicy.KEEP,
            stretchRequest
        )
        
        val bookRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInputData(workDataOf("type" to "book"))
            .addTag("periodic_book")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "periodic_book",
            ExistingPeriodicWorkPolicy.KEEP,
            bookRequest
        )
    }
}












