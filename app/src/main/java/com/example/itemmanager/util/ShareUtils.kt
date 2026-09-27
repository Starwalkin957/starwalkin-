package com.example.itemmanager.util

import android.content.Context
import android.content.Intent
import com.example.itemmanager.data.local.ItemEntity
import java.io.File

/**
 * 分享工具类
 * 通过 Android 系统分享（Share Sheet）把物品信息和图片分享到微信、QQ、云盘、邮件等任意应用，
 * 从而实现"云端分享"——用户可选择已安装的网盘/社交应用上传。
 */
object ShareUtils {

    /**
     * 分享单个物品（图片 + 文字信息）
     * @param context 上下文
     * @param item 要分享的物品
     */
    fun shareItem(context: Context, item: ItemEntity) {
        // 组装文字内容
        val text = buildString {
            append("【物品】${item.name}\n")
            append("分类：${item.category}\n")
            append("数量：${item.quantity}\n")
            if (item.location.isNotBlank()) append("位置：${item.location}\n")
            if (item.description.isNotBlank()) append("备注：${item.description}\n")
        }.trimEnd()

        val imageFile = item.imagePath?.let { File(it) }
        val hasImage = imageFile != null && imageFile.exists()

        val intent = if (hasImage) {
            // 图文分享
            val imageUri = ImageUtils.getUriForFile(context, imageFile!!)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_STREAM, imageUri)
                // 临时授予目标应用读取该图片的权限
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            // 纯文字分享
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }

        // 弹出系统分享面板
        val chooser = Intent.createChooser(intent, "分享「${item.name}」到…")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * 分享任意文件（用于把备份文件分享/上传到云端）
     * @param context 上下文
     * @param file 要分享的文件
     * @param title 分享面板标题
     */
    fun shareFile(context: Context, file: File, title: String = "分享备份文件到…") {
        val uri = ImageUtils.getUriForFile(context, file) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
