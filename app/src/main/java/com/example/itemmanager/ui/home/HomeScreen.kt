package com.example.itemmanager.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.itemmanager.R
import com.example.itemmanager.ui.category.CategoryScreen
import com.example.itemmanager.ui.dashboard.DashboardViewModel
import com.example.itemmanager.ui.list.ItemListScreen
import com.example.itemmanager.ui.list.ItemListViewModel
import com.example.itemmanager.ui.profile.ProfileScreen
import com.example.itemmanager.ui.stats.StatsScreen
import kotlinx.coroutines.launch

/**
 * 主页容器
 *
 * 用 [HorizontalPager] 承载四个 Tab：物品（主页）、分类、统计、我的（副页）。
 * - 左右滑动手指即可在四个 Tab 间切换；
 * - 点击底部导航栏也会平滑滚动到对应页；
 * - 滑动过程中底部导航栏的高亮项自动跟随。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    listViewModel: ItemListViewModel,
    dashboardViewModel: DashboardViewModel,
    backgroundPath: String?,
    onAddClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onShowOnboarding: () -> Unit,
    onBackgroundGallery: () -> Unit,
    onPrivacy: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { homeTabs.size })

    Scaffold(
        containerColor = if (backgroundPath != null) Color.Transparent
        else MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = if (backgroundPath != null)
                    Color.White.copy(alpha = 0.95f)
                else MaterialTheme.colorScheme.surface,
                // 显式指定深色内容色，避免半透明背景下自动算出白色导致图标消失/模糊
                contentColor = Color(0xFF1E293B)
            ) {
                homeTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> ItemListScreen(
                    viewModel = listViewModel,
                    onAddClick = onAddClick,
                    onItemClick = onItemClick
                )
                1 -> CategoryScreen(
                    dashboardViewModel = dashboardViewModel,
                    onItemClick = onItemClick
                )
                2 -> StatsScreen(dashboardViewModel = dashboardViewModel)
                3 -> ProfileScreen(
                    dashboardViewModel = dashboardViewModel,
                    onShowOnboarding = onShowOnboarding,
                    onBackgroundGallery = onBackgroundGallery,
                    onPrivacy = onPrivacy
                )
            }
        }
    }
}

private data class HomeTab(
    val labelRes: Int,
    val icon: ImageVector
)

private val homeTabs = listOf(
    HomeTab(R.string.tab_items, Icons.Default.Inventory2),
    HomeTab(R.string.tab_category, Icons.Default.Category),
    HomeTab(R.string.tab_stats, Icons.Default.BarChart),
    HomeTab(R.string.tab_profile, Icons.Default.Person)
)
