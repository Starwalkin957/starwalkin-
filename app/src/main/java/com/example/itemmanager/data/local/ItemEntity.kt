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

    /** 物品的多张图片本地路径（支持多张照片） */
    val imagePaths: List<String> = emptyList(),

    /** 存放位置 */
    val location: String = "",

    /** 数量 */
    val quantity: Int = 1,

    /** 购买日期（毫秒），可为空 */
    val purchaseDate: Long? = null,

    /** 保质期/有效期截止（毫秒），可为空 */
    val expiryDate: Long? = null,

    /** 保修期截止（毫秒），可为空 */
    val warrantyDate: Long? = null,

    /** 物品价格/价值（元），可为空 */
    val price: Double? = null,

    /** 当前借出人（为空表示未借出） */
    val borrower: String? = null,

    /** 借出日期（毫秒），可为空 */
    val borrowDate: Long? = null,

    /** 预计归还日期（毫秒），可为空 */
    val expectedReturnDate: Long? = null,

    /** 创建时间戳（毫秒） */
    val createdAt: Long = System.currentTimeMillis(),

    /** 最后更新时间戳（毫秒） */
    val updatedAt: Long = System.currentTimeMillis()
)
