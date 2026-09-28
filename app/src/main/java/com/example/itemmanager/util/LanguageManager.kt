package com.example.itemmanager.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * 应用内语言切换管理
 * 通过 AppCompatDelegate.setApplicationLocales 实现，切换后系统自动重建界面。
 */
object LanguageManager {

    data class Language(val tag: String, val displayName: String)

    /** 支持的 9 种语言 */
    val languages = listOf(
        Language("zh-CN", "简体中文"),
        Language("zh-TW", "繁體中文"),
        Language("en", "English"),
        Language("fr", "Français"),
        Language("es", "Español"),
        Language("ru", "Русский"),
        Language("de", "Deutsch"),
        Language("ja", "日本語"),
        Language("ko", "한국어")
    )

    /** 切换语言 */
    fun setLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** 当前语言标签 */
    fun currentTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return "zh-CN"
        val locale = locales[0] ?: return "zh-CN"
        return locale.toLanguageTag()
    }
}
