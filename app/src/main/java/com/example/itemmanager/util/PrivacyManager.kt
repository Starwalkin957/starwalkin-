package com.example.itemmanager.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.security.MessageDigest

/**
 * 加密箱密码管理器
 * 支持两种解锁方式（用户首次设置时二选一）：
 * - 6 位数字密码（PIN）
 * - 手势密码（9 宫格连线，至少连接 4 个点）
 * 密码仅以 SHA-256（带固定盐）哈希形式存储，不保存明文；
 * 密码配置可导出/导入，用于跨设备 ZIP 同步。
 */
object PrivacyManager {

    private const val PREFS_NAME = "privacy_prefs"
    private const val KEY_TYPE = "pw_type"
    private const val KEY_HASH = "pw_hash"
    private const val SALT = "item_manager_privacy_salt_v1::"

    const val TYPE_NONE = "none"
    const val TYPE_PIN = "pin"
    const val TYPE_PATTERN = "pattern"

    private lateinit var prefs: SharedPreferences

    /** 初始化（幂等） */
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** 是否已设置密码 */
    fun hasPassword(): Boolean = passwordType() != TYPE_NONE

    /** 当前密码类型 */
    fun passwordType(): String =
        prefs.getString(KEY_TYPE, TYPE_NONE) ?: TYPE_NONE

    /** 设置 6 位数字密码 */
    fun setPin(pin: String) {
        require(pin.length == 6 && pin.all { it.isDigit() }) { "PIN must be 6 digits" }
        prefs.edit()
            .putString(KEY_TYPE, TYPE_PIN)
            .putString(KEY_HASH, hash(pin))
            .apply()
    }

    /** 验证数字密码 */
    fun verifyPin(pin: String): Boolean =
        constantTimeEquals(hash(pin), prefs.getString(KEY_HASH, ""))

    /** 设置手势密码（点索引列表，至少 4 个点） */
    fun setPattern(points: List<Int>) {
        require(points.size >= 4) { "Pattern needs at least 4 points" }
        prefs.edit()
            .putString(KEY_TYPE, TYPE_PATTERN)
            .putString(KEY_HASH, hash(points.joinToString(",")))
            .apply()
    }

    /** 验证手势密码 */
    fun verifyPattern(points: List<Int>): Boolean =
        constantTimeEquals(
            hash(points.joinToString(",")),
            prefs.getString(KEY_HASH, "")
        )

    /** SHA-256 带盐哈希，输出十六进制字符串 */
    private fun hash(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((SALT + input).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** 常量时间比较，避免计时侧信道 */
    private fun constantTimeEquals(a: String, b: String?): Boolean {
        if (b == null || a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    /** 导出密码配置（类型 + 哈希），用于 ZIP 同步 */
    fun exportConfig(): JSONObject {
        val obj = JSONObject()
        obj.put("type", passwordType())
        obj.put("hash", prefs.getString(KEY_HASH, "") ?: "")
        return obj
    }

    /** 导入密码配置（从 ZIP 同步） */
    fun importConfig(obj: JSONObject) {
        val type = obj.optString("type", TYPE_NONE)
        val hashValue = obj.optString("hash", "")
        prefs.edit()
            .putString(KEY_TYPE, type)
            .putString(KEY_HASH, hashValue)
            .apply()
    }

    /** 重置密码（清空） */
    fun reset() {
        prefs.edit().clear().apply()
    }
}
