package com.example.itemmanager.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.itemmanager.data.local.ItemEntity
import com.example.itemmanager.ui.theme.ErrorColor
import com.example.itemmanager.ui.theme.SuccessColor
import com.example.itemmanager.ui.theme.TextSecondary
import com.example.itemmanager.util.DateUtils
import com.example.itemmanager.util.ImageUtils
import com.example.itemmanager.util.ShareUtils
import java.io.File

private val WarningColor = Color(0xFFF59E0B)

/**
 * 物品详情页
 * 展示多张图片、有效期信息（含到期状态），支持分享、编辑和删除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    viewModel: ItemDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val item by viewModel.item.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("物品详情", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                    }
                },
                actions = {
                    if (item != null) {
                        IconButton(onClick = { ShareUtils.shareItem(context, item!!) }) {
                            Icon(Icons.Default.Share, "分享", tint = Color.White)
                        }
                        IconButton(onClick = { onEdit(item!!.id) }) {
                            Icon(Icons.Default.Edit, "编辑", tint = Color.White)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "删除", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                item == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("物品不存在或已被删除", color = TextSecondary)
                }
                else -> ItemDetailContent(item = item!!)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${item?.name ?: ""}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteCurrentItem(onBack)
                }) { Text("删除", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

/**
 * 详情内容
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemDetailContent(item: ItemEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ---- 多图轮播 ----
        if (item.imagePaths.isNotEmpty()) {
            val pagerState = rememberPagerState(
                pageCount = { item.imagePaths.size }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                ) { index ->
                    val path = item.imagePaths[index]
                    AsyncImage(
                        model = File(path),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // 多张时显示计数
                if (item.imagePaths.size > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Text(
                            "${pagerState.currentPage + 1}/${item.imagePaths.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Inventory2, null,
                    modifier = Modifier.size(80.dp), tint = Color.LightGray
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(item.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
            Text(
                item.category,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        // ---- 基本信息 ----
        DetailRow("数量", item.quantity.toString())
        DetailRow("存放位置", item.location.ifBlank { "未设置" })

        Spacer(Modifier.height(8.dp))

        // ---- 有效期信息 ----
        DetailRow("购买日期", DateUtils.formatDate(item.purchaseDate).ifBlank { "未设置" })
        ExpiryStatusRow("保质期", item.expiryDate)
        ExpiryStatusRow("保修期", item.warrantyDate)

        Spacer(Modifier.height(8.dp))

        DetailRow("创建时间", ImageUtils.formatDate(item.createdAt))
        DetailRow("更新时间", ImageUtils.formatDate(item.updatedAt))

        Spacer(Modifier.height(20.dp))

        Text("描述", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            item.description.ifBlank { "暂无描述" },
            style = MaterialTheme.typography.bodyLarge,
            color = if (item.description.isBlank()) TextSecondary else Color.Unspecified
        )
    }
}

/**
 * 基本信息行
 */
@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

/**
 * 有效期状态行：显示日期与有效/临期/过期状态（彩色）
 */
@Composable
fun ExpiryStatusRow(label: String, timestamp: Long?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        Spacer(Modifier.width(16.dp))
        if (timestamp == null) {
            Text(
                "未设置",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        } else {
            val days = DateUtils.daysUntil(timestamp)
            val (status, color) = when {
                days < 0 -> "已过期" to ErrorColor
                days <= 7 -> "即将到期" to WarningColor
                else -> "有效" to SuccessColor
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    DateUtils.formatDate(timestamp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    status,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
