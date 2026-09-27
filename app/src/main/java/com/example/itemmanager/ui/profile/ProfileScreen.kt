package com.example.itemmanager.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.itemmanager.ui.dashboard.DashboardViewModel
import com.example.itemmanager.ui.theme.TextSecondary
import com.example.itemmanager.util.ThemeManager
import kotlinx.coroutines.launch

/**
 * 我的页（副页）
 * 提供备份导出、导入恢复、重看新手引导、关于等入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    dashboardViewModel: DashboardViewModel,
    onShowOnboarding: () -> Unit
) {
    val items by dashboardViewModel.items.collectAsState()
    val message by dashboardViewModel.message.collectAsState()
    val themeId by ThemeManager.themeId.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 选择备份文件（zip）
    val openZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // 申请长期读取权限并交给 ViewModel 导入
            runCatching {
                dashboardViewModel.importFrom(it)
            }
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            dashboardViewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // ---- 应用信息卡片 ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "物品管家",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "已记录 ${items.size} 种物品",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- 主题配色 ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "主题配色",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ThemeDot("蓝", Color(0xFF2563EB), themeId == ThemeManager.THEME_BLUE) {
                            ThemeManager.setTheme(context, ThemeManager.THEME_BLUE)
                        }
                        ThemeDot("白", Color(0xFFFFFFFF), themeId == ThemeManager.THEME_WHITE) {
                            ThemeManager.setTheme(context, ThemeManager.THEME_WHITE)
                        }
                        ThemeDot("黑", Color(0xFF111827), themeId == ThemeManager.THEME_BLACK) {
                            ThemeManager.setTheme(context, ThemeManager.THEME_BLACK)
                        }
                        ThemeDot("紫", Color(0xFF7C3AED), themeId == ThemeManager.THEME_PURPLE) {
                            ThemeManager.setTheme(context, ThemeManager.THEME_PURPLE)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- 功能列表 ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    ProfileItem(
                        icon = Icons.Default.Backup,
                        iconTint = Color(0xFF2563EB),
                        title = "导出备份到云端",
                        subtitle = "打包全部物品，上传网盘保存",
                        onClick = { dashboardViewModel.exportAndShare() }
                    )
                    ProfileDivider()
                    ProfileItem(
                        icon = Icons.Default.SettingsBackupRestore,
                        iconTint = Color(0xFF10B981),
                        title = "导入备份恢复",
                        subtitle = "从备份文件恢复物品",
                        onClick = { openZipLauncher.launch(arrayOf("application/zip", "*/*")) }
                    )
                    ProfileDivider()
                    ProfileItem(
                        icon = Icons.Default.HelpOutline,
                        iconTint = Color(0xFFF59E0B),
                        title = "重新查看新手引导",
                        subtitle = "回顾 App 的使用方法",
                        onClick = onShowOnboarding
                    )
                    ProfileDivider()
                    ProfileItem(
                        icon = Icons.Default.Info,
                        iconTint = Color(0xFF64748B),
                        title = "关于",
                        subtitle = "物品管家 v1.0",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("物品管家 v1.0 · 本地物品管理")
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 我的页列表项
 */
@Composable
private fun ProfileItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}

@Composable
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 58.dp)
            .height(1.dp)
            .background(Color(0xFFF1F5F9))
    )
}

/**
 * 主题色块（圆形，选中显示对勾和描边）
 */
@Composable
private fun ThemeDot(
    name: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else Color(0xFFE2E8F0),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                val checkColor = if (color == Color(0xFFFFFFFF)) Color(0xFF2563EB) else Color.White
                Icon(Icons.Default.Check, null, tint = checkColor, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(name, style = MaterialTheme.typography.labelMedium)
    }
}
