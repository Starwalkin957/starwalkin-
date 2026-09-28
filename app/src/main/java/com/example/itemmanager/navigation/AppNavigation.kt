package com.example.itemmanager.navigation

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.itemmanager.R
import com.example.itemmanager.data.local.ItemDatabase
import com.example.itemmanager.data.repository.ItemRepository
import com.example.itemmanager.ui.category.CategoryScreen
import com.example.itemmanager.ui.dashboard.DashboardViewModel
import com.example.itemmanager.ui.detail.ItemDetailScreen
import com.example.itemmanager.ui.detail.ItemDetailViewModel
import com.example.itemmanager.ui.edit.ItemEditScreen
import com.example.itemmanager.ui.edit.ItemEditViewModel
import com.example.itemmanager.ui.list.ItemListScreen
import com.example.itemmanager.ui.list.ItemListViewModel
import com.example.itemmanager.ui.onboarding.OnboardingScreen
import com.example.itemmanager.ui.profile.ProfileScreen
import com.example.itemmanager.ui.stats.StatsScreen
import com.example.itemmanager.util.BackgroundManager
import com.example.itemmanager.util.ExpiryReminder
import com.example.itemmanager.util.OnboardingManager
import com.example.itemmanager.util.SelectedItemHolder
import com.example.itemmanager.util.ThemeManager
import java.io.File

/**
 * 应用导航路由定义
 * - onboarding：新用户引导（可跳过）
 * - tab_*：底部导航的四个主 Tab（物品=主页，分类/统计/我的=副页）
 * - item_detail / item_edit：覆盖在 Tab 之上的详情、编辑页
 * 详情/编辑不使用导航参数，通过全局 SelectedItemHolder 传递物品 ID
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val TAB_ITEMS = "tab_items"
    const val TAB_CATEGORY = "tab_category"
    const val TAB_STATS = "tab_stats"
    const val TAB_PROFILE = "tab_profile"
    const val ITEM_DETAIL = "item_detail"
    const val ITEM_EDIT = "item_edit"
}

/**
 * 底部导航 Tab 数据（label 使用字符串资源以支持多语言）
 */
private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab(Routes.TAB_ITEMS, R.string.tab_items, Icons.Default.Inventory2),
    BottomTab(Routes.TAB_CATEGORY, R.string.tab_category, Icons.Default.Category),
    BottomTab(Routes.TAB_STATS, R.string.tab_stats, Icons.Default.BarChart),
    BottomTab(Routes.TAB_PROFILE, R.string.tab_profile, Icons.Default.Person)
)

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
 * 应用主导航图：底部导航 + 首次引导 + 到期提醒 + 动态主题/背景
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

    // 副页共享的 ViewModel，绑定 Activity 级别，分类/统计/我的共用同一份数据
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.provideFactory(application, repository)
    )

    // 到期提醒：本次启动只检查一次，数据加载后扫描
    val allItems by dashboardViewModel.items.collectAsState()
    var reminders by remember { mutableStateOf<List<ExpiryReminder.Reminder>>(emptyList()) }
    var hasCheckedReminders by remember { mutableStateOf(false) }
    LaunchedEffect(allItems) {
        if (!hasCheckedReminders && allItems.isNotEmpty()) {
            val list = ExpiryReminder.collect(allItems)
            if (list.isNotEmpty()) reminders = list
            hasCheckedReminders = true
        }
    }

    // 记录引导页是否由"我的"页触发
    var onboardingFromProfile by remember { mutableStateOf(false) }

    // 当前路由，用于决定是否显示底部导航栏
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomTabs.map { it.route }

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

            Scaffold(
                containerColor = if (bgPath != null) Color.Transparent
                else MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = if (bgPath != null)
                                Color.White.copy(alpha = 0.88f)
                            else MaterialTheme.colorScheme.surface
                        ) {
                            bottomTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(tab.icon, contentDescription = null)
                                    },
                                    label = { Text(stringResource(tab.labelRes)) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = if (OnboardingManager.shouldShow(context))
                        Routes.ONBOARDING else Routes.TAB_ITEMS,
                    modifier = Modifier.padding(innerPadding)
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
                                    navController.navigate(Routes.TAB_ITEMS) {
                                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    // ---- Tab 1：物品（主页）----
                    composable(Routes.TAB_ITEMS) { backStackEntry ->
                        val vm: ItemListViewModel = viewModel(
                            viewModelStoreOwner = backStackEntry,
                            factory = ItemListViewModel.provideFactory(application, repository)
                        )
                        ItemListScreen(
                            viewModel = vm,
                            onAddClick = {
                                SelectedItemHolder.clear()
                                navController.navigate(Routes.ITEM_EDIT)
                            },
                            onItemClick = { itemId ->
                                SelectedItemHolder.select(itemId)
                                navController.navigate(Routes.ITEM_DETAIL)
                            }
                        )
                    }

                    // ---- Tab 2：分类（副页）----
                    composable(Routes.TAB_CATEGORY) {
                        CategoryScreen(
                            dashboardViewModel = dashboardViewModel,
                            onItemClick = { itemId ->
                                SelectedItemHolder.select(itemId)
                                navController.navigate(Routes.ITEM_DETAIL)
                            }
                        )
                    }

                    // ---- Tab 3：统计（副页）----
                    composable(Routes.TAB_STATS) {
                        StatsScreen(dashboardViewModel = dashboardViewModel)
                    }

                    // ---- Tab 4：我的（副页）----
                    composable(Routes.TAB_PROFILE) {
                        ProfileScreen(
                            dashboardViewModel = dashboardViewModel,
                            onShowOnboarding = {
                                onboardingFromProfile = true
                                navController.navigate(Routes.ONBOARDING)
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
                }
            }

            // ---- 到期提醒弹窗 ----
            val currentReminders = reminders
            if (currentReminders.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { reminders = emptyList() },
                    title = { Text(stringResource(R.string.reminder_title)) },
                    text = {
                        androidx.compose.foundation.layout.Column {
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
    onPrimary = Color.White,
    primaryContainer = base.copy(alpha = 0.16f),
    onPrimaryContainer = base,
    secondary = base,
    onSecondary = Color.White,
    secondaryContainer = base.copy(alpha = 0.11f),
    onSecondaryContainer = base,
    tertiary = base,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF1E293B),
    surface = Color.White,
    onSurface = Color(0xFF1E293B),
    error = Color(0xFFEF4444)
)
