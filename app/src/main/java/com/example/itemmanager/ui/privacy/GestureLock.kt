package com.example.itemmanager.ui.privacy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * 九宫格手势密码组件
 * - 手指拖动经过的点按顺序选中，支持自动补选两点之间的中间点
 * - 抬手回调完整点序列；结果保留显示，下次拖动时清空
 * - [isError] 为 true 时以红色绘制（验证失败反馈）
 */
@Composable
fun GestureLock(
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF2563EB),
    isError: Boolean = false,
    resetKey: Any? = Unit,
    onReset: () -> Unit = {},
    onComplete: (List<Int>) -> Unit
) {
    val selected = remember { mutableStateListOf<Int>() }
    var currentPos by remember { mutableStateOf<Offset?>(null) }

    // 步骤变化（首次绘制→再次确认）时清空已绘制图案
    LaunchedEffect(resetKey) {
        selected.clear()
        currentPos = null
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        // 9 个点中心：在宽高方向均匀分布（1/4、2/4、3/4 处）
        val centers = remember(widthPx, heightPx) {
            Array(9) { i ->
                val row = i / 3
                val col = i % 3
                Offset((col + 1) * widthPx / 4f, (row + 1) * heightPx / 4f)
            }
        }
        val hitRadius = widthPx * 0.11f

        /** 检查触摸位置命中了哪个未选中点，处理中间点补选 */
        fun trySelect(pos: Offset) {
            for (i in 0 until 9) {
                if (i in selected) continue
                val c = centers[i]
                val dist = hypot(pos.x - c.x, pos.y - c.y)
                if (dist <= hitRadius) {
                    val last = selected.lastOrNull()
                    if (last != null) {
                        val mid = middlePoint(last, i)
                        if (mid != null && mid !in selected) selected.add(mid)
                    }
                    selected.add(i)
                    break
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selected.clear()
                            currentPos = offset
                            onReset()
                            trySelect(offset)
                        },
                        onDrag = { change, _ ->
                            currentPos = change.position
                            trySelect(change.position)
                            change.consume()
                        },
                        onDragEnd = {
                            currentPos = null
                            if (selected.isNotEmpty()) onComplete(selected.toList())
                        },
                        onDragCancel = {
                            currentPos = null
                        }
                    )
                }
        ) {
            val lineColor = if (isError) Color(0xFFEF4444) else activeColor
            val dotOuter = widthPx * 0.075f
            val dotInner = widthPx * 0.032f

            // 选中点之间的连线
            if (selected.size >= 2) {
                for (i in 0 until selected.size - 1) {
                    val a = centers[selected[i]]
                    val b = centers[selected[i + 1]]
                    drawLine(
                        color = lineColor,
                        start = a,
                        end = b,
                        strokeWidth = widthPx * 0.018f,
                        cap = StrokeCap.Round
                    )
                }
            }
            // 最后一个选中点到当前手指位置的连线
            val finger = currentPos
            if (finger != null && selected.isNotEmpty()) {
                drawLine(
                    color = lineColor,
                    start = centers[selected.last()],
                    end = finger,
                    strokeWidth = widthPx * 0.018f,
                    cap = StrokeCap.Round
                )
            }

            // 绘制 9 个点
            for (i in 0 until 9) {
                val c = centers[i]
                val isSelected = i in selected
                // 外圈
                drawCircle(
                    color = if (isSelected) lineColor
                    else Color(0xFF94A3B8).copy(alpha = 0.5f),
                    radius = dotOuter,
                    center = c
                )
                // 内点
                drawCircle(
                    color = if (isSelected) Color.White
                    else Color(0xFF94A3B8),
                    radius = dotInner,
                    center = c
                )
            }
        }
    }
}

/**
 * 两点之间是否存在需要自动补选的中间点（同行/列/对角线且跨越一个点）
 * 返回中间点索引，无则 null
 */
private fun middlePoint(a: Int, b: Int): Int? = when {
    (a == 0 && b == 2) || (a == 2 && b == 0) -> 1
    (a == 0 && b == 6) || (a == 6 && b == 0) -> 3
    (a == 0 && b == 8) || (a == 8 && b == 0) -> 4
    (a == 1 && b == 7) || (a == 7 && b == 1) -> 4
    (a == 2 && b == 8) || (a == 8 && b == 2) -> 5
    (a == 2 && b == 6) || (a == 6 && b == 2) -> 4
    (a == 3 && b == 5) || (a == 5 && b == 3) -> 4
    (a == 6 && b == 8) || (a == 8 && b == 6) -> 7
    else -> null
}
