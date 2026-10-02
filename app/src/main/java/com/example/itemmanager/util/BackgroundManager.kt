package com.example.itemmanager.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

/**
 * 应用背景图管理
 *
 * 维护一个"背景图册"：用户可一次添加若干张图片到图册，
 * 再从图册中挑选一张作为整个 App 的背景；也可以选择"无背景"。
 * 图册与当前选择均持久化保存。
 */
object BackgroundManager {

    private const val PREFS_NAME = "background_prefs"
    private const val KEY_PATH = "background_path"
    private const val KEY_GALLERY = "background_gallery"

    private val _gallery = MutableStateFlow<List<String>>(emptyList())
    val gallery: StateFlow<List<String>> = _gallery

    private val _backgroundPath = MutableStateFlow<String?>(null)
    val backgroundPath: StateFlow<String?> = _backgroundPath

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** App 启动时读取，并把旧版本的单张背景迁移进图册 */
    fun init(context: Context) {
        val p = prefs(context)
        val list = decodeList(p.getString(KEY_GALLERY, null)).toMutableList()
        val current = p.getString(KEY_PATH, null)
        // 兼容旧版本：旧数据只有当前背景、没有图册，将其补进图册
        if (current != null && current !in list) list.add(current)
        _gallery.value = list
        _backgroundPath.value = current
        p.edit().putString(KEY_GALLERY, encodeList(list)).apply()
    }

    /** 把一张已复制到内部存储的图片加入图册（不会自动设为背景） */
    fun addToGallery(context: Context, path: String) {
        val list = _gallery.value.toMutableList()
        if (path !in list) list.add(path)
        _gallery.value = list
        prefs(context).edit().putString(KEY_GALLERY, encodeList(list)).apply()
    }

    /** 从图册中挑选一张作为当前背景 */
    fun selectBackground(context: Context, path: String) {
        _backgroundPath.value = path
        prefs(context).edit().putString(KEY_PATH, path).apply()
    }

    /** 取消背景（回到无背景），图册中的图片仍然保留 */
    fun clearBackground(context: Context) {
        _backgroundPath.value = null
        prefs(context).edit().remove(KEY_PATH).apply()
    }

    /** 从图册删除一张图片；若它正是当前背景则同时取消背景 */
    fun removeFromGallery(context: Context, path: String) {
        val list = _gallery.value.toMutableList()
        if (!list.remove(path)) return
        _gallery.value = list
        ImageUtils.deleteImageFile(path)
        val editor = prefs(context).edit().putString(KEY_GALLERY, encodeList(list))
        if (_backgroundPath.value == path) {
            _backgroundPath.value = null
            editor.remove(KEY_PATH)
        }
        editor.apply()
    }

    private fun encodeList(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun decodeList(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
