package com.example.itemmanager.ui.stats

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.itemmanager.ui.dashboard.DashboardViewModel
import com.example.itemmanager.ui.theme.TextSecondary

// 分类进度条配色（循环使用）
private val chartColors = listOf(
    Color(0xFF2563EB), Color(0xFF10B981), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899),
    Color(0xFF06B6D4), Color(0xFF84CC16), Color(0xFFF97316),
    Color(0xFF6366F1)
)

/**
 * 统计页（副页）
 * 展示物品总数、总数量、分类数，以及各分类的数量占比。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(dashboardViewModel: DashboardViewModel) {
    val items by dashboardViewModel.items.collectAsState()

    val totalKinds = items.size                       // 物品种类（条目）数
    val totalQuantity = items.sumOf { it.quantity }  // 物品总数量
    val categoryCountMap = items.groupingBy { it.category }.eachCount()
    val categoryCount = categoryCountMap.size        // 分类数量

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无数据，添加物品后查看统计", color = TextSecondary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ---- 顶部三个统计卡片 ----
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Inventory2,
                    value = totalKinds.toString(),
                    label = "物品种类",
                    color = Color(0xFF2563EB)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Tag,
                    value = totalQuantity.toString(),
                    label = "物品总数",
                    color = Color(0xFF10B981)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Category,
                    value = categoryCount.toString(),
                    label = "分类数量",
                    color = Color(0xFFF59E0B)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---- 分类占比 ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "分类分布",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val sorted = categoryCountMap.entries
                        .sortedByDescending { it.value }
                    sorted.forEachIndexed { index, entry ->
                        val color = chartColors[index % chartColors.size]
                        val fraction = entry.value.toFloat() / totalKinds
                        CategoryBar(
                            name = entry.key,
                            count = entry.value,
                            fraction = fraction,
                            color = color
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

/**
 * 单个统计卡片
 */
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

/**
 * 分类占比条
 */
@Composable
private fun CategoryBar(
    name: String,
    count: Int,
    fraction: Float,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(72.dp),
            maxLines = 1
        )
        Spacer(modifier = Modifier.size(8.dp))
        // 进度条轨道
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "${count}件 ${(fraction * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.width(64.dp)
        )
    }
}
