package com.example.itemmanager.ui.dashboard

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.itemmanager.R
import com.example.itemmanager.data.local.ItemEntity
import com.example.itemmanager.data.repository.ItemRepository
import com.example.itemmanager.util.BackupUtils
import com.example.itemmanager.util.ShareUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 副页（分类 / 统计 / 我的）共享的 ViewModel
 * 持有全部物品数据，并封装备份导出、导入恢复逻辑。
 * 绑定到 Activity 级别，使三个 Tab 共享同一份数据。
 */
class DashboardViewModel(
    private val application: Application,
    private val repository: ItemRepository
) : ViewModel() {

    // 全部物品
    val items: StateFlow<List<ItemEntity>> = repository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 已有分类
    val categories: StateFlow<List<String>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 借出中的物品数量
    val borrowedCount: StateFlow<Int> = repository.getBorrowedItems()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // 操作提示
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun consumeMessage() { _message.value = null }

    /** 导出备份并分享（上传云端） */
    fun exportAndShare() {
        viewModelScope.launch {
            val file = BackupUtils.exportBackup(application, repository)
            if (file != null) {
                _message.value = application.getString(R.string.backup_ready)
                ShareUtils.shareFile(application, file)
            } else {
                _message.value = application.getString(R.string.export_failed)
            }
        }
    }

    /** 从备份文件导入恢复 */
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

    companion object {
        fun provideFactory(
            application: Application,
            repository: ItemRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(application, repository) as T
                }
            }
    }
}
