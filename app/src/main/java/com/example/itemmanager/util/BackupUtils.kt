package com.example.itemmanager.util

import android.content.Context
import android.net.Uri
import com.example.itemmanager.data.local.ItemEntity
import com.example.itemmanager.data.repository.ItemRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 备份工具类
 * 把整个物品库导出为一个 zip 备份文件（items.json 数据 + images/ 图片），
 * 用户可将该文件上传到任意网盘/云端或发送给他人；也支持从备份 zip 导入恢复。
 * 这样即使卸载应用或更换手机，数据也能找回。
 */
object BackupUtils {

    private const val JSON_NAME = "items.json"
    private const val IMAGE_DIR = "images/"

    /**
     * 导出全部物品为备份 zip
     * @return 生成的备份文件，失败返回 null
     */
    suspend fun exportBackup(context: Context, repository: ItemRepository): File? {
        return try {
            // 一次性读取当前全部物品
            val items: List<ItemEntity> = repository.getAllItems().first()

            val backupDir = File(context.filesDir, "backup")
            if (!backupDir.exists()) backupDir.mkdirs()
            val timeStamp = java.text.SimpleDateFormat(
                "yyyyMMdd_HHmmss", java.util.Locale.getDefault()
            ).format(java.util.Date())
            val zipFile = File(backupDir, "item_backup_$timeStamp.zip")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // 1. 写入图片，并记录每个物品对应的图片在 zip 内的文件名
                val idToImageName = HashMap<Long, String>()
                items.forEachIndexed { index, item ->
                    item.imagePath?.let { path ->
                        val src = File(path)
                        if (src.exists()) {
                            // 用 序号+原文件名 保证唯一
                            val imageName = "img_$index" + "_" + src.name
                            zos.putNextEntry(ZipEntry(IMAGE_DIR + imageName))
                            src.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            idToImageName[item.id] = imageName
                        }
                    }
                }

                // 2. 写入 items.json
                val jsonArray = JSONArray()
                items.forEach { item ->
                    val obj = JSONObject()
                    obj.put("name", item.name)
                    obj.put("category", item.category)
                    obj.put("description", item.description)
                    obj.put("location", item.location)
                    obj.put("quantity", item.quantity)
                    obj.put("createdAt", item.createdAt)
                    obj.put("updatedAt", item.updatedAt)
                    idToImageName[item.id]?.let { obj.put("imageName", it) }
                    jsonArray.put(obj)
                }
                zos.putNextEntry(ZipEntry(JSON_NAME))
                zos.write(jsonArray.toString().toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从备份 zip 导入物品
     * @param zipUri 用户选择的备份文件 Uri
     * @return 成功导入的物品数量，失败返回 -1
     */
    suspend fun importBackup(
        context: Context,
        repository: ItemRepository,
        zipUri: Uri
    ): Int {
        return try {
            // 先把 zip 中图片解压到临时映射：imageName -> 本地新路径
            val imageNameToPath = HashMap<String, String>()
            var jsonText: String? = null

            context.contentResolver.openInputStream(zipUri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        when {
                            // 图片文件
                            entry.name.startsWith(IMAGE_DIR) && !entry.isDirectory -> {
                                val imageName = entry.name.removePrefix(IMAGE_DIR)
                                val imagesDir = File(context.filesDir, "images")
                                if (!imagesDir.exists()) imagesDir.mkdirs()
                                // 加时间戳避免重名
                                val ts = System.currentTimeMillis()
                                val dest = File(imagesDir, "restored_${ts}_$imageName")
                                dest.outputStream().use { zis.copyTo(it) }
                                imageNameToPath[imageName] = dest.absolutePath
                            }
                            // 数据文件
                            entry.name == JSON_NAME -> {
                                jsonText = zis.bufferedReader(Charsets.UTF_8).readText()
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            if (jsonText == null) return -1

            // 解析并插入（作为新物品，id 自动生成，避免与现有数据冲突）
            val jsonArray = JSONArray(jsonText)
            var count = 0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val imageName = if (obj.has("imageName")) obj.getString("imageName") else null
                val now = System.currentTimeMillis()
                val item = ItemEntity(
                    name = obj.getString("name"),
                    category = obj.optString("category", "其他"),
                    description = obj.optString("description", ""),
                    location = obj.optString("location", ""),
                    quantity = obj.optInt("quantity", 1),
                    imagePath = imageName?.let { imageNameToPath[it] },
                    createdAt = obj.optLong("createdAt", now),
                    updatedAt = now
                )
                repository.insertItem(item)
                count++
            }
            count
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }
}
