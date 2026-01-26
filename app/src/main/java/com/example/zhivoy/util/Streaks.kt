package com.example.zhivoy.util

/**
 * Считает стрик по списку "выполненных дней" (epochDay).
 * Стрик считается до сегодняшнего дня (включая), если todayDone=true,
 * иначе — до вчера (переносится).
 */
object Streaks {
    fun computeStreak(doneDays: Set<Int>, todayEpochDay: Int): Int {
        var streak = 0
        var day = todayEpochDay
        while (doneDays.contains(day)) {
            streak++
            day--
        }
        return streak
    }
}














