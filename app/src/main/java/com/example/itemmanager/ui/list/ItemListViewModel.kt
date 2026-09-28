package com.example.itemmanager.ui.list

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.itemmanager.R
import com.example.itemmanager.data.local.ItemEntity
import com.example.itemmanager.data.repository.ItemRepository
import com.example.itemmanager.util.BackupUtils
import com.example.itemmanager.util.ImageUtils
import com.example.itemmanager.util.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 物品列表页 ViewModel
 * 管理搜索关键词、分类筛选、物品列表，以及备份导出/导入恢复
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ItemListViewModel(
    private val application: Application,
    private val repository: ItemRepository
) : ViewModel() {

    // 搜索关键词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // 当前选中的分类筛选，null 表示全部
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    // 排序方式
    enum class SortMode { DATE_DESC, DATE_ASC, NAME_ASC, EXPIRY_ASC }
    private val _sortMode = MutableStateFlow(SortMode.DATE_DESC)
    val sortMode: StateFlow<SortMode> = _sortMode

    // 所有可用分类（从数据库动态获取）
    val categories: StateFlow<List<String>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 根据搜索和筛选条件动态切换数据源（支持搜索与分类组合）
    val items: StateFlow<List<ItemEntity>> = _searchQuery
        .combineWith(_selectedCategory) { query, category ->
            when {
                query.isNotBlank() && category != null ->
                    repository.searchItemsInCategory(query, category)
                query.isNotBlank() -> repository.searchItems(query)
                category != null -> repository.getItemsByCategory(category)
                else -> repository.getAllItems()
            }
        }
        .flatMapLatest { it }
        .combine(_sortMode) { list, mode -> sortList(list, mode) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 一次性操作提示消息（导出/导入结果）
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun consumeMessage() { _message.value = null }

    /** 更新搜索关键词 */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /** 切换分类筛选 */
    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    /** 切换排序方式 */
    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    /** 删除物品（同时删除关联图片） */
    fun deleteItem(item: ItemEntity) {
        viewModelScope.launch {
            repository.deleteItem(item)
            withContext(Dispatchers.IO) {
                item.imagePaths.forEach { ImageUtils.deleteImageFile(it) }
            }
        }
    }

    /**
     * 导出备份并调起系统分享（可上传到云盘/发送他人）
     */
    fun exportAndShare() {
        viewModelScope.launch {
            val file = BackupUtils.exportBackup(application, repository)
            if (file != null) {
                _message.value = application.getString(R.string.backup_ready)
                ShareUtils.shareFile(
                    application, file, application.getString(R.string.share_backup_title)
                )
            } else {
                _message.value = application.getString(R.string.export_failed)
            }
        }
    }

    /**
     * 从用户选择的备份文件导入恢复
     */
    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val count = BackupUtils.importBackup(application, repository, uri)
            _message.value = when {
                count > 0 -> application.getString(R.string.restore_success, count)
                count == 0 -> application.getString(R.string.restore_empty)
                else -> application.getString(R.string.import_failed)
            }
        }
    }

    /** 按排序模式对列表排序 */
    private fun sortList(list: List<ItemEntity>, mode: SortMode): List<ItemEntity> = when (mode) {
        SortMode.DATE_DESC -> list.sortedByDescending { it.createdAt }
        SortMode.DATE_ASC -> list.sortedBy { it.createdAt }
        SortMode.NAME_ASC -> list.sortedBy { it.name }
        SortMode.EXPIRY_ASC -> list.sortedBy { it.expiryDate ?: Long.MAX_VALUE }
    }

    companion object {
        /** 提供 Factory，注入 Application 和 Repository */
        fun provideFactory(
            application: Application,
            repository: ItemRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ItemListViewModel(application, repository) as T
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
