package com.example.itemmanager.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 应用背景图管理
 * 用户可导入一张照片作为整个 App 的背景，持久化记住，也可清除。
 */
object BackgroundManager {

    private const val PREFS_NAME = "background_prefs"
    private const val KEY_PATH = "background_path"

    private val _backgroundPath = MutableStateFlow<String?>(null)
    val backgroundPath: StateFlow<String?> = _backgroundPath

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** App 启动时读取 */
    fun init(context: Context) {
        _backgroundPath.value = prefs(context).getString(KEY_PATH, null)
    }

    /** 设置背景图（path 为已复制到内部存储的路径） */
    fun setBackground(context: Context, path: String) {
        // 切换时删除旧背景
        _backgroundPath.value?.takeIf { it != path }?.let { ImageUtils.deleteImageFile(it) }
        _backgroundPath.value = path
        prefs(context).edit().putString(KEY_PATH, path).apply()
    }

    /** 清除背景并删除文件 */
    fun clearBackground(context: Context) {
        _backgroundPath.value?.let { ImageUtils.deleteImageFile(it) }
        _backgroundPath.value = null
        prefs(context).edit().remove(KEY_PATH).apply()
    }
}
