package com.example.itemmanager.util

import android.content.Context
import android.content.Intent
import com.example.itemmanager.R
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
        // 组装文字内容（标签随界面语言）
        val text = buildString {
            append(context.getString(R.string.share_item_label) + item.name + "\n")
            append(context.getString(R.string.share_category) + item.category + "\n")
            append(context.getString(R.string.share_quantity) + item.quantity + "\n")
            if (item.location.isNotBlank())
                append(context.getString(R.string.share_location) + item.location + "\n")
            item.purchaseDate?.let {
                append(context.getString(R.string.share_purchase) + DateUtils.formatDate(it) + "\n")
            }
            item.expiryDate?.let {
                append(context.getString(R.string.share_expiry) + DateUtils.formatDate(it) + "\n")
            }
            item.warrantyDate?.let {
                append(context.getString(R.string.share_warranty) + DateUtils.formatDate(it) + "\n")
            }
            if (item.description.isNotBlank())
                append(context.getString(R.string.share_note) + item.description + "\n")
        }.trimEnd()

        // 收集存在的图片 Uri
        val imageUris = item.imagePaths.mapNotNull { path ->
            val f = File(path)
            if (f.exists()) ImageUtils.getUriForFile(context, f) else null
        }

        val intent = when {
            imageUris.size > 1 -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_TEXT, text)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(imageUris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            imageUris.size == 1 -> Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_STREAM, imageUris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            else -> Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }

        val chooser = Intent.createChooser(
            intent, context.getString(R.string.share_chooser, item.name)
        )
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * 分享任意文件（用于把备份文件分享/上传到云端）
     */
    fun shareFile(
        context: Context,
        file: File,
        title: String = context.getString(R.string.share_file_title)
    ) {
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
