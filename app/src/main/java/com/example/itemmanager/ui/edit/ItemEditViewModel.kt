package com.example.itemmanager.ui.edit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.itemmanager.data.local.ItemEntity
import com.example.itemmanager.data.repository.ItemRepository
import com.example.itemmanager.util.DraftManager
import com.example.itemmanager.util.ImageUtils
import com.example.itemmanager.util.ItemDraft
import com.example.itemmanager.util.SelectedItemHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 物品添加/编辑页 ViewModel
 * - 支持为同一物品添加多张照片（相册/相机），可单独删除
 * - 支持购买日期、保质期、保修期
 * - 编辑模式从 SelectedItemHolder 读取 ID；添加模式自动保存草稿
 */
class ItemEditViewModel(
    private val application: Application,
    private val repository: ItemRepository
) : ViewModel() {

    private val editingItemId: Long? = SelectedItemHolder.consume()
    val isEditing: Boolean = editingItemId != null

    // ---- 文字字段 ----
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location

    private val _quantity = MutableStateFlow("1")
    val quantity: StateFlow<String> = _quantity

    // ---- 多张图片（统一为内部存储路径列表）----
    private val _imagePaths = MutableStateFlow<List<String>>(emptyList())
    val imagePaths: StateFlow<List<String>> = _imagePaths

    // 编辑模式下原有的图片（用于判断新增/删除）
    private var originalPaths: List<String> = emptyList()

    // ---- 日期字段 ----
    private val _purchaseDate = MutableStateFlow<Long?>(null)
    val purchaseDate: StateFlow<Long?> = _purchaseDate

    private val _expiryDate = MutableStateFlow<Long?>(null)
    val expiryDate: StateFlow<Long?> = _expiryDate

    private val _warrantyDate = MutableStateFlow<Long?>(null)
    val warrantyDate: StateFlow<Long?> = _warrantyDate

    // ---- 其他状态 ----
    private val _saveCompleted = MutableStateFlow(false)
    val saveCompleted: StateFlow<Boolean> = _saveCompleted

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _pendingDraft = MutableStateFlow<ItemDraft?>(null)
    val pendingDraft: StateFlow<ItemDraft?> = _pendingDraft

    init {
        if (editingItemId != null) {
            loadExistingItem(editingItemId)
        } else {
            DraftManager.loadDraft(application)?.let { _pendingDraft.value = it }
        }
    }

    private fun loadExistingItem(id: Long) {
        viewModelScope.launch {
            repository.getItemById(id)?.let { item ->
                _name.value = item.name
                _category.value = item.category
                _description.value = item.description
                _location.value = item.location
                _quantity.value = item.quantity.toString()
                _imagePaths.value = item.imagePaths
                originalPaths = item.imagePaths
                _purchaseDate.value = item.purchaseDate
                _expiryDate.value = item.expiryDate
                _warrantyDate.value = item.warrantyDate
            }
        }
    }

    // ---- 多图管理 ----

    /** 从相册选择图片：复制到内部存储后加入列表 */
    fun addImageFromUri(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val savedPath = ImageUtils.copyImageToInternalStorage(application, uri)
            if (savedPath != null) {
                _imagePaths.update { it + savedPath }
                autoSaveDraft()
            } else {
                _errorMessage.value = "图片添加失败，请重试"
            }
        }
    }

    /** 相机拍照完成：照片已在内部存储，加入列表 */
    fun addPhotoPath(path: String?) {
        if (path == null) return
        _imagePaths.update { if (path in it) it else it + path }
        autoSaveDraft()
    }

    /** 删除某张图片：新增的图片同时删除文件 */
    fun removeImage(path: String) {
        _imagePaths.update { it - path }
        if (path !in originalPaths) ImageUtils.deleteImageFile(path)
        autoSaveDraft()
    }

    // ---- 日期设置 ----

    fun setPurchaseDate(ts: Long?) { _purchaseDate.value = ts; autoSaveDraft() }
    fun setExpiryDate(ts: Long?) { _expiryDate.value = ts; autoSaveDraft() }
    fun setWarrantyDate(ts: Long?) { _warrantyDate.value = ts; autoSaveDraft() }

    // ---- 草稿 ----

    private fun autoSaveDraft() {
        if (isEditing || _pendingDraft.value != null) return
        val draft = ItemDraft(
            name = _name.value,
            category = _category.value,
            description = _description.value,
            location = _location.value,
            quantity = _quantity.value,
            imagePaths = _imagePaths.value,
            purchaseDate = _purchaseDate.value,
            expiryDate = _expiryDate.value,
            warrantyDate = _warrantyDate.value
        )
        DraftManager.saveDraft(application, draft)
    }

    /** 恢复草稿 */
    fun restoreDraft() {
        val d = _pendingDraft.value ?: return
        _name.value = d.name
        _category.value = d.category
        _description.value = d.description
        _location.value = d.location
        _quantity.value = d.quantity
        _imagePaths.value = d.imagePaths
        _purchaseDate.value = d.purchaseDate
        _expiryDate.value = d.expiryDate
        _warrantyDate.value = d.warrantyDate
        _pendingDraft.value = null
    }

    /** 丢弃草稿 */
    fun discardDraft() {
        DraftManager.clearDraft(application)
        _pendingDraft.value = null
    }

    // ---- 文字更新 ----

    fun onNameChanged(value: String) { _name.value = value; autoSaveDraft() }
    fun onCategoryChanged(value: String) { _category.value = value; autoSaveDraft() }
    fun onDescriptionChanged(value: String) { _description.value = value; autoSaveDraft() }
    fun onLocationChanged(value: String) { _location.value = value; autoSaveDraft() }
    fun onQuantityChanged(value: String) {
        if (value.all { it.isDigit() }) {
            _quantity.value = value
            autoSaveDraft()
        }
    }

    /**
     * 保存物品
     */
    fun saveItem() {
        if (_name.value.isBlank()) { _errorMessage.value = "请输入物品名称"; return }
        if (_category.value.isBlank()) { _errorMessage.value = "请输入物品分类"; return }

        viewModelScope.launch {
            val qty = _quantity.value.toIntOrNull() ?: 1
            val now = System.currentTimeMillis()

            if (isEditing && editingItemId != null) {
                // 清理被移除的原有图片文件
                (originalPaths - _imagePaths.value.toSet()).forEach {
                    ImageUtils.deleteImageFile(it)
                }
                val existing = repository.getItemById(editingItemId)
                val updated = existing?.copy(
                    name = _name.value.trim(),
                    category = _category.value.trim(),
                    description = _description.value.trim(),
                    location = _location.value.trim(),
                    quantity = qty,
                    imagePaths = _imagePaths.value,
                    purchaseDate = _purchaseDate.value,
                    expiryDate = _expiryDate.value,
                    warrantyDate = _warrantyDate.value,
                    updatedAt = now
                )
                if (updated != null) repository.updateItem(updated)
            } else {
                val newItem = ItemEntity(
                    name = _name.value.trim(),
                    category = _category.value.trim(),
                    description = _description.value.trim(),
                    location = _location.value.trim(),
                    quantity = qty,
                    imagePaths = _imagePaths.value,
                    purchaseDate = _purchaseDate.value,
                    expiryDate = _expiryDate.value,
                    warrantyDate = _warrantyDate.value,
                    createdAt = now,
                    updatedAt = now
                )
                repository.insertItem(newItem)
            }

            DraftManager.clearDraft(application)
            SelectedItemHolder.clear()
            _saveCompleted.value = true
        }
    }

    fun clearError() { _errorMessage.value = null }

    companion object {
        fun provideFactory(
            application: Application,
            repository: ItemRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ItemEditViewModel(application, repository) as T
                }
            }
    }
}
