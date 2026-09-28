package com.example.itemmanager

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.itemmanager.navigation.AppNavigation
import com.example.itemmanager.ui.theme.ItemManagerTheme
import com.example.itemmanager.util.BackgroundManager
import java.io.File

/**
 * 应用主 Activity
 * 作为 Compose UI 的入口，设置主题并加载导航图。
 * 支持用户导入照片作为全局背景图（带半透明遮罩保证可读性）。
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 启动时读取已保存的背景图路径
        BackgroundManager.init(applicationContext)

        setContent {
            ItemManagerTheme {
                val backgroundPath by BackgroundManager.backgroundPath.collectAsState()
                val bgFile = backgroundPath?.takeIf { it.isNotBlank() }?.let { File(it) }
                val hasBg = bgFile != null && bgFile.exists()

                Box(modifier = Modifier.fillMaxSize()) {
                    // 底层：背景图（仅当存在且文件有效时显示）
                    if (hasBg) {
                        Image(
                            painter = rememberAsyncImagePainter(model = bgFile),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(0.dp),
                            contentScale = ContentScale.Crop
                        )
                        // 半透明白色遮罩，保证前景文字可读
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.72f))
                        )
                    }

                    // 上层：内容（无背景图时 Surface 提供纯色底）
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (hasBg) Color.Transparent
                        else MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}
