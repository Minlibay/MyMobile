package com.example.zhivoy.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object DateTime {
    fun epochDayNow(): Int = LocalDate.now().toEpochDay().toInt()

    fun epochDayFromEpochMs(epochMs: Long): Int {
        return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay().toInt()
    }
}





