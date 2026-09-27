package com.example.itemmanager.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 图片工具类
 * 负责将用户选择/拍摄的图片复制到 App 私有目录，确保图片持久化存储
 */
object ImageUtils {

    /**
     * 创建用于相机拍照的临时图片文件
     * @param context 上下文
     * @return 临时文件，失败返回 null
     */
    fun createCameraImageFile(context: Context): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "camera_${timeStamp}.jpg"
            val directory = File(context.filesDir, "images")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            File(directory, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取文件的 FileProvider Uri，用于向相机应用共享文件
     * @param context 上下文
     * @param file 要共享的文件
     * @return 对应的 content Uri
     */
    fun getUriForFile(context: Context, file: File): Uri? {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 将外部 Uri 指向的图片复制到 App 内部存储
     * @param context 上下文
     * @param uri 图片的 Content Uri
     * @return 复制后的本地文件绝对路径，失败返回 null
     */
    fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            // 生成带时间戳的唯一文件名
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "item_${timeStamp}.jpg"

            // App 私有目录下的 images 文件夹
            val directory = File(context.filesDir, "images")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val destFile = File(directory, fileName)

            // 从输入流复制到输出流
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 删除指定路径的图片文件
     */
    fun deleteImageFile(path: String?) {
        if (path.isNullOrEmpty()) return
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 格式化时间戳为可读日期
     */
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
