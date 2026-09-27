package com.example.itemmanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.itemmanager.data.local.ItemDatabase
import com.example.itemmanager.data.repository.ItemRepository
import com.example.itemmanager.ui.detail.ItemDetailScreen
import com.example.itemmanager.ui.detail.ItemDetailViewModel
import com.example.itemmanager.ui.edit.ItemEditScreen
import com.example.itemmanager.ui.edit.ItemEditViewModel
import com.example.itemmanager.ui.list.ItemListScreen
import com.example.itemmanager.ui.list.ItemListViewModel
import com.example.itemmanager.util.SelectedItemHolder

/**
 * 应用导航路由定义
 * 详情页和编辑页不使用导航参数，改为通过全局 SelectedItemHolder 传递物品 ID
 */
object Routes {
    const val ITEM_LIST = "item_list"
    const val ITEM_DETAIL = "item_detail"
    const val ITEM_EDIT = "item_edit"
}

/**
 * 预设分类示例，供添加物品时快速选择
 */
val CategoryPresets = listOf(
    "电子产品",
    "衣物",
    "书籍",
    "食品",
    "日用品",
    "工具",
    "化妆品",
    "运动器材",
    "证件文件",
    "其他"
)

/**
 * 应用主导航图
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // 初始化数据库和仓库（单例）
    val database = ItemDatabase.getDatabase(context)
    val repository = ItemRepository(database.itemDao())

    NavHost(
        navController = navController,
        startDestination = Routes.ITEM_LIST
    ) {
        // 物品列表页
        composable(Routes.ITEM_LIST) { backStackEntry ->
            val viewModel: ItemListViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = ItemListViewModel.provideFactory(repository)
            )
            ItemListScreen(
                viewModel = viewModel,
                onAddClick = {
                    // 添加模式：清除持有者，导航到编辑页
                    SelectedItemHolder.clear()
                    navController.navigate(Routes.ITEM_EDIT)
                },
                onItemClick = { itemId ->
                    // 查看详情：先设置全局持有者，再导航
                    SelectedItemHolder.select(itemId)
                    navController.navigate(Routes.ITEM_DETAIL)
                }
            )
        }

        // 物品详情页（不带参数，从全局持有者读取 ID）
        composable(Routes.ITEM_DETAIL) { backStackEntry ->
            val viewModel: ItemDetailViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = ItemDetailViewModel.provideFactory(repository)
            )
            ItemDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { itemId ->
                    // 编辑模式：设置全局持有者，导航到编辑页
                    SelectedItemHolder.select(itemId)
                    navController.navigate(Routes.ITEM_EDIT)
                }
            )
        }

        // 添加/编辑页（不带参数，从全局持有者读取 ID，null 表示添加模式）
        composable(Routes.ITEM_EDIT) { backStackEntry ->
            val viewModel: ItemEditViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = ItemEditViewModel.provideFactory(
                    context.applicationContext as android.app.Application,
                    repository
                )
            )
            ItemEditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaveComplete = { navController.popBackStack() }
            )
        }
    }
}
