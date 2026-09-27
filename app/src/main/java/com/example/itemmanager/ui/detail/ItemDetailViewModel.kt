package com.example.itemmanager.ui.detail

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
 * 物品详情页 ViewModel
 * 从全局 SelectedItemHolder 读取物品 ID，彻底避免导航参数丢失问题
 */
class ItemDetailViewModel(
    private val repository: ItemRepository
) : ViewModel() {

    private val _item = MutableStateFlow<ItemEntity?>(null)
    val item: StateFlow<ItemEntity?> = _item

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 从全局持有者获取当前选中的物品 ID
    private val currentItemId: Long? = SelectedItemHolder.consume()

    init {
        loadItem()
    }

    /** 加载物品详情 */
    fun loadItem() {
        viewModelScope.launch {
            _isLoading.value = true
            val id = currentItemId
            _item.value = if (id != null && id > 0) {
                repository.getItemById(id)
            } else {
                null
            }
            _isLoading.value = false
        }
    }

    /** 删除当前物品及其图片 */
    fun deleteCurrentItem(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _item.value?.let { item ->
                repository.deleteItem(item)
                ImageUtils.deleteImageFile(item.imagePath)
                SelectedItemHolder.clear()
                onDeleted()
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: ItemRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ItemDetailViewModel(repository) as T
                }
            }
    }
}
