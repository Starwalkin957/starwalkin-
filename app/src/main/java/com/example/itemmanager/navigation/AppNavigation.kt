package com.example.itemmanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.itemmanager.data.local.ItemDatabase
import com.example.itemmanager.data.repository.ItemRepository
import com.example.itemmanager.ui.detail.ItemDetailScreen
import com.example.itemmanager.ui.detail.ItemDetailViewModel
import com.example.itemmanager.ui.edit.ItemEditScreen
import com.example.itemmanager.ui.edit.ItemEditViewModel
import com.example.itemmanager.ui.list.ItemListScreen
import com.example.itemmanager.ui.list.ItemListViewModel

/**
 * 应用导航路由定义
 */
object Routes {
    const val ITEM_LIST = "item_list"
    const val ITEM_DETAIL = "item_detail/{itemId}"
    const val ITEM_EDIT = "item_edit"
    const val ITEM_EDIT_WITH_ID = "item_edit/{itemId}"

    fun itemDetail(itemId: Long) = "item_detail/$itemId"
    fun itemEdit(itemId: Long) = "item_edit/$itemId"
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
 * 物品 ID 直接从 backStackEntry.arguments 读取并传给 ViewModel，彻底避免 SavedStateHandle 参数丢失问题
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
                onAddClick = { navController.navigate(Routes.ITEM_EDIT) },
                onItemClick = { itemId ->
                    navController.navigate(Routes.itemDetail(itemId))
                }
            )
        }

        // 物品详情页
        composable(
            route = Routes.ITEM_DETAIL,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            // 直接从 arguments 读取物品 ID，不经过 SavedStateHandle
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: -1L
            val viewModel: ItemDetailViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = ItemDetailViewModel.provideFactory(repository, itemId)
            )
            ItemDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { id ->
                    navController.navigate(Routes.itemEdit(id))
                }
            )
        }

        // 添加新物品（无 ID）
        composable(Routes.ITEM_EDIT) { backStackEntry ->
            val viewModel: ItemEditViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = ItemEditViewModel.provideFactory(
                    context.applicationContext as android.app.Application,
                    repository,
                    null
                )
            )
            ItemEditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaveComplete = { navController.popBackStack() }
            )
        }

        // 编辑已有物品（带 ID）
        composable(
            route = Routes.ITEM_EDIT_WITH_ID,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            // 直接从 arguments 读取物品 ID
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: -1L
            val viewModel: ItemEditViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = ItemEditViewModel.provideFactory(
                    context.applicationContext as android.app.Application,
                    repository,
                    itemId
                )
            )
            ItemEditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaveComplete = {
                    navController.popBackStack()
                }
            )
        }
    }
}
