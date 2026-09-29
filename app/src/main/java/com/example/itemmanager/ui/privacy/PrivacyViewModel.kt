package com.example.itemmanager.ui.privacy

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.itemmanager.R
import com.example.itemmanager.data.local.ItemEntity
import com.example.itemmanager.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 加密箱 ViewModel
 * 持有隐私物品列表，支持把物品移出加密箱
 */
class PrivacyViewModel(
    private val application: Application,
    private val repository: ItemRepository
) : ViewModel() {

    /** 加密箱内的隐私物品 */
    val items: StateFlow<List<ItemEntity>> = repository.getPrivateItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun consumeMessage() { _message.value = null }

    /** 把物品移出加密箱（变为普通物品） */
    fun removeFromVault(item: ItemEntity) {
        viewModelScope.launch {
            repository.updateItem(
                item.copy(
                    isPrivate = false,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _message.value = application.getString(R.string.removed_from_vault)
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
                    return PrivacyViewModel(application, repository) as T
                }
            }
    }
}
