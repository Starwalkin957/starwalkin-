package com.example.itemmanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 物品数据访问对象
 * 定义对 items 表的所有 CRUD 操作，返回 Flow 以实现数据变化自动通知 UI
 */
@Dao
interface ItemDao {

    /** 插入新物品，返回生成的主键 ID */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity): Long

    /** 更新已有物品 */
    @Update
    suspend fun update(item: ItemEntity)

    /** 删除指定物品 */
    @Delete
    suspend fun delete(item: ItemEntity)

    /** 根据 ID 查询单个物品 */
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): ItemEntity?

    /** 获取所有物品，按更新时间倒序排列 */
    @Query("SELECT * FROM items ORDER BY updatedAt DESC")
    fun getAllItems(): Flow<List<ItemEntity>>

    /** 按分类筛选物品 */
    @Query("SELECT * FROM items WHERE category = :category ORDER BY updatedAt DESC")
    fun getItemsByCategory(category: String): Flow<List<ItemEntity>>

    /** 按名称或描述模糊搜索 */
    @Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchItems(query: String): Flow<List<ItemEntity>>

    /** 在指定分类下按名称或描述模糊搜索（搜索 + 分类组合） */
    @Query("SELECT * FROM items WHERE (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') AND category = :category ORDER BY updatedAt DESC")
    fun searchItemsInCategory(query: String, category: String): Flow<List<ItemEntity>>

    /** 获取所有不重复的分类名称，用于筛选下拉 */
    @Query("SELECT DISTINCT category FROM items ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    /** 获取物品总数 */
    @Query("SELECT COUNT(*) FROM items")
    suspend fun getItemCount(): Int

    /** 获取借出中的物品（按预计归还日期升序） */
    @Query("SELECT * FROM items WHERE borrower IS NOT NULL AND borrower != '' ORDER BY expectedReturnDate ASC")
    fun getBorrowedItems(): Flow<List<ItemEntity>>

    /** 物品总价值（price 非空的求和，全为空时返回 null） */
    @Query("SELECT SUM(price) FROM items")
    fun getTotalValue(): Flow<Double?>
}
