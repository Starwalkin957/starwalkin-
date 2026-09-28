package com.example.itemmanager.ui.edit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.itemmanager.R
import com.example.itemmanager.data.local.ItemEntity
import com.example.itemmanager.data.repository.ItemRepository
import com.example.itemmanager.util.DateUtils
import com.example.itemmanager.util.DraftManager
import com.example.itemmanager.util.ImageUtils
import com.example.itemmanager.util.ItemDraft
import com.example.itemmanager.util.SelectedItemHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 物品添加/编辑页 ViewModel
 * - 支持为同一物品添加多张照片（相册/相机），可单独删除
 * - 支持购买日期、保质期、保修期，日期可手动输入或日历选择
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

    // ---- 多张图片 ----
    private val _imagePaths = MutableStateFlow<List<String>>(emptyList())
    val imagePaths: StateFlow<List<String>> = _imagePaths

    private var originalPaths: List<String> = emptyList()

    // ---- 日期：时间戳 + 输入框文本（两者同步）----
    private val _purchaseDate = MutableStateFlow<Long?>(null)
    val purchaseDate: StateFlow<Long?> = _purchaseDate
    private val _purchaseDateText = MutableStateFlow("")
    val purchaseDateText: StateFlow<String> = _purchaseDateText

    private val _expiryDate = MutableStateFlow<Long?>(null)
    val expiryDate: StateFlow<Long?> = _expiryDate
    private val _expiryDateText = MutableStateFlow("")
    val expiryDateText: StateFlow<String> = _expiryDateText

    private val _warrantyDate = MutableStateFlow<Long?>(null)
    val warrantyDate: StateFlow<Long?> = _warrantyDate
    private val _warrantyDateText = MutableStateFlow("")
    val warrantyDateText: StateFlow<String> = _warrantyDateText

    // ---- 借出信息 ----
    private val _borrower = MutableStateFlow("")
    val borrower: StateFlow<String> = _borrower

    private val _expectedReturnDate = MutableStateFlow<Long?>(null)
    val expectedReturnDate: StateFlow<Long?> = _expectedReturnDate
    private val _expectedReturnDateText = MutableStateFlow("")
    val expectedReturnDateText: StateFlow<String> = _expectedReturnDateText

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
                setPurchaseDate(item.purchaseDate)
                setExpiryDate(item.expiryDate)
                setWarrantyDate(item.warrantyDate)
                _borrower.value = item.borrower ?: ""
                setExpectedReturnDate(item.expectedReturnDate)
            }
        }
    }

    // ---- 多图管理 ----

    fun addImageFromUri(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                ImageUtils.copyImageToInternalStorage(application, uri)
            }
            if (savedPath != null) {
                _imagePaths.update { it + savedPath }
                autoSaveDraft()
            } else {
                _errorMessage.value = application.getString(R.string.photo_failed)
            }
        }
    }

    fun addPhotoPath(path: String?) {
        if (path == null) return
        _imagePaths.update { if (path in it) it else it + path }
        autoSaveDraft()
    }

    fun removeImage(path: String) {
        _imagePaths.update { it - path }
        if (path !in originalPaths) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { ImageUtils.deleteImageFile(path) }
            }
        }
        autoSaveDraft()
    }

    // ---- 日期：日历选择/清除（同步文本）----

    fun setPurchaseDate(ts: Long?) {
        _purchaseDate.value = ts
        _purchaseDateText.value = DateUtils.formatDate(ts)
        autoSaveDraft()
    }

    fun setExpiryDate(ts: Long?) {
        _expiryDate.value = ts
        _expiryDateText.value = DateUtils.formatDate(ts)
        autoSaveDraft()
    }

    fun setWarrantyDate(ts: Long?) {
        _warrantyDate.value = ts
        _warrantyDateText.value = DateUtils.formatDate(ts)
        autoSaveDraft()
    }

    // ---- 日期：手动文本输入（实时尝试解析）----

    fun onPurchaseDateTextChange(text: String) {
        _purchaseDateText.value = text
        _purchaseDate.value = DateUtils.parseDate(text)
        autoSaveDraft()
    }

    fun onExpiryDateTextChange(text: String) {
        _expiryDateText.value = text
        _expiryDate.value = DateUtils.parseDate(text)
        autoSaveDraft()
    }

    fun onWarrantyDateTextChange(text: String) {
        _warrantyDateText.value = text
        _warrantyDate.value = DateUtils.parseDate(text)
        autoSaveDraft()
    }

    // ---- 借出 ----

    fun onBorrowerChanged(value: String) {
        _borrower.value = value
        autoSaveDraft()
    }

    fun setExpectedReturnDate(ts: Long?) {
        _expectedReturnDate.value = ts
        _expectedReturnDateText.value = DateUtils.formatDate(ts)
        autoSaveDraft()
    }

    fun onExpectedReturnDateTextChange(text: String) {
        _expectedReturnDateText.value = text
        _expectedReturnDate.value = DateUtils.parseDate(text)
        autoSaveDraft()
    }

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
            warrantyDate = _warrantyDate.value,
            borrower = _borrower.value,
            expectedReturnDate = _expectedReturnDate.value
        )
        DraftManager.saveDraft(application, draft)
    }

    fun restoreDraft() {
        val d = _pendingDraft.value ?: return
        _name.value = d.name
        _category.value = d.category
        _description.value = d.description
        _location.value = d.location
        _quantity.value = d.quantity
        _imagePaths.value = d.imagePaths
        setPurchaseDate(d.purchaseDate)
        setExpiryDate(d.expiryDate)
        setWarrantyDate(d.warrantyDate)
        _borrower.value = d.borrower
        setExpectedReturnDate(d.expectedReturnDate)
        _pendingDraft.value = null
    }

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
        if (_name.value.isBlank()) {
            _errorMessage.value = application.getString(R.string.enter_name); return
        }
        if (_category.value.isBlank()) {
            _errorMessage.value = application.getString(R.string.enter_category); return
        }

        viewModelScope.launch {
            val qty = _quantity.value.toIntOrNull() ?: 1
            val borrowerVal = _borrower.value.trim().ifBlank { null }
            val now = System.currentTimeMillis()

            if (isEditing && editingItemId != null) {
                withContext(Dispatchers.IO) {
                    (originalPaths - _imagePaths.value.toSet()).forEach {
                        ImageUtils.deleteImageFile(it)
                    }
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
                    borrower = borrowerVal,
                    borrowDate = if (borrowerVal != null) existing?.borrowDate ?: now else null,
                    expectedReturnDate = if (borrowerVal != null) _expectedReturnDate.value else null,
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
                    borrower = borrowerVal,
                    borrowDate = if (borrowerVal != null) now else null,
                    expectedReturnDate = if (borrowerVal != null) _expectedReturnDate.value else null,
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
