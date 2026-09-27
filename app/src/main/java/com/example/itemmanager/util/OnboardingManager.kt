package com.example.itemmanager.util

import android.content.Context

/**
 * 新用户引导状态管理
 * 用 SharedPreferences 记录用户是否已看过引导页：
 * - 首次打开显示引导
 * - 跳过或完成后不再自动弹出
 */
object OnboardingManager {

    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_COMPLETED = "onboarding_completed"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 是否应该显示引导页（未完成过引导时返回 true） */
    fun shouldShow(context: Context): Boolean =
        !prefs(context).getBoolean(KEY_COMPLETED, false)

    /** 标记引导已完成（跳过或看完时调用） */
    fun setCompleted(context: Context) {
        prefs(context).edit().putBoolean(KEY_COMPLETED, true).apply()
    }
}
