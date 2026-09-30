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
 * 把整个物品库导出为一个 zip 备份文件，结构如下：
 *   items/item_000.json、items/item_001.json … 每个物品单独一个 JSON 文件
 *   images/                    物品的多张图片
 *   manifest.json              清单（备份版本、物品数量、物品文件列表）
 *   privacy.json               加密箱密码配置
 * 用户可上传到任意网盘/云端或发送给他人；导入时同时兼容新格式与旧版单文件 items.json。
 */
object BackupUtils {

    private const val BACKUP_VERSION = 2
    private const val ITEMS_DIR = "items/"
    private const val IMAGE_DIR = "images/"
    private const val MANIFEST_NAME = "manifest.json"
    private const val PRIVACY_NAME = "privacy.json"
    private const val LEGACY_JSON_NAME = "items.json"

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

                // 2. 每个物品单独生成一个 JSON 文件：items/item_000.json …
                val itemFileNames = ArrayList<String>()
                items.forEachIndexed { index, item ->
                    val imageNames = idToImageNames[item.id] ?: emptyList()
                    val itemFileName = ITEMS_DIR + "item_%03d.json".format(index)
                    zos.putNextEntry(ZipEntry(itemFileName))
                    zos.write(
                        itemToJson(item, imageNames).toString().toByteArray(Charsets.UTF_8)
                    )
                    zos.closeEntry()
                    itemFileNames.add(itemFileName)
                }

                // 3. 写入清单 manifest.json
                val manifest = JSONObject()
                manifest.put("version", BACKUP_VERSION)
                manifest.put("count", items.size)
                manifest.put("files", JSONArray(itemFileNames))
                zos.putNextEntry(ZipEntry(MANIFEST_NAME))
                zos.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 4. 写入加密箱密码配置 privacy.json，使隐私设置随备份同步
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
            // 新格式：每个物品文件的 JSON 文本
            val itemJsonTexts = ArrayList<String>()
            // 旧格式：单个 items.json
            var legacyJsonText: String? = null
            var privacyText: String? = null
            var restoreCounter = 0L

            context.contentResolver.openInputStream(zipUri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        when {
                            name.startsWith(IMAGE_DIR) && !entry.isDirectory -> {
                                val imageName = name.removePrefix(IMAGE_DIR)
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
                            // 新格式：items/ 目录下的单个物品 JSON
                            name.startsWith(ITEMS_DIR) && name.endsWith(".json") -> {
                                itemJsonTexts.add(
                                    zis.bufferedReader(Charsets.UTF_8).readText()
                                )
                            }
                            // 旧格式兼容：单个 items.json
                            name == LEGACY_JSON_NAME -> {
                                legacyJsonText = zis.bufferedReader(Charsets.UTF_8).readText()
                            }
                            name == PRIVACY_NAME -> {
                                privacyText = zis.bufferedReader(Charsets.UTF_8).readText()
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            // 既没有新格式物品文件，也没有旧 items.json，说明不是有效备份
            if (itemJsonTexts.isEmpty() && legacyJsonText == null) return -1

            val now = System.currentTimeMillis()
            var count = 0

            // 新格式：逐个物品 JSON 导入
            itemJsonTexts.forEach { text ->
                val item = jsonToItem(JSONObject(text), imageNameToPath, now)
                repository.insertItem(item)
                count++
            }

            // 旧格式兼容：解析 items.json 数组
            legacyJsonText?.let { text ->
                val arr = JSONArray(text)
                for (i in 0 until arr.length()) {
                    val item = jsonToItem(arr.getJSONObject(i), imageNameToPath, now)
                    repository.insertItem(item)
                    count++
                }
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

    /** 把单个物品序列化为 JSON（含图片名列表、品牌、隐私标记） */
    private fun itemToJson(item: ItemEntity, imageNames: List<String>): JSONObject {
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
        if (imageNames.isNotEmpty()) obj.put("imageNames", JSONArray(imageNames))
        return obj
    }

    /** 把单个物品 JSON 反序列化为 ItemEntity */
    private fun jsonToItem(
        obj: JSONObject,
        imageNameToPath: Map<String, String>,
        now: Long
    ): ItemEntity {
        val paths: List<String> = if (obj.has("imageNames")) {
            val arr = obj.getJSONArray("imageNames")
            (0 until arr.length()).mapNotNull { imageNameToPath[arr.getString(it)] }
        } else emptyList()

        fun optDate(key: String): Long? =
            if (obj.has(key) && !obj.isNull(key)) obj.getLong(key) else null

        return ItemEntity(
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
    }
}
