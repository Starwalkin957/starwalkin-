package com.example.itemmanager.data.local

import androidx.room.TypeConverter

/**
 * Room 类型转换器
 * 把物品的多张图片路径 List<String> 与数据库中的字符串互相转换。
 * 内部存储文件路径不含 "|"，故用它作分隔符。
 */
class Converters {

    @TypeConverter
    fun fromImagePaths(paths: List<String>?): String =
        paths?.filter { it.isNotBlank() }?.joinToString(separator = "|") ?: ""

    @TypeConverter
    fun toImagePaths(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList()
        else value.split("|").filter { it.isNotBlank() }
}
