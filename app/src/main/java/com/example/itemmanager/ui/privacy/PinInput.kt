package com.example.itemmanager.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 6 位数字密码输入组件
 * 顶部圆点显示输入进度，底部自定义数字键盘；
 * 输入满 6 位自动回调 [onComplete]；[isError] 时圆点变红并短暂延迟后清空。
 */
@Composable
fun PinInput(
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF2563EB),
    isError: Boolean = false,
    resetKey: Any? = Unit,
    onErrorShown: () -> Unit = {},
    onComplete: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    // 步骤变化（如首次输入→再次确认）时清空已输入内容
    LaunchedEffect(resetKey) { pin = "" }

    // 错误反馈后清空
    LaunchedEffect(isError) {
        if (isError) {
            delay(450)
            pin = ""
            onErrorShown()
        }
    }

    // 满 6 位自动提交
    LaunchedEffect(pin) {
        if (pin.length == 6) {
            delay(120)
            onComplete(pin)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 6 个密码圆点
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 6) {
                val filled = i < pin.length
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .then(
                            if (filled) Modifier.background(
                                if (isError) Color(0xFFEF4444) else activeColor
                            )
                            else Modifier.border(
                                2.dp,
                                if (isError) Color(0xFFEF4444)
                                else Color(0xFF94A3B8),
                                CircleShape
                            )
                        )
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        // 数字键盘
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            keys.chunked(3).forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowKeys.forEach { key ->
                        when (key) {
                            "" -> Box(modifier = Modifier.size(64.dp))
                            "del" -> Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = pin.isNotEmpty()) {
                                        pin = pin.dropLast(1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Backspace,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
                                )
                            }
                            else -> Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                                    .clickable(enabled = pin.length < 6) {
                                        if (pin.length < 6) pin += key
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
