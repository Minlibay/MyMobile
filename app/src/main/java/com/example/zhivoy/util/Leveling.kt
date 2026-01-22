package com.example.zhivoy.util

import kotlin.math.floor
import kotlin.math.sqrt

data class LevelInfo(
    val level: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int,
)

object Leveling {
    /**
     * Простая кривая:
     * - 1 уровень начинается с 0 XP
     * - для каждого следующего нужно чуть больше (прибл. квадратично)
     */
    fun levelInfo(totalXp: Int): LevelInfo {
        val xp = totalXp.coerceAtLeast(0)

        // totalXp ~= 100*(L-1)^2  =>  L ~= 1 + sqrt(totalXp/100)
        val raw = 1.0 + sqrt(xp / 100.0)
        val level = floor(raw).toInt().coerceAtLeast(1)

        val levelBase = xpForLevelStart(level)
        val nextBase = xpForLevelStart(level + 1)
        return LevelInfo(
            level = level,
            xpIntoLevel = xp - levelBase,
            xpForNextLevel = nextBase - levelBase,
        )
    }

    fun xpForLevelStart(level: Int): Int {
        val l = level.coerceAtLeast(1)
        val x = (l - 1)
        return 100 * x * x
    }
}





