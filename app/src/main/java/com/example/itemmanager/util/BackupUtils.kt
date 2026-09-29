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
 * 把整个物品库导出为一个 zip 备份文件（items.json 数据 + images/ 多张图片），
 * 用户可上传到任意网盘/云端或发送给他人；也支持从备份 zip 导入恢复。
 */
object BackupUtils {

    private const val JSON_NAME = "items.json"
    private const val PRIVACY_NAME = "privacy.json"
    private const val IMAGE_DIR = "images/"

    /** 导出全部物品为备份 zip，失败返回 null */
    suspend fun exportBackup(context: Context, repository: ItemRepository): File? {
        return try {
            val items: List<ItemEntity> = repository.getAllItems().first()

            val backupDir = File(context.filesDir, "backup")
            if (!backupDir.exists()) backupDir.mkdirs()
            val timeStamp = java.text.SimpleDateFormat(
                "yyyyMMdd_HHmmss", java.util.Locale.getDefault()
            ).format(java.util.Date())
            val zipFile = File(backupDir, "item_backup_$timeStamp.zip")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // 1. 写入所有图片，记录物品 ID -> zip 内图片名列表
                val idToImageNames = HashMap<Long, MutableList<String>>()
                items.forEachIndexed { index, item ->
                    item.imagePaths.forEachIndexed { imgIndex, path ->
                        val src = File(path)
                        if (src.exists()) {
                            val imageName = "img_${index}_${imgIndex}_" + src.name
                            zos.putNextEntry(ZipEntry(IMAGE_DIR + imageName))
                            src.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            idToImageNames.getOrPut(item.id) { mutableListOf() }.add(imageName)
                        }
                    }
                }

                // 2. 写入 items.json
                val jsonArray = JSONArray()
                items.forEach { item ->
                    val obj = JSONObject()
                    obj.put("name", item.name)
                    obj.put("category", item.category)
                    obj.put("brand", item.brand)
                    obj.put("description", item.description)
                    obj.put("isPrivate", item.isPrivate)
                    obj.put("location", item.location)
                    obj.put("quantity", item.quantity)
                    item.purchaseDate?.let { obj.put("purchaseDate", it) }
                    item.expiryDate?.let { obj.put("expiryDate", it) }
                    item.warrantyDate?.let { obj.put("warrantyDate", it) }
                    item.borrower?.let { obj.put("borrower", it) }
                    item.borrowDate?.let { obj.put("borrowDate", it) }
                    item.expectedReturnDate?.let { obj.put("expectedReturnDate", it) }
                    obj.put("createdAt", item.createdAt)
                    obj.put("updatedAt", item.updatedAt)
                    idToImageNames[item.id]?.let { names ->
                        obj.put("imageNames", JSONArray(names))
                    }
                    jsonArray.put(obj)
                }
                zos.putNextEntry(ZipEntry(JSON_NAME))
                zos.write(jsonArray.toString().toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 3. 写入加密箱密码配置 privacy.json，使隐私设置随备份同步
                PrivacyManager.init(context)
                zos.putNextEntry(ZipEntry(PRIVACY_NAME))
                zos.write(
                    PrivacyManager.exportConfig().toString().toByteArray(Charsets.UTF_8)
                )
                zos.closeEntry()
            }
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 从备份 zip 导入，返回导入数量，失败 -1 */
    suspend fun importBackup(
        context: Context,
        repository: ItemRepository,
        zipUri: Uri
    ): Int {
        return try {
            val imageNameToPath = HashMap<String, String>()
            var jsonText: String? = null
            var privacyText: String? = null
            var restoreCounter = 0L

            context.contentResolver.openInputStream(zipUri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        when {
                            entry.name.startsWith(IMAGE_DIR) && !entry.isDirectory -> {
                                val imageName = entry.name.removePrefix(IMAGE_DIR)
                                val imagesDir = File(context.filesDir, "images")
                                if (!imagesDir.exists()) imagesDir.mkdirs()
                                restoreCounter++
                                val dest = File(
                                    imagesDir,
                                    "restored_${System.currentTimeMillis()}_${restoreCounter}_$imageName"
                                )
                                dest.outputStream().use { zis.copyTo(it) }
                                imageNameToPath[imageName] = dest.absolutePath
                            }
                            entry.name == JSON_NAME -> {
                                jsonText = zis.bufferedReader(Charsets.UTF_8).readText()
                            }
                            entry.name == PRIVACY_NAME -> {
                                privacyText = zis.bufferedReader(Charsets.UTF_8).readText()
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            if (jsonText == null) return -1

            val jsonArray = JSONArray(jsonText)
            var count = 0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val paths: List<String> = if (obj.has("imageNames")) {
                    val arr = obj.getJSONArray("imageNames")
                    (0 until arr.length()).mapNotNull {
                        imageNameToPath[arr.getString(it)]
                    }
                } else emptyList()

                fun optDate(key: String): Long? =
                    if (obj.has(key) && !obj.isNull(key)) obj.getLong(key) else null

                val now = System.currentTimeMillis()
                val item = ItemEntity(
                    name = obj.getString("name"),
                    category = obj.optString("category", "其他"),
                    brand = obj.optString("brand", ""),
                    description = obj.optString("description", ""),
                    isPrivate = obj.optBoolean("isPrivate", false),
                    location = obj.optString("location", ""),
                    quantity = obj.optInt("quantity", 1),
                    imagePaths = paths,
                    purchaseDate = optDate("purchaseDate"),
                    expiryDate = optDate("expiryDate"),
                    warrantyDate = optDate("warrantyDate"),
                    borrower = if (obj.has("borrower") && !obj.isNull("borrower"))
                        obj.getString("borrower").ifBlank { null } else null,
                    borrowDate = optDate("borrowDate"),
                    expectedReturnDate = optDate("expectedReturnDate"),
                    createdAt = obj.optLong("createdAt", now),
                    updatedAt = now
                )
                repository.insertItem(item)
                count++
            }

            // 恢复加密箱密码配置（隐私箱设置随备份同步）
            privacyText?.let { text ->
                PrivacyManager.init(context)
                runCatching { PrivacyManager.importConfig(JSONObject(text)) }
            }
            count
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }
}
