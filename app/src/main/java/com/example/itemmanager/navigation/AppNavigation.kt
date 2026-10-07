package com.example.itemmanager.navigation

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.itemmanager.R
import com.example.itemmanager.data.local.ItemDatabase
import com.example.itemmanager.data.repository.ItemRepository
import com.example.itemmanager.ui.background.BackgroundGalleryScreen
import com.example.itemmanager.ui.dashboard.DashboardViewModel
import com.example.itemmanager.ui.detail.ItemDetailScreen
import com.example.itemmanager.ui.detail.ItemDetailViewModel
import com.example.itemmanager.ui.edit.ItemEditScreen
import com.example.itemmanager.ui.edit.ItemEditViewModel
import com.example.itemmanager.ui.home.HomeScreen
import com.example.itemmanager.ui.list.ItemListViewModel
import com.example.itemmanager.ui.onboarding.OnboardingScreen
import com.example.itemmanager.ui.privacy.PrivacyScreen
import com.example.itemmanager.ui.privacy.PrivacyViewModel
import com.example.itemmanager.util.BackgroundManager
import com.example.itemmanager.util.ExpiryReminder
import com.example.itemmanager.util.OnboardingManager
import com.example.itemmanager.util.PrivacyManager
import com.example.itemmanager.util.SelectedItemHolder
import com.example.itemmanager.util.ThemeManager
import java.io.File

/**
 * 应用导航路由定义
 * - onboarding：新用户引导（可跳过）
 * - home：主容器，内部用 HorizontalPager 承载四个 Tab，可左右滑动切换
 * - item_detail / item_edit：覆盖在主页之上的详情、编辑页
 * - privacy：加密箱；background_gallery：背景图册选择页
 * 详情/编辑不使用导航参数，通过全局 SelectedItemHolder 传递物品 ID
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ITEM_DETAIL = "item_detail"
    const val ITEM_EDIT = "item_edit"
    const val PRIVACY = "privacy"
    const val BACKGROUND_GALLERY = "background_gallery"
}

/**
 * 预设分类示例（字符串资源 id），供添加物品时快速选择。
 * 标签文本通过 stringResource 解析，跟随应用当前语言显示对应翻译。
 */
val CategoryPresetRes = listOf(
    R.string.cat_electronics,
    R.string.cat_digital_accessories,
    R.string.cat_clothing,
    R.string.cat_shoes_bags,
    R.string.cat_books_stationery,
    R.string.cat_food_drinks,
    R.string.cat_daily_necessities,
    R.string.cat_kitchen,
    R.string.cat_furniture_appliances,
    R.string.cat_tools,
    R.string.cat_cosmetics,
    R.string.cat_sports,
    R.string.cat_baby,
    R.string.cat_pet,
    R.string.cat_jewelry,
    R.string.cat_medicine,
    R.string.cat_documents,
    R.string.cat_automotive,
    R.string.cat_other
)

