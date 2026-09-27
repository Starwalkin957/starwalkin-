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
import kotlinx.coroutines.launch

/**
 * 物品添加/编辑页 ViewModel
 * - 编辑模式：从 SelectedItemHolder 读取物品 ID 并回填
 * - 添加模式：表单自动保存为草稿，关闭/杀掉应用后可恢复继续编辑
 */
class ItemEditViewModel(
    private val application: Application,
    private val repository: ItemRepository
) : ViewModel() {

    // 从全局持有者获取编辑模式的物品 ID（添加模式为 null）
    private val editingItemId: Long? = SelectedItemHolder.consume()

    // 表单字段状态
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

    // 已保存到内部存储的图片路径（编辑回填用）
    private val _imagePath = MutableStateFlow<String?>(null)
    val imagePath: StateFlow<String?> = _imagePath

    // 相册临时 Uri（尚未复制到内部存储）
    private val _tempImageUri = MutableStateFlow<Uri?>(null)
    val tempImageUri: StateFlow<Uri?> = _tempImageUri

    // 相机拍照后的图片路径（已直接保存到内部存储）
    private val _cameraImagePath = MutableStateFlow<String?>(null)
    val cameraImagePath: StateFlow<String?> = _cameraImagePath

    val isEditing: Boolean = editingItemId != null

    private val _saveCompleted = MutableStateFlow(false)
    val saveCompleted: StateFlow<Boolean> = _saveCompleted

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // 启动时检测到的待恢复草稿（仅添加模式），非 null 时 UI 弹窗询问
    private val _pendingDraft = MutableStateFlow<ItemDraft?>(null)
    val pendingDraft: StateFlow<ItemDraft?> = _pendingDraft

    init {
        if (editingItemId != null) {
            loadExistingItem(editingItemId)
        } else {
            // 添加模式：检查是否有上次未完成的草稿
            DraftManager.loadDraft(application)?.let { _pendingDraft.value = it }
        }
    }

    private fun loadExistingItem(id: Long) {
        viewModelScope.launch {
            val item = repository.getItemById(id)
            item?.let {
                _name.value = it.name
                _category.value = it.category
                _description.value = it.description
                _location.value = it.location
                _quantity.value = it.quantity.toString()
                _imagePath.value = it.imagePath
                _cameraImagePath.value = it.imagePath
            }
        }
    }

    // ---- 草稿：自动保存 ----

    /**
     * 自动保存草稿（仅添加模式，且没有待处理的恢复询问时）
     * 图片只持久化相机拍摄的（已在内部存储），相册临时 Uri 不持久化
     */
    private fun autoSaveDraft() {
        if (isEditing) return
        if (_pendingDraft.value != null) return
        val draft = ItemDraft(
            name = _name.value,
            category = _category.value,
            description = _description.value,
            location = _location.value,
            quantity = _quantity.value,
            imagePath = _cameraImagePath.value
        )
        DraftManager.saveDraft(application, draft)
    }

    /** 恢复草稿到表单 */
    fun restoreDraft() {
        val d = _pendingDraft.value ?: return
        _name.value = d.name
        _category.value = d.category
        _description.value = d.description
        _location.value = d.location
        _quantity.value = d.quantity
        _imagePath.value = d.imagePath
        _cameraImagePath.value = d.imagePath
        _tempImageUri.value = null
        _pendingDraft.value = null
    }

    /** 丢弃草稿 */
    fun discardDraft() {
        DraftManager.clearDraft(application)
        _pendingDraft.value = null
    }

    // ---- 表单更新 ----

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

    /** 相册选图 */
    fun onImageSelected(uri: Uri?) {
        _tempImageUri.value = uri
        _cameraImagePath.value = null
        autoSaveDraft()
    }

    /** 相机拍照完成 */
    fun onPhotoTaken(path: String?) {
        _cameraImagePath.value = path
        _tempImageUri.value = null
        autoSaveDraft()
    }

    /** 清除图片 */
    fun clearImage() {
        _cameraImagePath.value?.let { path ->
            if (_imagePath.value != path) ImageUtils.deleteImageFile(path)
        }
        _tempImageUri.value = null
        _cameraImagePath.value = null
        _imagePath.value = null
        autoSaveDraft()
    }

    /**
     * 保存物品
     */
    fun saveItem() {
        if (_name.value.isBlank()) {
            _errorMessage.value = "请输入物品名称"
            return
        }
        if (_category.value.isBlank()) {
            _errorMessage.value = "请输入物品分类"
            return
        }

        viewModelScope.launch {
            var finalImagePath = _imagePath.value

            // 相机图片（已在内部存储）
            _cameraImagePath.value?.let { cameraPath ->
                if (isEditing && _imagePath.value != null && _imagePath.value != cameraPath) {
                    ImageUtils.deleteImageFile(_imagePath.value)
                }
                finalImagePath = cameraPath
            }

            // 相册图片：复制到内部存储
            _tempImageUri.value?.let { uri ->
                val savedPath = ImageUtils.copyImageToInternalStorage(application, uri)
                if (savedPath != null) {
                    if (isEditing && _imagePath.value != null) {
                        ImageUtils.deleteImageFile(_imagePath.value)
                    }
                    finalImagePath = savedPath
                }
            }

            val qty = _quantity.value.toIntOrNull() ?: 1
            val now = System.currentTimeMillis()

            if (isEditing && editingItemId != null) {
                val existing = repository.getItemById(editingItemId)
                val updatedItem = existing?.copy(
                    name = _name.value.trim(),
                    category = _category.value.trim(),
                    description = _description.value.trim(),
                    location = _location.value.trim(),
                    quantity = qty,
                    imagePath = finalImagePath,
                    updatedAt = now
                )
                if (updatedItem != null) repository.updateItem(updatedItem)
            } else {
                val newItem = ItemEntity(
                    name = _name.value.trim(),
                    category = _category.value.trim(),
                    description = _description.value.trim(),
                    location = _location.value.trim(),
                    quantity = qty,
                    imagePath = finalImagePath,
                    createdAt = now,
                    updatedAt = now
                )
                repository.insertItem(newItem)
            }

            // 保存成功，清除草稿
            DraftManager.clearDraft(application)
            SelectedItemHolder.clear()
            _saveCompleted.value = true
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

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
