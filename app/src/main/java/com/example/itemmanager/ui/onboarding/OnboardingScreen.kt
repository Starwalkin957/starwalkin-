package com.example.itemmanager.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 单页引导内容数据
 */
private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Default.Inventory2,
        title = "欢迎使用物品管家",
        description = "把身边的每件物品都记录下来，\n告别找不到东西的烦恼。",
        color = Color(0xFF2563EB)
    ),
    OnboardingPage(
        icon = Icons.Default.PhotoCamera,
        title = "拍照记录，自动保存",
        description = "调用手机相机一键拍照，\n数据本地持久化，关闭也不丢失。",
        color = Color(0xFF0EA5E9)
    ),
    OnboardingPage(
        icon = Icons.Default.Search,
        title = "模糊搜索，分类管理",
        description = "输入几个字就能找到物品，\n还能按分类快速筛选浏览。",
        color = Color(0xFF10B981)
    ),
    OnboardingPage(
        icon = Icons.Default.CloudUpload,
        title = "备份分享，数据安全",
        description = "支持分享物品、导出云端备份，\n换机恢复，安心使用。",
        color = Color(0xFFF59E0B)
    )
)

/**
 * 新用户引导页
 * 左右滑动浏览，支持"跳过"，最后一页点"开始使用"进入主页
 * @param onFinished 跳过或完成时回调（由外层标记已完成并进入主界面）
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // 顶部：跳过按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            AnimatedVisibility(visible = !isLastPage) {
                TextButton(onClick = onFinished) {
                    Text("跳过", color = Color.Gray)
                }
            }
        }

        // 中部：可滑动页面
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 图标圆形背景
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(page.color, page.color.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 底部：指示点 + 按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 指示点
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { i ->
                    val selected = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(
                                if (selected) Modifier.size(width = 24.dp, height = 8.dp)
                                else Modifier.size(8.dp)
                            )
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else Color(0xFFCBD5E1)
                            )
                    )
                }
            }

            // 下一步 / 开始使用
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = if (isLastPage) "开始使用" else "下一步",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