/**
 * 应用主导航图：主容器（可滑动切 Tab）+ 首次引导 + 到期提醒 + 动态主题/背景
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // 初始化主题与背景
    LaunchedEffect(Unit) {
        ThemeManager.init(context)
        BackgroundManager.init(context)
        PrivacyManager.init(context)
    }
    val themeId by ThemeManager.themeId.collectAsState()
    val customColor by ThemeManager.customColor.collectAsState()
    val backgroundPath by BackgroundManager.backgroundPath.collectAsState()
    val appColorScheme = remember(themeId, customColor, backgroundPath) {
        val base = buildAppColorScheme(themeId, customColor)
        // 有自定义背景图时，把页面背景色改为透明，
        // 使各子页面 Scaffold（默认 containerColor=background）露出底层背景图
        if (backgroundPath != null) base.copy(background = Color.Transparent) else base
    }

    // 数据库和仓库（单例）
    val database = ItemDatabase.getDatabase(context)
    val repository = ItemRepository(database.itemDao())

    // Activity 级共享 ViewModel：主页列表与副页共用同一份数据
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.provideFactory(application, repository)
    )
    val listViewModel: ItemListViewModel = viewModel(
        factory = ItemListViewModel.provideFactory(application, repository)
    )

    // 到期提醒：本次启动只检查一次，数据加载后扫描
    val allItems by dashboardViewModel.items.collectAsState()
    var reminders by remember { mutableStateOf<List<ExpiryReminder.Reminder>>(emptyList()) }
    var hasCheckedReminders by remember { mutableStateOf(false) }
    LaunchedEffect(allItems) {
        if (!hasCheckedReminders && allItems.isNotEmpty()) {
            // 提醒扫描排除加密箱内隐私物品，避免在未解锁时暴露其名称
            val list = ExpiryReminder.collect(allItems.filter { !it.isPrivate })
            if (list.isNotEmpty()) reminders = list
            hasCheckedReminders = true
        }
    }

    // 记录引导页是否由"我的"页触发
    var onboardingFromProfile by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = appColorScheme) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ---- 自定义背景图 + 半透明遮罩 ----
            val bgPath = backgroundPath
            if (bgPath != null) {
                AsyncImage(
                    model = File(bgPath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // 半透明遮罩保证前景文字可读（深色主题用黑色遮罩，其余用白色）
                val scrim = if (themeId == ThemeManager.THEME_BLACK)
                    Color.Black.copy(alpha = 0.5f)
                else Color.White.copy(alpha = 0.5f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scrim)
                )
            }

            NavHost(
                navController = navController,
                startDestination = if (OnboardingManager.shouldShow(context))
                    Routes.ONBOARDING else Routes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                // ---- 新用户引导页 ----
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onFinished = {
                            OnboardingManager.setCompleted(context)
                            if (onboardingFromProfile) {
                                onboardingFromProfile = false
                                navController.popBackStack()
                            } else {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // ---- 主容器：四个 Tab，可左右滑动切换 ----
                composable(Routes.HOME) {
                    HomeScreen(
                        listViewModel = listViewModel,
                        dashboardViewModel = dashboardViewModel,
                        backgroundPath = bgPath,
                        onAddClick = {
                            SelectedItemHolder.clear()
                            navController.navigate(Routes.ITEM_EDIT)
                        },
                        onItemClick = { itemId ->
                            SelectedItemHolder.select(itemId)
                            navController.navigate(Routes.ITEM_DETAIL)
                        },
                        onShowOnboarding = {
                            onboardingFromProfile = true
                            navController.navigate(Routes.ONBOARDING)
                        },
                        onBackgroundGallery = {
                            navController.navigate(Routes.BACKGROUND_GALLERY)
                        },
                        onPrivacy = {
                            navController.navigate(Routes.PRIVACY)
                        }
                    )
                }

                // ---- 物品详情页（覆盖页，无底部栏）----
                composable(Routes.ITEM_DETAIL) { backStackEntry ->
                    val vm: ItemDetailViewModel = viewModel(
                        viewModelStoreOwner = backStackEntry,
                        factory = ItemDetailViewModel.provideFactory(repository)
                    )
                    ItemDetailScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onEdit = { itemId ->
                            SelectedItemHolder.select(itemId)
                            navController.navigate(Routes.ITEM_EDIT)
                        }
                    )
                }

                // ---- 添加/编辑页（覆盖页，无底部栏）----
                composable(Routes.ITEM_EDIT) { backStackEntry ->
                    val vm: ItemEditViewModel = viewModel(
                        viewModelStoreOwner = backStackEntry,
                        factory = ItemEditViewModel.provideFactory(application, repository)
                    )
                    ItemEditScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onSaveComplete = { navController.popBackStack() }
                    )
                }

                // ---- 加密箱（覆盖页，无底部栏）----
                composable(Routes.PRIVACY) { backStackEntry ->
                    val vm: PrivacyViewModel = viewModel(
                        viewModelStoreOwner = backStackEntry,
                        factory = PrivacyViewModel.provideFactory(application, repository)
                    )
                    PrivacyScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onItemClick = { itemId ->
                            SelectedItemHolder.select(itemId)
                            navController.navigate(Routes.ITEM_DETAIL)
                        }
                    )
                }

                // ---- 背景图册选择页（覆盖页，无底部栏）----
                composable(Routes.BACKGROUND_GALLERY) {
                    BackgroundGalleryScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // ---- 到期提醒弹窗 ----
            val currentReminders = reminders
            if (currentReminders.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { reminders = emptyList() },
                    title = { Text(stringResource(R.string.reminder_title)) },
                    text = {
                        Column {
                            currentReminders.forEach { r ->
                                val msg = when (r.kind) {
                                    ExpiryReminder.Kind.EXPIRY_TOMORROW ->
                                        stringResource(R.string.expiry_tomorrow)
                                    ExpiryReminder.Kind.EXPIRY_TODAY ->
                                        stringResource(R.string.expiry_today)
                                    ExpiryReminder.Kind.WARRANTY_DAYS ->
                                        stringResource(R.string.warranty_days, r.days)
                                    ExpiryReminder.Kind.WARRANTY_TODAY ->
                                        stringResource(R.string.warranty_today)
                                    ExpiryReminder.Kind.RETURN_TOMORROW ->
                                        stringResource(R.string.return_tomorrow)
                                    ExpiryReminder.Kind.RETURN_TODAY ->
                                        stringResource(R.string.return_today)
                                    ExpiryReminder.Kind.RETURN_OVERDUE ->
                                        stringResource(R.string.return_overdue, r.days)
                                }
                                Text("• ${r.itemName}：$msg")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { reminders = emptyList() }) {
                            Text(stringResource(R.string.got_it))
                        }
                    }
                )
            }
        }
    }
}

/**
 * 根据主题 id 构建配色方案：蓝（默认）、白（简约）、黑（深色）、紫、自定义
 */
