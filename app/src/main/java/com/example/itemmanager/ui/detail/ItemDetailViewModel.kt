package com.example.itemmanager.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.itemmanager.data.local.ItemEntity
import com.example.itemmanager.data.repository.ItemRepository
import com.example.itemmanager.util.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 物品详情页 ViewModel
 * 根据导航参数中的物品 ID 加载物品详情
 */
class ItemDetailViewModel(
    private val repository: ItemRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 从导航参数中获取物品 ID
    private val itemId: Long = savedStateHandle.get<Long>("itemId") ?: -1L

    private val _item = MutableStateFlow<ItemEntity?>(null)
    val item: StateFlow<ItemEntity?> = _item

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadItem()
    }

    /** 加载物品详情 */
    fun loadItem() {
        viewModelScope.launch {
            _isLoading.value = true
            _item.value = repository.getItemById(itemId)
            _isLoading.value = false
        }
    }

    /** 删除当前物品及其图片 */
    fun deleteCurrentItem(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _item.value?.let { item ->
                repository.deleteItem(item)
                ImageUtils.deleteImageFile(item.imagePath)
                onDeleted()
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: ItemRepository,
            savedStateHandle: SavedStateHandle
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ItemDetailViewModel(repository, savedStateHandle) as T
                }
            }
    }
}
