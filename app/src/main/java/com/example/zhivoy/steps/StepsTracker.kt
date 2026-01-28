package com.example.zhivoy.steps

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.example.zhivoy.data.AppDatabase
import com.example.zhivoy.data.repository.StepsRepository
import com.example.zhivoy.data.entities.StepCounterStateEntity
import com.example.zhivoy.data.entities.StepEntryEntity
import com.example.zhivoy.util.DateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StepsTracker(
    context: Context,
    private val db: AppDatabase,
    private val userId: Long,
    private val scope: CoroutineScope,
    private val stepsRepository: StepsRepository? = null,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var isStarted = false
    private var job: Job? = null

    // throttle writes
    private var lastSavedAtElapsed: Long = 0
    private var lastSavedSteps: Int = -1

    fun start() {
        if (isStarted) return
        isStarted = true
        if (stepCounter != null) {
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        sensorManager.unregisterListener(this)
        job?.cancel()
        job = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isStarted) return
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

        val sensorTotal = event.values.firstOrNull()?.toLong() ?: return
        val epochDay = DateTime.epochDayNow()

        val nowElapsed = SystemClock.elapsedRealtime()
        val shouldWrite = (nowElapsed - lastSavedAtElapsed) > 3_000 // every 3s max
        if (!shouldWrite) return

        lastSavedAtElapsed = nowElapsed

        job?.cancel()
        job = scope.launch {
            withContext(Dispatchers.IO) {
                val current = db.stepCounterStateDao().get(userId)

                val state = when {
                    current == null -> StepCounterStateEntity(
                        userId = userId,
                        epochDay = epochDay,
                        baselineSensorTotal = sensorTotal,
                        lastSensorTotal = sensorTotal,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                    // day changed -> reset baseline to current sensor total
                    current.epochDay != epochDay -> StepCounterStateEntity(
                        id = current.id,
                        userId = userId,
                        epochDay = epochDay,
                        baselineSensorTotal = sensorTotal,
                        lastSensorTotal = sensorTotal,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                    // device reboot detected (current sensor value is less than last recorded)
                    sensorTotal < current.lastSensorTotal -> {
                        // We need to adjust baseline so that (sensorTotal - adjustedBaseline) == currentTotalBeforeReboot
                        val stepsBeforeReboot = (current.lastSensorTotal - current.baselineSensorTotal).coerceAtLeast(0)
                        current.copy(
                            baselineSensorTotal = sensorTotal - stepsBeforeReboot,
                            lastSensorTotal = sensorTotal,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }
                    else -> current.copy(
                        lastSensorTotal = sensorTotal,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }

                db.stepCounterStateDao().upsert(state)

                val stepsToday = (state.lastSensorTotal - state.baselineSensorTotal).coerceAtLeast(0).toInt()
                if (stepsToday == lastSavedSteps) return@withContext
                lastSavedSteps = stepsToday

                db.stepsDao().upsert(
                    StepEntryEntity(
                        userId = userId,
                        dateEpochDay = epochDay,
                        steps = stepsToday,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    ),
                )

                // Sync to backend (best-effort)
                try {
                    stepsRepository?.upsertSteps(epochDay, stepsToday)
                } catch (_: Exception) {
                    // ignore: offline / server issues should not break local tracking
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}


