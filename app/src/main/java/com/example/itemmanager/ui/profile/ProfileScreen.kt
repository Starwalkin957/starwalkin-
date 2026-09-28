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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.itemmanager.R
import com.example.itemmanager.ui.dashboard.DashboardViewModel
import com.example.itemmanager.ui.theme.TextSecondary
import com.example.itemmanager.util.BackgroundManager
import com.example.itemmanager.util.ImageUtils
import com.example.itemmanager.util.LanguageManager
import com.example.itemmanager.util.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 我的页（副页）
 * 提供主题配色（含自定义调色盘）、应用背景、语言切换，以及备份/引导/关于等入口。
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
    val customColor by ThemeManager.customColor.collectAsState()
    val backgroundPath by BackgroundManager.backgroundPath.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showColorPicker by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentLangTag = remember { LanguageManager.currentTag() }

    // 选择备份文件（zip）
    val openZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { dashboardViewModel.importFrom(it) } }

    // 选择背景图
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val path = withContext(Dispatchers.IO) {
                    ImageUtils.copyImageToInternalStorage(context, it)
                }
                if (path != null) withContext(Dispatchers.IO) {
                    BackgroundManager.setBackground(context, path)
                }
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
                title = {
                    Text(
                        stringResource(R.string.profile),
                        fontWeight = FontWeight.Bold, color = Color.White
                    )
                },
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
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.recorded_items, items.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- 个性化卡片 ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.theme_color),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ThemeDot(stringResource(R.string.theme_blue), Color(0xFF2563EB), themeId == ThemeManager.THEME_BLUE) {
                            ThemeManager.setTheme(context, ThemeManager.THEME_BLUE)
                        }
                        ThemeDot(stringResource(R.string.theme_white), Color(0xFFFFFFFF), themeId == ThemeManager.THEME_WHITE) {
                            ThemeManager.setTheme(context, ThemeManager.THEME_WHITE)
                        }
                        ThemeDot(stringResource(R.string.theme_black), Color(0xFF111827), themeId == ThemeManager.THEME_BLACK) {
                            ThemeManager.setTheme(context, ThemeManager.THEME_BLACK)
                        }
                        ThemeDot(stringResource(R.string.theme_purple), Color(0xFF7C3AED), themeId == ThemeManager.THEME_PURPLE) {
                            ThemeManager.setTheme(context, ThemeManager.THEME_PURPLE)
                        }
                        ThemeDot(
                            stringResource(R.string.custom_color),
                            customColor,
                            themeId == ThemeManager.THEME_CUSTOM
                        ) {
                            showColorPicker = true
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- 显示与语言卡片 ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    val hasBackground = backgroundPath != null
                    ProfileItem(
                        icon = Icons.Default.Wallpaper,
                        iconTint = Color(0xFF8B5CF6),
                        title = stringResource(R.string.app_background),
                        subtitle = if (hasBackground) stringResource(R.string.clear_background)
                        else stringResource(R.string.choose_background),
                        onClick = {
                            if (hasBackground) scope.launch {
                                withContext(Dispatchers.IO) {
                                    BackgroundManager.clearBackground(context)
                                }
                            }
                            else backgroundLauncher.launch("image/*")
                        }
                    )
                    ProfileDivider()
                    val currentLangName = LanguageManager.languages
                        .find { it.tag == currentLangTag }?.displayName
                    ProfileItem(
                        icon = Icons.Default.Language,
                        iconTint = Color(0xFF0EA5E9),
                        title = stringResource(R.string.language),
                        subtitle = currentLangName ?: "",
                        onClick = { showLanguageDialog = true }
                    )
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
                        title = stringResource(R.string.export_cloud),
                        subtitle = stringResource(R.string.export_cloud_desc),
                        onClick = { dashboardViewModel.exportAndShare() }
                    )
                    ProfileDivider()
                    ProfileItem(
                        icon = Icons.Default.SettingsBackupRestore,
                        iconTint = Color(0xFF10B981),
                        title = stringResource(R.string.import_backup),
                        subtitle = stringResource(R.string.import_backup_desc),
                        onClick = { openZipLauncher.launch(arrayOf("application/zip", "*/*")) }
                    )
                    ProfileDivider()
                    ProfileItem(
                        icon = Icons.Default.HelpOutline,
                        iconTint = Color(0xFFF59E0B),
                        title = stringResource(R.string.replay_guide),
                        subtitle = stringResource(R.string.replay_guide_desc),
                        onClick = onShowOnboarding
                    )
                    ProfileDivider()
                    ProfileItem(
                        icon = Icons.Default.Info,
                        iconTint = Color(0xFF64748B),
                        title = stringResource(R.string.about),
                        subtitle = stringResource(R.string.about_desc),
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.about_msg)
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // ---- 调色盘对话框 ----
    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = customColor,
            onConfirm = { color ->
                ThemeManager.setCustomColor(context, color)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    // ---- 语言选择对话框 ----
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    LanguageManager.languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    LanguageManager.setLanguage(lang.tag)
                                    showLanguageDialog = false
                                }
                                .padding(horizontal = 4.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                lang.displayName,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (lang.tag == currentLangTag) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
