package com.example.itemmanager.util

import android.content.Context
import android.content.Intent
import com.example.itemmanager.data.local.ItemEntity
import java.io.File

/**
 * 分享工具类
 * 通过 Android 系统分享（Share Sheet）把物品信息和图片分享到微信、QQ、云盘、邮件等任意应用，
 * 支持多张图片（ACTION_SEND_MULTIPLE），从而实现"云端分享"。
 */
object ShareUtils {

    /**
     * 分享单个物品（多张图片 + 文字信息）
     */
    fun shareItem(context: Context, item: ItemEntity) {
        // 组装文字内容
        val text = buildString {
            append("【物品】${item.name}\n")
            append("分类：${item.category}\n")
            append("数量：${item.quantity}\n")
            if (item.location.isNotBlank()) append("位置：${item.location}\n")
            item.purchaseDate?.let { append("购买日期：${DateUtils.formatDate(it)}\n") }
            item.expiryDate?.let { append("保质期至：${DateUtils.formatDate(it)}\n") }
            item.warrantyDate?.let { append("保修期至：${DateUtils.formatDate(it)}\n") }
            if (item.description.isNotBlank()) append("备注：${item.description}\n")
        }.trimEnd()

        // 收集存在的图片 Uri
        val imageUris = item.imagePaths.mapNotNull { path ->
            val f = File(path)
            if (f.exists()) ImageUtils.getUriForFile(context, f) else null
        }

        val intent = when {
            // 多张图片：批量分享
            imageUris.size > 1 -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_TEXT, text)
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM, ArrayList(imageUris)
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // 单张图片：图文分享
            imageUris.size == 1 -> Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_STREAM, imageUris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // 无图片：纯文字
            else -> Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }

        val chooser = Intent.createChooser(intent, "分享「${item.name}」到…")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * 分享任意文件（用于把备份文件分享/上传到云端）
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
