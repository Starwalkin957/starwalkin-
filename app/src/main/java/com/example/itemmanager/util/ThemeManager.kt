package com.example.itemmanager.util

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 主题配色管理
 * 支持蓝色、白色、黑色、紫色四套预设，以及调色盘自定义颜色。
 * 用 SharedPreferences 记住选择，通过 StateFlow 即时刷新界面。
 */
object ThemeManager {

    const val THEME_BLUE = 0
    const val THEME_WHITE = 1
    const val THEME_BLACK = 2
    const val THEME_PURPLE = 3
    const val THEME_CUSTOM = 4

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "theme_id"
    private const val KEY_CUSTOM = "custom_color"

    private val _themeId = MutableStateFlow(THEME_BLUE)
    val themeId: StateFlow<Int> = _themeId

    // 用户自定义的主题色
    private val _customColor = MutableStateFlow(Color(0xFF2563EB))
    val customColor: StateFlow<Color> = _customColor

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** App 启动时初始化 */
    fun init(context: Context) {
        val p = prefs(context)
        _themeId.value = p.getInt(KEY_THEME, THEME_BLUE)
        val stored = p.getLong(KEY_CUSTOM, 0xFF2563EB)
        _customColor.value = Color(stored)
    }

    /** 切换预设主题并持久化 */
    fun setTheme(context: Context, id: Int) {
        _themeId.value = id
        prefs(context).edit().putInt(KEY_THEME, id).apply()
    }

    /** 设置自定义颜色并切换到自定义主题 */
    fun setCustomColor(context: Context, color: Color) {
        _customColor.value = color
        _themeId.value = THEME_CUSTOM
        prefs(context).edit()
            .putInt(KEY_THEME, THEME_CUSTOM)
            .putLong(KEY_CUSTOM, color.value.toLong())
            .apply()
    }
}
