package com.example.itemmanager.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.itemmanager.R

// 预设色相
private val presetHues = listOf(0f, 25f, 45f, 90f, 140f, 175f, 210f, 240f, 275f, 310f, 340f)

private fun hsvColor(hue: Float) = Color.hsv(hue, 0.72f, 0.92f)

/**
 * 调色盘对话框：拖动色相滑块或点预设色块选色，实时预览，确定后回调
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableStateOf(210f) }
    val current = hsvColor(hue)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_color)) },
        text = {
            Column {
                // 彩虹色相条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.hsv(0f, 0.72f, 0.92f),
                                    Color.hsv(60f, 0.72f, 0.92f),
                                    Color.hsv(120f, 0.72f, 0.92f),
                                    Color.hsv(180f, 0.72f, 0.92f),
                                    Color.hsv(240f, 0.72f, 0.92f),
                                    Color.hsv(300f, 0.72f, 0.92f),
                                    Color.hsv(360f, 0.72f, 0.92f)
                                )
                            )
                        )
                )

                // 色相滑块
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = current,
                        activeTrackColor = current
                    )
                )

                // 预览
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(current)
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(stringResource(R.string.current_hue, hue.toInt()))
                }

                Spacer(Modifier.height(12.dp))

                // 预设色块
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetHues.forEach { h ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(hsvColor(h))
                                .clickable { hue = h }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current) }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
