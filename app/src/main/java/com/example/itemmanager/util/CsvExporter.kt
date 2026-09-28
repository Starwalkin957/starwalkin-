package com.example.itemmanager.util

import android.content.Context
import com.example.itemmanager.data.local.ItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CSV 导出工具
 * 把物品清单导出为 UTF-8（带 BOM）CSV 文件，可用 Excel/WPS 直接打开。
 * 字段含逗号/引号/换行时自动用双引号包裹并转义。
 */
object CsvExporter {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 导出全部物品为 CSV 文件，失败返回 null */
    suspend fun export(context: Context, items: List<ItemEntity>): File? {
        return withContext(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "exports")
                if (!dir.exists()) dir.mkdirs()
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val file = File(dir, "items_$stamp.csv")

                file.bufferedWriter(Charsets.UTF_8).use { w ->
                    // UTF-8 BOM，让 Excel 正确识别中文
                    w.write('\uFEFF'.toString())
                    w.write(
                        "名称,分类,描述,位置,数量,价格,购买日期,保质期,保修期," +
                            "借出人,借出日期,预计归还,创建时间\n"
                    )
                    items.forEach { item ->
                        val row = listOf(
                            item.name,
                            item.category,
                            item.description,
                            item.location,
                            item.quantity.toString(),
                            item.price?.toString() ?: "",
                            item.purchaseDate?.let { dateFmt.format(Date(it)) } ?: "",
                            item.expiryDate?.let { dateFmt.format(Date(it)) } ?: "",
                            item.warrantyDate?.let { dateFmt.format(Date(it)) } ?: "",
                            item.borrower ?: "",
                            item.borrowDate?.let { dateFmt.format(Date(it)) } ?: "",
                            item.expectedReturnDate?.let { dateFmt.format(Date(it)) } ?: "",
                            dateFmt.format(Date(item.createdAt))
                        ).joinToString(",") { escape(it) }
                        w.write(row + "\n")
                    }
                }
                file
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /** CSV 字段转义：含逗号/引号/换行时用双引号包裹 */
    private fun escape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }
}
