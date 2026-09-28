package com.example.itemmanager.util

import com.example.itemmanager.data.local.ItemEntity

/**
 * 有效期提醒工具
 * - 保质期：到期前一天（含当天）弹窗提醒
 * - 保修期：到期前一周（7 天内，含当天）弹窗提醒
 * 已过期的不在此提醒（详情页会显示）。
 * 返回类型标识，由界面结合多语言资源生成提示文字。
 */
object ExpiryReminder {

    enum class Kind { EXPIRY_TOMORROW, EXPIRY_TODAY, WARRANTY_DAYS, WARRANTY_TODAY }

    /** 单条提醒 */
    data class Reminder(
        val itemName: String,
        val kind: Kind,
        val days: Int = 0
    )

    /** 扫描物品，收集需要弹窗提醒的条目 */
    fun collect(items: List<ItemEntity>): List<Reminder> {
        val result = mutableListOf<Reminder>()
        items.forEach { item ->
            // 保质期：前一天（明天）或今天到期
            item.expiryDate?.let { ts ->
                val days = DateUtils.daysUntil(ts)
                when (days) {
                    1L -> result.add(Reminder(item.name, Kind.EXPIRY_TOMORROW))
                    0L -> result.add(Reminder(item.name, Kind.EXPIRY_TODAY))
                    else -> {}
                }
            }
            // 保修期：前一周内（0..7 天）
            item.warrantyDate?.let { ts ->
                val days = DateUtils.daysUntil(ts)
                if (days in 0..7) {
                    if (days == 0L) {
                        result.add(Reminder(item.name, Kind.WARRANTY_TODAY))
                    } else {
                        result.add(Reminder(item.name, Kind.WARRANTY_DAYS, days.toInt()))
                    }
                }
            }
        }
        return result
    }
}
