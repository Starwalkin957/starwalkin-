package com.example.itemmanager.util

import android.content.Context

/**
 * 物品草稿数据
 * 仅持久化文字字段和已保存到内部存储的图片路径（相机拍摄）。
 * 相册临时 Uri 因权限会失效，不纳入草稿。
 */
data class ItemDraft(
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val location: String = "",
    val quantity: String = "1",
    val imagePath: String? = null
) {
    /** 草稿是否包含有效内容（任意字段有值即视为可恢复） */
    fun isNotEmpty(): Boolean =
        name.isNotBlank() || category.isNotBlank() || description.isNotBlank()
            || location.isNotBlank() || imagePath != null
}

/**
 * 草稿管理器
 * 使用 SharedPreferences 自动保存"添加物品"页的表单内容。
 * 即使用户在填写过程中关闭/杀掉应用，下次进入添加页也能恢复草稿继续编辑，
 * 实现"关闭应用后依然可以继续保存文件"。保存成功后自动清除草稿。
 */
object DraftManager {

    private const val PREFS_NAME = "item_draft_prefs"
    private const val KEY_NAME = "name"
    private const val KEY_CATEGORY = "category"
    private const val KEY_DESCRIPTION = "description"
    private const val KEY_LOCATION = "location"
    private const val KEY_QUANTITY = "quantity"
    private const val KEY_IMAGE_PATH = "image_path"
    private const val KEY_HAS_DRAFT = "has_draft"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 保存草稿（表单每次变化时调用） */
    fun saveDraft(context: Context, draft: ItemDraft) {
        prefs(context).edit().apply {
            putString(KEY_NAME, draft.name)
            putString(KEY_CATEGORY, draft.category)
            putString(KEY_DESCRIPTION, draft.description)
            putString(KEY_LOCATION, draft.location)
            putString(KEY_QUANTITY, draft.quantity)
            putString(KEY_IMAGE_PATH, draft.imagePath)
            putBoolean(KEY_HAS_DRAFT, true)
            apply()
        }
    }

    /** 读取草稿，无草稿返回 null */
    fun loadDraft(context: Context): ItemDraft? {
        val p = prefs(context)
        if (!p.getBoolean(KEY_HAS_DRAFT, false)) return null
        val draft = ItemDraft(
            name = p.getString(KEY_NAME, "") ?: "",
            category = p.getString(KEY_CATEGORY, "") ?: "",
            description = p.getString(KEY_DESCRIPTION, "") ?: "",
            location = p.getString(KEY_LOCATION, "") ?: "",
            quantity = p.getString(KEY_QUANTITY, "1") ?: "1",
            imagePath = p.getString(KEY_IMAGE_PATH, null)
        )
        return if (draft.isNotEmpty()) draft else null
    }

    /** 是否存在可恢复草稿 */
    fun hasDraft(context: Context): Boolean = loadDraft(context) != null

    /** 清除草稿（保存成功或用户主动放弃时调用） */
    fun clearDraft(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
