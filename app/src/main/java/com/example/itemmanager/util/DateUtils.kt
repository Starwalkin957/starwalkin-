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
     * 距离目标日期还有多少天
     * 正数=剩余天数，0=今天到期，负数=已过期天数
     */
    fun daysUntil(timestamp: Long): Long {
        val target = toLocalDate(timestamp)
        return ChronoUnit.DAYS.between(LocalDate.now(), target)
    }

    /**
     * 有效期状态文案
     * @return 状态描述，null 表示未设置日期
     */
    fun statusText(timestamp: Long?, label: String): String? {
        if (timestamp == null) return null
        val days = daysUntil(timestamp)
        return when {
            days < 0 -> "${label}已过期 ${-days} 天"
            days == 0L -> "${label}今天到期"
            else -> "${label}剩余 $days 天"
        }
    }
}
