package com.example.itemmanager.util

import android.content.Context

/**
 * 物品草稿数据
 * 持久化文字字段、多张已保存到内部存储的图片路径（相机拍摄）和日期字段。
 * 相册临时 Uri 因权限会失效，不纳入草稿。
 */
data class ItemDraft(
    val name: String = "",
    val category: String = "",
    val brand: String = "",
    val description: String = "",
    val location: String = "",
    val quantity: String = "1",
    val imagePaths: List<String> = emptyList(),
    val purchaseDate: Long? = null,
    val expiryDate: Long? = null,
    val warrantyDate: Long? = null,
    val borrower: String = "",
    val expectedReturnDate: Long? = null
) {
    /** 草稿是否包含有效内容 */
    fun isNotEmpty(): Boolean =
        name.isNotBlank() || category.isNotBlank() || description.isNotBlank()
            || location.isNotBlank() || imagePaths.isNotEmpty()
}

/**
 * 草稿管理器
 * 用 SharedPreferences 自动保存"添加物品"页的表单内容，
 * 关闭/杀掉应用后再次进入可恢复草稿继续编辑。保存成功后自动清除。
 */
object DraftManager {

    private const val PREFS_NAME = "item_draft_prefs"
    private const val KEY_NAME = "name"
    private const val KEY_CATEGORY = "category"
    private const val KEY_BRAND = "brand"
    private const val KEY_DESCRIPTION = "description"
    private const val KEY_LOCATION = "location"
    private const val KEY_QUANTITY = "quantity"
    private const val KEY_IMAGE_PATHS = "image_paths"
    private const val KEY_PURCHASE = "purchase_date"
    private const val KEY_EXPIRY = "expiry_date"
    private const val KEY_WARRANTY = "warranty_date"
    private const val KEY_BORROWER = "borrower"
    private const val KEY_EXPECTED_RETURN = "expected_return_date"
    private const val KEY_HAS_DRAFT = "has_draft"
    private const val NONE = Long.MIN_VALUE

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 保存草稿 */
    fun saveDraft(context: Context, draft: ItemDraft) {
        prefs(context).edit().apply {
            putString(KEY_NAME, draft.name)
            putString(KEY_CATEGORY, draft.category)
            putString(KEY_BRAND, draft.brand)
            putString(KEY_DESCRIPTION, draft.description)
            putString(KEY_LOCATION, draft.location)
            putString(KEY_QUANTITY, draft.quantity)
            putString(KEY_IMAGE_PATHS, draft.imagePaths.joinToString("|"))
            putLong(KEY_PURCHASE, draft.purchaseDate ?: NONE)
            putLong(KEY_EXPIRY, draft.expiryDate ?: NONE)
            putLong(KEY_WARRANTY, draft.warrantyDate ?: NONE)
            putString(KEY_BORROWER, draft.borrower)
            putLong(KEY_EXPECTED_RETURN, draft.expectedReturnDate ?: NONE)
            putBoolean(KEY_HAS_DRAFT, true)
            apply()
        }
    }

    /** 读取草稿，无草稿返回 null */
    fun loadDraft(context: Context): ItemDraft? {
        val p = prefs(context)
        if (!p.getBoolean(KEY_HAS_DRAFT, false)) return null

        fun date(key: String): Long? = p.getLong(key, NONE).let { if (it == NONE) null else it }

        val pathsStr = p.getString(KEY_IMAGE_PATHS, "") ?: ""
        val draft = ItemDraft(
            name = p.getString(KEY_NAME, "") ?: "",
            category = p.getString(KEY_CATEGORY, "") ?: "",
            brand = p.getString(KEY_BRAND, "") ?: "",
            description = p.getString(KEY_DESCRIPTION, "") ?: "",
            location = p.getString(KEY_LOCATION, "") ?: "",
            quantity = p.getString(KEY_QUANTITY, "1") ?: "1",
            imagePaths = if (pathsStr.isBlank()) emptyList()
            else pathsStr.split("|").filter { it.isNotBlank() },
            purchaseDate = date(KEY_PURCHASE),
            expiryDate = date(KEY_EXPIRY),
            warrantyDate = date(KEY_WARRANTY),
            borrower = p.getString(KEY_BORROWER, "") ?: "",
            expectedReturnDate = date(KEY_EXPECTED_RETURN)
        )
        return if (draft.isNotEmpty()) draft else null
    }

    /** 是否存在可恢复草稿 */
    fun hasDraft(context: Context): Boolean = loadDraft(context) != null

    /** 清除草稿 */
    fun clearDraft(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
