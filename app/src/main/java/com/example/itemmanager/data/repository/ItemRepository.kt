package com.example.itemmanager.data.repository

import com.example.itemmanager.data.local.ItemDao
import com.example.itemmanager.data.local.ItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * 物品仓库
 * 作为数据层与 UI 层之间的中介，封装数据源操作
 * 目前仅使用本地 Room 数据库，未来可扩展云端同步
 */
class ItemRepository(private val itemDao: ItemDao) {

    /** 获取所有物品（含隐私，用于备份/提醒） */
    fun getAllItems(): Flow<List<ItemEntity>> = itemDao.getAllItems()

    /** 获取非隐私物品（主列表） */
    fun getPublicItems(): Flow<List<ItemEntity>> = itemDao.getPublicItems()

    /** 获取加密箱隐私物品 */
    fun getPrivateItems(): Flow<List<ItemEntity>> = itemDao.getPrivateItems()

    /** 按分类筛选（非隐私） */
    fun getItemsByCategory(category: String): Flow<List<ItemEntity>> =
        itemDao.getItemsByCategory(category)

    /** 搜索物品（名称/描述/品牌，非隐私） */
    fun searchItems(query: String): Flow<List<ItemEntity>> =
        itemDao.searchItems(query)

    /** 在指定分类下搜索物品（非隐私） */
    fun searchItemsInCategory(query: String, category: String): Flow<List<ItemEntity>> =
        itemDao.searchItemsInCategory(query, category)

    /** 获取所有分类（非隐私） */
    fun getAllCategories(): Flow<List<String>> = itemDao.getAllCategories()

    /** 根据 ID 获取单个物品 */
    suspend fun getItemById(id: Long): ItemEntity? = itemDao.getItemById(id)

    /** 插入物品，返回新 ID */
    suspend fun insertItem(item: ItemEntity): Long = itemDao.insert(item)

    /** 更新物品 */
    suspend fun updateItem(item: ItemEntity) = itemDao.update(item)

    /** 删除物品 */
    suspend fun deleteItem(item: ItemEntity) = itemDao.delete(item)

    /** 获取物品总数 */
    suspend fun getItemCount(): Int = itemDao.getItemCount()

    /** 获取借出中的物品 */
    fun getBorrowedItems(): Flow<List<ItemEntity>> = itemDao.getBorrowedItems()
}
