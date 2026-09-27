package com.example.itemmanager.ui.edit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.itemmanager.data.local.ItemEntity
import com.example.itemmanager.data.repository.ItemRepository
import com.example.itemmanager.util.ImageUtils
import com.example.itemmanager.util.SelectedItemHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 物品添加/编辑页 ViewModel
 * 从全局 SelectedItemHolder 读取编辑模式的物品 ID（添加模式为 null）
 */
class ItemEditViewModel(
    private val application: Application,
    private val repository: ItemRepository
) : ViewModel() {

    // 从全局持有者获取编辑模式的物品 ID
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

    // 图片路径（已保存到内部存储的路径）
    private val _imagePath = MutableStateFlow<String?>(null)
    val imagePath: StateFlow<String?> = _imagePath

    // 临时选中的图片 Uri（尚未复制到内部存储）
    private val _tempImageUri = MutableStateFlow<Uri?>(null)
    val tempImageUri: StateFlow<Uri?> = _tempImageUri

    // 是否为编辑模式
    val isEditing: Boolean = editingItemId != null

    // 保存完成回调标志
    private val _saveCompleted = MutableStateFlow(false)
    val saveCompleted: StateFlow<Boolean> = _saveCompleted

    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        // 编辑模式下加载已有数据
        if (editingItemId != null) {
            loadExistingItem(editingItemId)
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
            }
        }
    }

    // ---- 表单更新函数 ----

    fun onNameChanged(value: String) { _name.value = value }
    fun onCategoryChanged(value: String) { _category.value = value }
    fun onDescriptionChanged(value: String) { _description.value = value }
    fun onLocationChanged(value: String) { _location.value = value }
    fun onQuantityChanged(value: String) {
        // 只允许数字输入
        if (value.all { it.isDigit() }) {
            _quantity.value = value
        }
    }

    /** 用户从相册选择图片后，记录临时 Uri */
    fun onImageSelected(uri: Uri?) {
        _tempImageUri.value = uri
    }

    /** 清除已选图片 */
    fun clearImage() {
        _tempImageUri.value = null
        _imagePath.value = null
    }

    /**
     * 保存物品
     * 先校验必填项，再将图片复制到内部存储，最后写入数据库
     */
    fun saveItem() {
        // 校验名称
        if (_name.value.isBlank()) {
            _errorMessage.value = "请输入物品名称"
            return
        }
        // 校验分类
        if (_category.value.isBlank()) {
            _errorMessage.value = "请输入物品分类"
            return
        }

        viewModelScope.launch {
            // 处理图片：如果有新选择的图片，复制到内部存储
            var finalImagePath = _imagePath.value
            _tempImageUri.value?.let { uri ->
                val savedPath = ImageUtils.copyImageToInternalStorage(application, uri)
                if (savedPath != null) {
                    // 如果是编辑且有旧图片，删除旧图片
                    if (isEditing && _imagePath.value != null) {
                        ImageUtils.deleteImageFile(_imagePath.value)
                    }
                    finalImagePath = savedPath
                }
            }

            val qty = _quantity.value.toIntOrNull() ?: 1
            val now = System.currentTimeMillis()

            if (isEditing && editingItemId != null) {
                // 更新已有物品
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
                if (updatedItem != null) {
                    repository.updateItem(updatedItem)
                }
            } else {
                // 插入新物品
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
