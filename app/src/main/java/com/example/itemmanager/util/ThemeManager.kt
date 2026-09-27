package com.example.itemmanager.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 主题配色管理
 * 支持蓝色、白色、黑色、紫色四套配色，用 SharedPreferences 记住用户选择，
 * 通过 StateFlow 让界面在切换时即时刷新。
 */
object ThemeManager {

    const val THEME_BLUE = 0
    const val THEME_WHITE = 1
    const val THEME_BLACK = 2
    const val THEME_PURPLE = 3

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "theme_id"

    // 当前主题（全局可观察）
    private val _themeId = MutableStateFlow(THEME_BLUE)
    val themeId: StateFlow<Int> = _themeId

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** App 启动时初始化，读取上次选择 */
    fun init(context: Context) {
        _themeId.value = prefs(context).getInt(KEY_THEME, THEME_BLUE)
    }

    /** 切换主题并持久化 */
    fun setTheme(context: Context, id: Int) {
        _themeId.value = id
        prefs(context).edit().putInt(KEY_THEME, id).apply()
    }
}
