package com.example.itemmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * App 本地数据库
 * 使用 Room 持久化库，单例模式确保全局只有一个数据库连接
 * v4：新增品牌 brand、隐私标记 isPrivate 字段
 */
@Database(entities = [ItemEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ItemDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: ItemDatabase? = null

        /**
         * 获取数据库单例
         * 使用双重检查锁定（DCL）保证线程安全；
         * 开发阶段结构变更时重建数据库（清空旧数据）。
         */
        fun getDatabase(context: Context): ItemDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ItemDatabase::class.java,
                    "item_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