private fun buildAppColorScheme(themeId: Int, customColor: Color) = when (themeId) {
    ThemeManager.THEME_BLUE -> lightColorScheme(
        primary = Color(0xFF2563EB),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF1E3A8A),
        secondary = Color(0xFF3B82F6),
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF1E293B),
        surface = Color.White,
        onSurface = Color(0xFF1E293B),
        error = Color(0xFFEF4444)
    )
    ThemeManager.THEME_WHITE -> lightColorScheme(
        primary = Color(0xFF1E293B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFF1F5F9),
        onPrimaryContainer = Color(0xFF1E293B),
        secondary = Color(0xFF64748B),
        background = Color.White,
        onBackground = Color(0xFF1E293B),
        surface = Color.White,
        onSurface = Color(0xFF1E293B),
        error = Color(0xFFEF4444)
    )
    ThemeManager.THEME_BLACK -> darkColorScheme(
        primary = Color(0xFF60A5FA),
        onPrimary = Color(0xFF0F172A),
        primaryContainer = Color(0xFF1E3A5F),
        onPrimaryContainer = Color(0xFFDBEAFE),
        secondary = Color(0xFF94A3B8),
        background = Color(0xFF0F172A),
        onBackground = Color(0xFFE2E8F0),
        surface = Color(0xFF1E293B),
        onSurface = Color(0xFFE2E8F0),
        error = Color(0xFFF87171)
    )
    ThemeManager.THEME_PURPLE -> lightColorScheme(
        primary = Color(0xFF7C3AED),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEDE9FE),
        onPrimaryContainer = Color(0xFF4C1D95),
        secondary = Color(0xFFA78BFA),
        background = Color(0xFFFAF5FF),
        onBackground = Color(0xFF1E1B2E),
        surface = Color.White,
        onSurface = Color(0xFF1E1B2E),
        error = Color(0xFFEF4444)
    )
    ThemeManager.THEME_CUSTOM -> buildCustomColorScheme(customColor)
    else -> lightColorScheme(
        primary = Color(0xFF2563EB),
        onPrimary = Color.White,
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF1E293B),
        surface = Color.White,
        onSurface = Color(0xFF1E293B)
    )
}

/**
 * 基于用户选择的颜色生成一套协调的浅色配色
 */
private fun buildCustomColorScheme(base: Color) = lightColorScheme(
    primary = base,
    onPrimary = onColorFor(base),
    primaryContainer = base.copy(alpha = 0.16f),
    onPrimaryContainer = base,
    secondary = base,
    onSecondary = onColorFor(base),
    secondaryContainer = base.copy(alpha = 0.11f),
    onSecondaryContainer = base,
    tertiary = base,
    onTertiary = onColorFor(base),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF1E293B),
    surface = Color.White,
    onSurface = Color(0xFF1E293B),
    error = Color(0xFFEF4444)
)

/**
 * 根据背景色亮度返回可读前景色：亮底用深色文字，暗底用白色文字，
 * 保证自定义主题下顶部栏、按钮等处的对比度。
 */
private fun onColorFor(bg: Color): Color =
    if (bg.luminance() > 0.5f) Color(0xFF1E293B) else Color.White
