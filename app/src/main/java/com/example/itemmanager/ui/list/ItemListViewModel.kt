package com.example.itemmanager.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.itemmanager.data.local.ItemEntity
import com.example.itemmanager.data.repository.ItemRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 物品列表页 ViewModel
 * 管理搜索关键词、分类筛选，以及物品列表数据
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ItemListViewModel(
    private val repository: ItemRepository
) : ViewModel() {

    // 搜索关键词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // 当前选中的分类筛选，null 表示全部
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    // 所有可用分类（从数据库动态获取）
    val categories: StateFlow<List<String>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 根据搜索和筛选条件动态切换数据源
    val items: StateFlow<List<ItemEntity>> = _searchQuery
        .combineWith(_selectedCategory) { query, category ->
            when {
                query.isNotBlank() -> repository.searchItems(query)
                category != null -> repository.getItemsByCategory(category)
                else -> repository.getAllItems()
            }
        }
        .flatMapLatest { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 更新搜索关键词 */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /** 切换分类筛选 */
    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    /** 删除物品（同时删除关联图片） */
    fun deleteItem(item: ItemEntity) {
        viewModelScope.launch {
            repository.deleteItem(item)
            com.example.itemmanager.util.ImageUtils.deleteImageFile(item.imagePath)
        }
    }

    companion object {
        /** 提供 Factory 以便注入 Repository */
        fun provideFactory(repository: ItemRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ItemListViewModel(repository) as T
                }
            }
    }
}

/**
 * 合并两个 StateFlow 的工具函数
 */
private fun <T1, T2, R> MutableStateFlow<T1>.combineWith(
    other: MutableStateFlow<T2>,
    transform: (T1, T2) -> R
): kotlinx.coroutines.flow.Flow<R> =
    kotlinx.coroutines.flow.combine(this, other, transform)
