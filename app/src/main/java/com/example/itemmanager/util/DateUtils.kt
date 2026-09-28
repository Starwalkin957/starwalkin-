package com.example.itemmanager.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 日期工具类
 * 负责日期与时间戳的互转、格式化，以及保质期/保修期剩余天数计算。
 */
object DateUtils {

    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

    private fun zone(): ZoneId = ZoneId.systemDefault()

    /** 时间戳转 LocalDate */
    fun toLocalDate(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone()).toLocalDate()

    /** 时间戳格式化为 yyyy-MM-dd，null 返回空串 */
    fun formatDate(timestamp: Long?): String =
        if (timestamp == null) "" else formatter.format(toLocalDate(timestamp))

    /** 指定年月日转为时间戳（该日 0 点） */
    fun dateToMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(zone())
            .toInstant()
            .toEpochMilli()

    /**
     * 解析用户手动输入的日期，支持：
     * 2026-09-28、2026/9/28、2026.9.28、2026年9月28日、2026 09 28、20260928
     * 解析失败返回 null
     */
    fun parseDate(input: String): Long? {
        return try {
            val raw = input.trim()
            // 纯数字 8 位：20260928
            if (raw.length == 8 && raw.all { it.isDigit() }) {
                return dateToMillis(
                    raw.substring(0, 4).toInt(),
                    raw.substring(4, 6).toInt(),
                    raw.substring(6, 8).toInt()
                )
            }
            // 统一分隔符：. / 空格 年 月 -> -，去掉 日
            val normalized = raw.replace(Regex("[./\\s年月]"), "-")
                .replace("日", "")
                .replace(Regex("-+"), "-")
                .trim('-')
            val parts = normalized.split("-").filter { it.isNotBlank() }
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull() ?: return null
                val m = parts[1].toIntOrNull() ?: return null
                val d = parts[2].toIntOrNull() ?: return null
                dateToMillis(y, m, d)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 距离目标日期还有多少天
     * 正数=剩余天数，0=今天到期，负数=已过期天数
     */
    fun daysUntil(timestamp: Long): Long {
        val target = toLocalDate(timestamp)
        return ChronoUnit.DAYS.between(LocalDate.now(), target)
    }
}
