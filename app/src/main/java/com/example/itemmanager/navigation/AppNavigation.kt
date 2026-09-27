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
 * 应用主导航图
 * 定义三个页面之间的跳转关系
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
        composable(Routes.ITEM_LIST) {
            val viewModel: ItemListViewModel = viewModel(
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
            val viewModel: ItemDetailViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = ItemDetailViewModel.provideFactory(repository, backStackEntry.savedStateHandle)
            )
            ItemDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { itemId ->
                    navController.navigate(Routes.itemEdit(itemId))
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
                    backStackEntry.savedStateHandle
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
            val viewModel: ItemEditViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = ItemEditViewModel.provideFactory(
                    context.applicationContext as android.app.Application,
                    repository,
                    backStackEntry.savedStateHandle
                )
            )
            ItemEditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaveComplete = {
                    // 保存后返回到详情页（如果是从详情页进入的）或列表页
                    navController.popBackStack()
                }
            )
        }
    }
}
