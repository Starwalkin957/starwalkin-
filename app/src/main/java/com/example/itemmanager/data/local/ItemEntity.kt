package com.example.itemmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 物品数据实体
 * 对应数据库中的 items 表，存储每件物品的完整信息
 */
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 物品名称 */
    val name: String,

    /** 物品分类（如：电子产品、衣物、书籍、食品等） */
    val category: String,

    /** 物品描述/备注 */
    val description: String = "",

    /** 物品图片的本地文件路径，可为空 */
    val imagePath: String? = null,

    /** 存放位置 */
    val location: String = "",

    /** 数量 */
    val quantity: Int = 1,

    /** 创建时间戳（毫秒） */
    val createdAt: Long = System.currentTimeMillis(),

    /** 最后更新时间戳（毫秒） */
    val updatedAt: Long = System.currentTimeMillis()
)
