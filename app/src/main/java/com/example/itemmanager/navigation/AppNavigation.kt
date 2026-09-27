package com.example.itemmanager.navigation

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.example.itemmanager.util.OnboardingManager
import com.example.itemmanager.util.SelectedItemHolder
import com.example.itemmanager.util.ThemeManager

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
 * 底部导航 Tab 数据
 */
private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab(Routes.TAB_ITEMS, "物品", Icons.Default.Inventory2),
    BottomTab(Routes.TAB_CATEGORY, "分类", Icons.Default.Category),
    BottomTab(Routes.TAB_STATS, "统计", Icons.Default.BarChart),
    BottomTab(Routes.TAB_PROFILE, "我的", Icons.Default.Person)
)

/**
 * 预设分类示例，供添加物品时快速选择（覆盖生活常见类别）
 */
val CategoryPresets = listOf(
    "电子产品",
    "数码配件",
    "衣物",
    "鞋靴箱包",
    "书籍文具",
    "食品饮料",
    "日用品",
    "厨房用品",
    "家具家电",
    "工具",
    "化妆品",
    "运动器材",
    "母婴用品",
    "宠物用品",
    "首饰饰品",
    "医疗药品",
    "证件文件",
    "汽车用品",
    "其他"
)

/**
 * 应用主导航图：底部导航 + 首次引导
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as Application

    // 初始化并监听主题，切换即时生效并持久记住
    LaunchedEffect(Unit) { ThemeManager.init(context) }
    val themeId by ThemeManager.themeId.collectAsState()
    val appColorScheme = remember(themeId) { buildAppColorScheme(themeId) }

    // 初始化数据库和仓库（单例）
    val database = ItemDatabase.getDatabase(context)
    val repository = ItemRepository(database.itemDao())

    // 副页共享的 ViewModel，绑定 Activity 级别，分类/统计/我的共用同一份数据
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.provideFactory(application, repository)
    )

    // 记录引导页是否由"我的"页触发（用于区分首次引导与重看）
    var onboardingFromProfile by remember { mutableStateOf(false) }

    // 当前路由，用于决定是否显示底部导航栏
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    MaterialTheme(colorScheme = appColorScheme) {
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    // 切换 Tab 时保留状态、避免重复建栈
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
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
                            // 从"我的"重看：直接返回
                            onboardingFromProfile = false
                            navController.popBackStack()
                        } else {
                            // 首次引导：进入主页并清掉引导页
                            navController.navigate(Routes.TAB_ITEMS) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // ---- Tab 1：物品（主页）----
            composable(Routes.TAB_ITEMS) { backStackEntry ->
                val viewModel: ItemListViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = ItemListViewModel.provideFactory(application, repository)
                )
                ItemListScreen(
                    viewModel = viewModel,
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
                val viewModel: ItemDetailViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = ItemDetailViewModel.provideFactory(repository)
                )
                ItemDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { itemId ->
                        SelectedItemHolder.select(itemId)
                        navController.navigate(Routes.ITEM_EDIT)
                    }
                )
            }

            // ---- 添加/编辑页（覆盖页，无底部栏）----
            composable(Routes.ITEM_EDIT) { backStackEntry ->
                val viewModel: ItemEditViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = ItemEditViewModel.provideFactory(application, repository)
                )
                ItemEditScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSaveComplete = { navController.popBackStack() }
                )
            }
        }
    }
    }
}

/**
 * 根据主题 id 构建配色方案：蓝（默认）、白（简约）、黑（深色）、紫
 */
private fun buildAppColorScheme(themeId: Int) = when (themeId) {
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
    else -> lightColorScheme(
        primary = Color(0xFF2563EB),
        onPrimary = Color.White,
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF1E293B),
        surface = Color.White,
        onSurface = Color(0xFF1E293B)
    )
}
