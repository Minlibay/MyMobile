package com.example.zhivoy.util

import com.example.zhivoy.data.entities.ProfileEntity
import kotlin.math.roundToInt

object Calories {
    /**
     * Mifflin-St Jeor (BMR)
     * male:   10w + 6.25h - 5a + 5
     * female: 10w + 6.25h - 5a - 161
     */
    fun bmr(profile: ProfileEntity): Int {
        val w = profile.weightKg
        val h = profile.heightCm.toDouble()
        val a = profile.age.toDouble()
        val base = 10.0 * w + 6.25 * h - 5.0 * a
        val sexAdd = if (profile.sex == "female") -161.0 else 5.0
        return (base + sexAdd).roundToInt()
    }

    /**
     * TDEE = BMR * activityMultiplier
     * По умолчанию считаем "умеренно сидячий" (1.2), потом можно дать выбор.
     */
    fun tdee(profile: ProfileEntity, activityMultiplier: Double = 1.2): Int {
        return (bmr(profile) * activityMultiplier).roundToInt()
    }

    fun calorieTarget(tdee: Int, mode: String): Int {
        val delta = when (mode) {
            "lose" -> -300
            "gain" -> 300
            else -> 0
        }
        return (tdee + delta).coerceAtLeast(1200)
    }
}













