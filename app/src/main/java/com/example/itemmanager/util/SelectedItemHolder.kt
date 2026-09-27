package com.example.itemmanager.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 全局物品选择持有者
 * 用于在页面间传递当前选中的物品 ID，避免 Navigation Compose 参数传递丢失的问题
 */
object SelectedItemHolder {
    private val _selectedItemId = MutableStateFlow<Long?>(null)
    val selectedItemId: StateFlow<Long?> = _selectedItemId

    /** 设置当前选中的物品 ID */
    fun select(itemId: Long) {
        _selectedItemId.value = itemId
    }

    /** 获取并清除当前选中的物品 ID（一次性使用） */
    fun consume(): Long? {
        val id = _selectedItemId.value
        return id
    }

    /** 清除选中状态 */
    fun clear() {
        _selectedItemId.value = null
    }
}
